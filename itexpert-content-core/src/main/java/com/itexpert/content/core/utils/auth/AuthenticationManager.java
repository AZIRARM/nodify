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

        // Étape 1: Récupérer le token
        String authToken = authentication.getCredentials().toString();
        log.info("Tentative d'authentification avec le token");

        // Étape 2: Valider le token
        boolean isValid = jwtUtil.validateToken(authToken);

        if (!isValid) {
            log.warn("Token invalide");
            return Mono.empty();
        }

        // Étape 3: Extraire les informations du token
        Claims claims = jwtUtil.getAllClaimsFromToken(authToken);
        String username = jwtUtil.getUsernameFromToken(authToken);

        // Étape 4: Récupérer la liste des rôles depuis le token
        List<String> rolesFromToken = claims.get("role", List.class);
        log.info("Rôles récupérés du token pour {}: {}", username, rolesFromToken);

        // Étape 5: Créer la liste des autorités (rôles)
        Collection<GrantedAuthority> authorities = new ArrayList<>();

        // Parcourir chaque rôle et l'ajouter aux autorités
        for (String role : rolesFromToken) {

            // Vérifier quel est le rôle et l'ajouter en conséquence
            if ("ADMIN".equals(role)) {
                log.debug("Ajout du rôle ADMIN pour {}", username);
                authorities.add(new SimpleGrantedAuthority("ADMIN"));
            } else if ("EDITOR".equals(role)) {
                log.debug("Ajout du rôle EDITOR pour {}", username);
                authorities.add(new SimpleGrantedAuthority("EDITOR"));
            } else if ("READER".equals(role)) {
                log.debug("Ajout du rôle READER pour {}", username);
                authorities.add(new SimpleGrantedAuthority("READER"));
            } else {
                log.warn("Rôle inconnu ignoré: {} pour {}", role, username);
            }
        }

        // Étape 6: Vérifier qu'au moins un rôle a été ajouté
        if (authorities.isEmpty()) {
            log.error("Aucun rôle valide trouvé pour {}", username);
            return Mono.empty();
        }

        // Étape 7: Afficher les rôles finaux
        log.info("Authentification réussie pour {} avec les rôles: {}",
                username, authorities);

        // Étape 8: Créer et retourner l'objet Authentication
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                username,
                null,
                authorities
        );

        return Mono.just(auth);
    }
}
