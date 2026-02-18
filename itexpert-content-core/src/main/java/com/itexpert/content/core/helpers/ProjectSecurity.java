package com.itexpert.content.core.helpers;

import com.itexpert.content.core.handlers.NodeHandler;
import com.itexpert.content.core.handlers.UserHandler;
import com.itexpert.content.lib.enums.StatusEnum;
import com.itexpert.content.lib.models.Node;
import com.itexpert.content.lib.models.UserPost;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProjectSecurity {

    private final UserHandler userHandler;
    private final NodeHandler nodeHandler;

    /**
     * Vérifie si l'utilisateur a accès au projet correspondant au code du node
     *
     * @param nodeCode Le code du node à vérifier
     * @return true si l'utilisateur a accès
     */
    public boolean hasProjectAccess(String nodeCode) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            log.warn("Pas d'authentification trouvée");
            return false;
        }

        String principal = auth.getPrincipal().toString();
        log.debug("Vérification accès pour utilisateur: {}, node: {}", principal, nodeCode);

        // Récupérer l'utilisateur
        UserPost user = userHandler.findByEmail(principal).block();
        if (user == null) {
            log.warn("Utilisateur non trouvé: {}", principal);
            return false;
        }

        // Si l'utilisateur n'a pas de projets, accès refusé
        if (user.getProjects() == null || user.getProjects().isEmpty()) {
            log.warn("Utilisateur {} n'a aucun projet assigné", principal);
            return false;
        }

        // Récupérer le projet racine pour ce node
        String rootProjectCode = findRootProjectCode(nodeCode);
        if (rootProjectCode == null) {
            log.warn("Impossible de trouver le projet racine pour le node: {}", nodeCode);
            return false;
        }

        // Vérifier si l'utilisateur a accès à ce projet
        boolean hasAccess = user.getProjects().contains(rootProjectCode);

        log.debug("Accès au projet {} pour node {}: {}", rootProjectCode, nodeCode, hasAccess);
        return hasAccess;
    }

    /**
     * Remonte l'arbre des parents pour trouver le code du projet racine
     */
    private String findRootProjectCode(String nodeCode) {
        String currentCode = nodeCode;
        int maxDepth = 50; // Sécurité pour éviter les boucles infinies
        int depth = 0;

        while (currentCode != null && depth < maxDepth) {
            Node node = nodeHandler.findByCodeAndStatus(currentCode, StatusEnum.SNAPSHOT.name()).block();

            if (node == null) {
                log.warn("Node non trouvé: {}", currentCode);
                return null;
            }

            // Si pas de parentCodeOrigin, on est à la racine
            if (node.getParentCodeOrigin() == null || node.getParentCodeOrigin().isEmpty()) {
                log.debug("Projet racine trouvé: {} pour node: {}", node.getCode(), nodeCode);
                return node.getCode();
            }

            // Remonter au parent
            currentCode = node.getParentCodeOrigin();
            depth++;
        }

        log.warn("Profondeur maximale atteinte pour node: {}", nodeCode);
        return null;
    }

    /**
     * Version avec Mono pour utilisation réactive
     */
    public Mono<Boolean> hasProjectAccessReactive(String nodeCode) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return Mono.just(false);
        }

        String principal = auth.getPrincipal().toString();

        return userHandler.findByEmail(principal)
                .flatMap(user -> {
                    if (user.getProjects() == null || user.getProjects().isEmpty()) {
                        return Mono.just(false);
                    }
                    return findRootProjectCodeReactive(nodeCode)
                            .map(rootCode -> user.getProjects().contains(rootCode));
                })
                .defaultIfEmpty(false);
    }

    /**
     * Version réactive pour remonter l'arbre
     */
    private Mono<String> findRootProjectCodeReactive(String nodeCode) {
        return findRootProjectCodeReactive(nodeCode, 0);
    }

    private Mono<String> findRootProjectCodeReactive(String nodeCode, int depth) {
        if (depth > 50) {
            return Mono.empty();
        }

        return nodeHandler.findByCodeAndStatus(nodeCode, StatusEnum.SNAPSHOT.name())
                .flatMap(node -> {
                    if (node.getParentCodeOrigin() == null || node.getParentCodeOrigin().isEmpty()) {
                        return Mono.just(node.getCode());
                    }
                    return findRootProjectCodeReactive(node.getParentCodeOrigin(), depth + 1);
                });
    }

    /**
     * Vérifie si l'utilisateur a accès à au moins un des nodes dans une liste
     */
    public boolean hasAnyProjectAccess(List<String> nodeCodes) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;

        UserPost user = userHandler.findByEmail(auth.getPrincipal().toString()).block();
        if (user == null || user.getProjects() == null || user.getProjects().isEmpty()) {
            return false;
        }

        return nodeCodes.stream()
                .map(this::findRootProjectCode)
                .anyMatch(rootCode -> rootCode != null && user.getProjects().contains(rootCode));
    }


}