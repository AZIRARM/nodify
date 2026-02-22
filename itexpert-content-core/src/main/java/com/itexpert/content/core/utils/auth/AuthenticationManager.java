package com.itexpert.content.core.utils.auth;

import io.jsonwebtoken.Claims;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private JWTUtil jwtUtil;

    @Override
    @SuppressWarnings("unchecked")
    public Mono<Authentication> authenticate(Authentication authentication) {
        String authToken = authentication.getCredentials().toString();

        if (!jwtUtil.validateToken(authToken)) {
            log.debug("Invalid token");
            return Mono.empty();
        }

        Claims claims = jwtUtil.getAllClaimsFromToken(authToken);
        String username = jwtUtil.getUsernameFromToken(authToken);
        List<String> rolesFromToken = claims.get("role", List.class);

        Collection<GrantedAuthority> authorities = new ArrayList<>();

        for (String role : rolesFromToken) {
            if ("ADMIN".equals(role) || "EDITOR".equals(role) || "READER".equals(role)) {
                authorities.add(new SimpleGrantedAuthority(role));
            }
        }

        if (authorities.isEmpty()) {
            log.debug("No valid roles found for {}", username);
            return Mono.empty();
        }

        log.debug("Authentication successful for {} with roles: {}", username, authorities);

        return Mono.just(new UsernamePasswordAuthenticationToken(username, null, authorities));
    }
}