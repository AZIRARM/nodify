package com.itexpert.content.core.utils.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itexpert.content.core.config.SecurityProperties;
import com.itexpert.content.core.handlers.UserHandler;
import com.itexpert.content.core.handlers.oauth2.OAuth2Service;
import com.itexpert.content.core.handlers.openid.OpenIDService;
import com.itexpert.content.core.models.auth.RoleEnum;
import com.itexpert.content.lib.models.UserPost;

import io.jsonwebtoken.Claims;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Slf4j
@Component
@AllArgsConstructor
public class AuthenticationManager implements ReactiveAuthenticationManager {

    private final JWTUtil jwtUtil;
    private final UserHandler userHandler;

    @Autowired(required = false)
    @Lazy
    private OAuth2Service oauth2Service;

    @Autowired(required = false)
    @Lazy
    private OpenIDService openIDService;

    private final SecurityProperties securityProperties;

    @Override
    @SuppressWarnings("unchecked")
    public Mono<Authentication> authenticate(Authentication authentication) {
        String authToken = authentication.getCredentials().toString();
        String mode = securityProperties.getMode();

        log.debug("Authentication mode: {}", mode);

        switch (mode) {
            case "oauth2":
                return authenticateWithOAuth2(authToken);
            case "openid":
                return authenticateWithOpenID(authToken);
            default:
                return authenticateWithInternalJWT(authToken);
        }
    }

    private Mono<Authentication> authenticateWithInternalJWT(String authToken) {
        if (!jwtUtil.validateToken(authToken)) {
            log.debug("Invalid internal JWT token");
            return Mono.empty();
        }

        String username = jwtUtil.getUsernameFromToken(authToken);
        Claims claims = jwtUtil.getAllClaimsFromToken(authToken);
        List<String> rolesFromToken = claims.get("role", List.class);

        Collection<GrantedAuthority> authorities = extractAuthorities(rolesFromToken);

        if (authorities.isEmpty()) {
            log.debug("No valid roles found for {}", username);
            return Mono.empty();
        }

        log.debug("Internal JWT authentication successful for {} with roles: {}", username, authorities);
        return Mono.just(new UsernamePasswordAuthenticationToken(username, null, authorities));
    }

    private Mono<Authentication> authenticateWithOAuth2(String authToken) {
        if (oauth2Service == null) {
            log.error("OAuth2Service is not available");
            return Mono.empty();
        }

        String email = extractEmailFromToken(authToken);

        if (email == null) {
            log.error("No email found in OAuth2 token");
            return Mono.empty();
        }

        log.info("OAuth2 authentication for email: {}", email);

        return userHandler.findByEmail(email)
                .<Authentication>flatMap(user -> {
                    Collection<GrantedAuthority> authorities = extractAuthorities(user.getRoles());
                    log.info("OAuth2 authentication successful for: {}", email);
                    return Mono.just(new UsernamePasswordAuthenticationToken(email, null, authorities));
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.error("User not found: {}", email);
                    return Mono.empty();
                }));
    }

    private Mono<Authentication> authenticateWithOpenID(String authToken) {
        if (openIDService == null) {
            log.error("OpenIDService is not available");
            return Mono.empty();
        }

        String email = extractEmailFromToken(authToken);

        if (email == null) {
            log.error("No email found in OpenID token");
            return Mono.empty();
        }

        log.info("OpenID authentication for email: {}", email);

        return userHandler.findByEmail(email)
                .<Authentication>flatMap(user -> {
                    Collection<GrantedAuthority> authorities = extractAuthorities(user.getRoles());
                    log.info("OpenID authentication successful for: {}", email);
                    return Mono.just(new UsernamePasswordAuthenticationToken(email, null, authorities));
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.info("User not found, creating new user: {}", email);

                    UserPost newUser = new UserPost();
                    newUser.setEmail(email);
                    newUser.setRoles(List.of(RoleEnum.EDITOR.name()));
                    newUser.setPassword("password");
                    return userHandler.subscribe(newUser, true)
                            .<Authentication>flatMap(savedUser -> {
                                Collection<GrantedAuthority> authorities = extractAuthorities(savedUser.getRoles());
                                log.info("User created and authenticated: {}", email);
                                return Mono.just(new UsernamePasswordAuthenticationToken(email, null, authorities));
                            })
                            .onErrorResume(error -> {
                                log.error("Failed to create user: {}", error.getMessage());
                                return Mono.empty();
                            });
                }));
    }

    private String extractEmailFromToken(String token) {
        try {
            String[] parts = token.split("\\.");
            String payload = parts[1];
            byte[] decodedBytes = java.util.Base64.getDecoder().decode(payload);
            String decoded = new String(decodedBytes, StandardCharsets.UTF_8);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode json = mapper.readTree(decoded);

            String email = null;
            if (json.has("email")) {
                email = json.get("email").asText();
            } else if (json.has("preferred_username")) {
                email = json.get("preferred_username").asText();
            } else if (json.has("sub")) {
                email = json.get("sub").asText();
            }

            log.info("Email extracted from token: {}", email);
            return email;
        } catch (Exception e) {
            log.error("Failed to extract email from token: {}", e.getMessage());
            return null;
        }
    }

    private Collection<GrantedAuthority> extractAuthorities(List<String> roles) {
        if (roles == null) {
            return new ArrayList<>();
        }

        Collection<GrantedAuthority> authorities = new ArrayList<>();
        for (String role : roles) {
            if ("ADMIN".equals(role) || "EDITOR".equals(role) || "READER".equals(role)) {
                authorities.add(new SimpleGrantedAuthority(role));
            }
        }
        return authorities;
    }
}