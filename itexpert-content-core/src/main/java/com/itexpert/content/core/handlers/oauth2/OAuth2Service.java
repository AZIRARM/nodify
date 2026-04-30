package com.itexpert.content.core.handlers.oauth2;

import com.itexpert.content.core.config.OAuth2Properties;
import com.itexpert.content.core.models.oauth.AuthUserInfo;
import com.itexpert.content.core.models.oauth.TokenResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2Service {

    private final WebClient.Builder webClientBuilder;
    private final OAuth2Properties oauth2Properties;

    public Mono<AuthUserInfo> validateAndGetUserInfo(String token) {
        if (!oauth2Properties.isEnabled()) {
            return Mono.error(new IllegalStateException("OAuth2 is not enabled"));
        }

        return getUserInfoFromOAuth2(token)
                .map(this::extractUserInfo);
    }

    private Mono<Map<String, Object>> getUserInfoFromOAuth2(String token) {
        WebClient webClient = webClientBuilder.build();
        OAuth2Properties.OAuth2Config config = oauth2Properties.getConfig();

        return webClient
                .get()
                .uri(config.getUserInfoUri())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                })
                .doOnError(error -> log.error("Failed to get user info from OAuth2: {}", error.getMessage()));
    }

    @SuppressWarnings("unchecked")
    private AuthUserInfo extractUserInfo(Map<String, Object> userInfoMap) {
        OAuth2Properties.OAuth2Config config = oauth2Properties.getConfig();

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
                "oauth2",
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

        return Collections.emptyList();
    }

    public Mono<TokenResponse> exchangeCodeForToken(String code) {
        WebClient webClient = webClientBuilder.build();
        OAuth2Properties.OAuth2Config config = oauth2Properties.getConfig();

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