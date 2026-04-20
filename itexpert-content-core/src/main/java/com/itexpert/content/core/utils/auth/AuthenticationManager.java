package com.itexpert.content.core.utils.auth;

import com.itexpert.content.core.config.SecurityProperties;
import com.itexpert.content.core.handlers.UserHandler;
import com.itexpert.content.core.handlers.oauth2.OAuth2Service;
import com.itexpert.content.core.handlers.openid.OpenIDService;
import com.itexpert.content.core.models.oauth.AuthUserInfo;
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
        // Email fixe pour tester (à enlever après)
        String email = "azirarm@gmail.com";

        return userHandler.findByEmail(email)
                .flatMap(user -> {
                    Collection<GrantedAuthority> authorities = extractAuthorities(user.getRoles());
                    log.info("OAuth2 authentication successful for: {}", email);
                    return Mono.just(new UsernamePasswordAuthenticationToken(email, null, authorities));
                })
                .cast(Authentication.class)
                .switchIfEmpty(Mono.defer(() -> {
                    log.error("User not found: {}", email);
                    return Mono.empty();
                }));
    }

    private String extractEmailFromToken(String token) {
        try {
            String[] parts = token.split("\\.");
            String payload = parts[1];
            String decoded = new String(java.util.Base64.getDecoder().decode(payload));

            // Extraire l'email avec des regex simples
            String email = null;
            if (decoded.contains("\"email\"")) {
                int start = decoded.indexOf("\"email\"") + 9;
                start = decoded.indexOf("\"", start) + 1;
                int end = decoded.indexOf("\"", start);
                email = decoded.substring(start, end);
            } else if (decoded.contains("\"preferred_username\"")) {
                int start = decoded.indexOf("\"preferred_username\"") + 21;
                start = decoded.indexOf("\"", start) + 1;
                int end = decoded.indexOf("\"", start);
                email = decoded.substring(start, end);
            }

            log.info("Email extracted from token: {}", email);
            return email;
        } catch (Exception e) {
            log.error("Failed to extract email: {}", e.getMessage());
            return null;
        }
    }

    private Mono<Authentication> authenticateWithOpenID(String authToken) {
        if (openIDService == null) {
            log.error("OpenIDService is not available");
            return Mono.empty();
        }

        return openIDService.validateAndGetUserInfo(authToken)
                .flatMap(this::enrichWithProjects)
                .flatMap(this::createAuthentication)
                .onErrorResume(error -> {
                    log.error("OpenID authentication failed: {}", error.getMessage());
                    return Mono.empty();
                });
    }

    private Mono<AuthUserInfo> enrichWithProjects(AuthUserInfo userInfo) {
        String email = userInfo.getEmail() != null ? userInfo.getEmail() : userInfo.getUsername();

        return userHandler.findByEmail(email)
                .map(user -> {
                    userInfo.setProjects(user.getProjects());
                    log.debug("Enriched user {} with projects: {}", userInfo.getUsername(), user.getProjects());
                    return userInfo;
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.debug("User not found in database for email: {}", email);
                    userInfo.setProjects(new ArrayList<>());
                    return Mono.just(userInfo);
                }));
    }

    private Mono<Authentication> createAuthentication(AuthUserInfo userInfo) {
        Collection<GrantedAuthority> authorities = extractAuthorities(userInfo.getRoles());

        if (authorities.isEmpty()) {
            log.debug("No valid roles found for {} with auth type {}",
                    userInfo.getUsername(), userInfo.getAuthType());
            return Mono.empty();
        }

        log.debug("{} authentication successful for {} with roles: {}",
                userInfo.getAuthType().toUpperCase(), userInfo.getUsername(), authorities);

        return Mono.just(new UsernamePasswordAuthenticationToken(userInfo.getUsername(), null, authorities));
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