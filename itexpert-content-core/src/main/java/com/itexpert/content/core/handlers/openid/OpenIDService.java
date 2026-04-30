package com.itexpert.content.core.handlers.openid;

import com.itexpert.content.core.models.oauth.AuthUserInfo;
import com.itexpert.content.core.models.oauth.TokenResponse;
import com.itexpert.content.core.config.OpenIDProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenIDService {

    private final WebClient.Builder webClientBuilder;
    private final OpenIDProperties openIDProperties;
    private final Map<String, PublicKey> publicKeyCache = new ConcurrentHashMap<>();
    private Map<String, Object> discoveryDocument;

    public Mono<AuthUserInfo> validateAndGetUserInfo(String token) {
        if (!openIDProperties.isEnabled()) {
            return Mono.error(new IllegalStateException("OpenID Connect is not enabled"));
        }

        return validateTokenSignature(token)
                .flatMap(claims -> getUserInfo(token, claims))
                .map(this::extractUserInfo);
    }

    private Mono<Claims> validateTokenSignature(String token) {
        return getPublicKey()
                .flatMap(publicKey -> {
                    try {
                        Claims claims = Jwts.parserBuilder()
                                .setSigningKey(publicKey)
                                .build()
                                .parseClaimsJws(token)
                                .getBody();

                        String issuer = openIDProperties.getConfig().getIssuerUri();
                        if (issuer != null && !issuer.equals(claims.getIssuer())) {
                            return Mono.error(new RuntimeException("Invalid issuer: " + claims.getIssuer()));
                        }

                        return Mono.just(claims);
                    } catch (Exception e) {
                        log.error("Token validation failed: {}", e.getMessage());
                        return Mono.error(e);
                    }
                });
    }

    private Mono<PublicKey> getPublicKey() {
        String cacheKey = openIDProperties.getConfig().getIssuerUri();
        if (publicKeyCache.containsKey(cacheKey)) {
            return Mono.just(publicKeyCache.get(cacheKey));
        }

        return fetchPublicKey()
                .doOnNext(key -> publicKeyCache.put(cacheKey, key));
    }

    private Mono<PublicKey> fetchPublicKey() {
        return getDiscoveryDocument()
                .flatMap(discovery -> {
                    String jwksUri = (String) discovery.get("jwks_uri");
                    if (jwksUri == null) {
                        jwksUri = openIDProperties.getConfig().getJwkSetUri();
                    }

                    if (jwksUri == null) {
                        return Mono.error(new RuntimeException("JWKS URI not configured"));
                    }

                    WebClient webClient = webClientBuilder.build();
                    return webClient.get()
                            .uri(jwksUri)
                            .retrieve()
                            .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                            });
                })
                .map(this::extractPublicKey);
    }

    private PublicKey extractPublicKey(Map<String, Object> jwks) {
        try {
            List<Map<String, Object>> keys = (List<Map<String, Object>>) jwks.get("keys");
            if (keys != null && !keys.isEmpty()) {
                Map<String, Object> key = keys.get(0);
                String modulus = (String) key.get("n");
                String exponent = (String) key.get("e");

                byte[] modulusBytes = Base64.getUrlDecoder().decode(modulus);
                byte[] exponentBytes = Base64.getUrlDecoder().decode(exponent);

                RSAPublicKeySpec spec = new RSAPublicKeySpec(
                        new java.math.BigInteger(1, modulusBytes),
                        new java.math.BigInteger(1, exponentBytes));
                KeyFactory factory = KeyFactory.getInstance("RSA");
                return factory.generatePublic(spec);
            }
            throw new RuntimeException("No keys found in JWKS");
        } catch (Exception e) {
            throw new RuntimeException("Failed to create public key", e);
        }
    }

    private Mono<Map<String, Object>> getDiscoveryDocument() {
        if (discoveryDocument != null) {
            return Mono.just(discoveryDocument);
        }

        String discoveryUrl = openIDProperties.getConfig().getIssuerUri() + "/.well-known/openid-configuration";
        WebClient webClient = webClientBuilder.build();

        return webClient.get()
                .uri(discoveryUrl)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                })
                .doOnNext(doc -> this.discoveryDocument = doc)
                .doOnError(error -> log.error("Failed to fetch discovery document: {}", error.getMessage()));
    }

    private Mono<Map<String, Object>> getUserInfo(String token, Claims claims) {
        String userInfoUri = openIDProperties.getConfig().getUserInfoUri();

        if (userInfoUri == null) {
            return Mono.just(claims);
        }

        WebClient webClient = webClientBuilder.build();
        return webClient
                .get()
                .uri(userInfoUri)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                })
                .defaultIfEmpty(claims);
    }

    @SuppressWarnings("unchecked")
    private AuthUserInfo extractUserInfo(Map<String, Object> userInfoMap) {
        OpenIDProperties.OpenIDConfig config = openIDProperties.getConfig();

        String username = (String) userInfoMap.getOrDefault(
                config.getUserNameClaim(),
                userInfoMap.getOrDefault("sub", "unknown"));

        String email = (String) userInfoMap.getOrDefault("email", username);

        List<String> roles = extractRoles(userInfoMap, config.getRolesClaim());

        return new AuthUserInfo(
                username,
                email,
                roles,
                new ArrayList<>(),
                "openid",
                userInfoMap);
    }

    @SuppressWarnings("unchecked")
    private List<String> extractRoles(Map<String, Object> userInfoMap, String rolesClaim) {
        String[] claimParts = rolesClaim.split("\\.");
        Object current = userInfoMap;

        for (String part : claimParts) {
            if (current instanceof Map) {
                current = ((Map<String, Object>) current).get(part);
            } else {
                return Collections.emptyList();
            }
        }

        if (current instanceof List) {
            return (List<String>) current;
        }

        if (current instanceof String) {
            return Collections.singletonList((String) current);
        }

        return Collections.emptyList();
    }

    public Mono<TokenResponse> exchangeCodeForToken(String code) {
        WebClient webClient = webClientBuilder.build();
        OpenIDProperties.OpenIDConfig config = openIDProperties.getConfig();

        return webClient
                .post()
                .uri(config.getTokenUri())
                .header(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded")
                .bodyValue("grant_type=authorization_code" +
                        "&code=" + code +
                        "&client_id=" + config.getClientId() +
                        "&client_secret=" + config.getClientSecret() +
                        "&redirect_uri=" + config.getRedirectUri())
                .retrieve()
                .bodyToMono(TokenResponse.class)
                .doOnError(error -> log.error("Failed to exchange code for token: {}", error.getMessage()));
    }
}