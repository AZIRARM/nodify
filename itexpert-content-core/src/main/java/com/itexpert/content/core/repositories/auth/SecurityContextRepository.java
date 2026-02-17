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
import reactor.core.publisher.MonoSink;

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
        String authHeader = swe.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("Pas de token Bearer trouvé dans la requête");
            return Mono.empty();
        }

        String authToken = authHeader.substring(7);
        log.debug("Token extrait: {}", authToken.substring(0, Math.min(authToken.length(), 20)) + "...");

        Authentication auth = new UsernamePasswordAuthenticationToken(authToken, authToken);

        return this.authenticationManager.authenticate(auth)
                .doOnNext(authentication -> {
                    log.info("Authentification réussie pour: {}", authentication.getPrincipal());
                    log.debug("Autorités: {}", authentication.getAuthorities());
                })
                .map(authentication -> {
                    SecurityContext context = new SecurityContextImpl(authentication);
                    log.debug("SecurityContext créé avec succès");
                    return context;
                })
                .doOnError(error -> {
                    log.error("Erreur lors de l'authentification: {}", error.getMessage());
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("Authentification échouée - aucun utilisateur authentifié");
                    return Mono.empty();
                }));
    }
}