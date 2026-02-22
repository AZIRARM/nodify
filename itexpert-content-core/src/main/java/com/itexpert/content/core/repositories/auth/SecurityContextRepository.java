package com.itexpert.content.core.repositories.auth;

import com.itexpert.content.core.utils.auth.AuthenticationManager;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@AllArgsConstructor
@Component
public class SecurityContextRepository implements ServerSecurityContextRepository {

    private final AuthenticationManager authenticationManager;

    @Override
    public Mono<Void> save(ServerWebExchange swe, SecurityContext sc) {
        return Mono.empty();
    }

    @Override
    public Mono<SecurityContext> load(ServerWebExchange swe) {
        String token = extractToken(swe);

        if (token == null) {
            log.debug("No Bearer token found in request");
            return Mono.empty();
        }

        log.debug("Token extracted: {}...", token.substring(0, Math.min(token.length(), 20)));

        Authentication auth = new UsernamePasswordAuthenticationToken(token, token);

        return this.authenticationManager.authenticate(auth)
                .<SecurityContext>map(authentication -> {
                    log.info("Authentication successful for: {}", authentication.getPrincipal());
                    return new SecurityContextImpl(authentication);
                })
                .doOnError(error ->
                        log.error("Authentication error: {}", error.getMessage())
                )
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("Authentication failed - no authenticated user");
                    return Mono.empty();
                }));
    }

    private String extractToken(ServerWebExchange swe) {
        String authHeader = swe.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        String urlToken = swe.getRequest().getQueryParams().getFirst("authorization");
        if (urlToken != null) {
            if (urlToken.startsWith("Bearer ")) {
                return urlToken.substring(7);
            }
            return urlToken;
        }

        return null;
    }
}