package com.itexpert.content.core.utils.auth;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import reactor.core.publisher.Mono;

public class SecurityUtils {

    private SecurityUtils() {
        // Classe utilitaire - constructeur privé
    }

    public static Mono<Authentication> getAuthentication() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication);
    }

    public static Mono<String> getUsername() {
        return getAuthentication()
                .map(Authentication::getName);
    }

    public static Mono<Boolean> hasRole(String role) {
        return getAuthentication()
                .map(auth -> auth.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals(role)))
                .defaultIfEmpty(false);
    }

    public static Mono<Boolean> hasAnyRole(String... roles) {
        return getAuthentication()
                .map(auth -> {
                    var authorities = auth.getAuthorities();
                    for (String role : roles) {
                        if (authorities.stream().anyMatch(a -> a.getAuthority().equals(role))) {
                            return true;
                        }
                    }
                    return false;
                })
                .defaultIfEmpty(false);
    }

    public static Mono<Boolean> hasAllRoles(String... roles) {
        return getAuthentication()
                .map(auth -> {
                    var authorities = auth.getAuthorities();
                    for (String role : roles) {
                        if (authorities.stream().noneMatch(a -> a.getAuthority().equals(role))) {
                            return false;
                        }
                    }
                    return true;
                })
                .defaultIfEmpty(false);
    }
}