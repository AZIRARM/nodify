package com.itexpert.content.core.helpers;

import com.itexpert.content.core.handlers.UserHandler;
import com.itexpert.content.lib.models.UserPost;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthorizationSecurity {

    private final UserHandler userHandler; // Votre handler pour récupérer l'utilisateur

    /**
     * Vérifie si l'utilisateur a le droit de modifier un contenu
     *
     * @param projectCode             Le code du projet à modifier
     * @param authenticationPrincipal Le principal de l'authentification
     * @return true si l'utilisateur peut modifier
     */
    public boolean canModify(String projectCode, String authenticationPrincipal) {
        try {
            // 1. Récupérer l'utilisateur complet
            UserPost user = userHandler.findByEmail(authenticationPrincipal).block();

            if (user == null) {
                log.warn("Utilisateur non trouvé: {}", authenticationPrincipal);
                return false;
            }

            log.debug("Vérification droits pour utilisateur: {}, rôles: {}, projets: {}",
                    user.getEmail(), user.getRoles(), user.getProjects());

            // 2. Vérifier les rôles
            if (!hasEditRole(user)) {
                log.warn("Utilisateur {} n'a pas le rôle approprié pour modifier. Rôles: {}",
                        user.getEmail(), user.getRoles());
                return false;
            }

            // 3. Vérifier les droits sur le projet
            if (!canAccessProject(user, projectCode)) {
                log.warn("Utilisateur {} n'a pas accès au projet {}. Projets autorisés: {}",
                        user.getEmail(), projectCode, user.getProjects());
                return false;
            }

            return true;

        } catch (Exception e) {
            log.error("Erreur lors de la vérification des droits pour {}: {}",
                    authenticationPrincipal, e.getMessage());
            return false;
        }
    }

    /**
     * Vérifie si l'utilisateur peut modifier (sans vérification de projet spécifique)
     */
    public boolean canModify(String authenticationPrincipal) {
        try {
            UserPost user = userHandler.findByEmail(authenticationPrincipal).block();

            if (user == null) {
                log.warn("Utilisateur non trouvé: {}", authenticationPrincipal);
                return false;
            }

            return hasEditRole(user);

        } catch (Exception e) {
            log.error("Erreur vérification droits pour {}: {}", authenticationPrincipal, e.getMessage());
            return false;
        }
    }

    /**
     * Vérifie si l'utilisateur a les rôles permettant la modification
     */
    private boolean hasEditRole(UserPost user) {
        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            return false;
        }

        // Rôles autorisés à modifier
        List<String> allowedRoles = List.of("ADMIN", "EDITOR");

        return user.getRoles().stream()
                .map(String::toUpperCase)
                .anyMatch(allowedRoles::contains);
    }

    /**
     * Vérifie si l'utilisateur peut accéder au projet
     */
    private boolean canAccessProject(UserPost user, String projectCode) {
        // ADMIN a tous les droits
        if (isAdmin(user)) {
            return true;
        }

        // EDITOR doit avoir le projet dans sa liste
        if (user.getProjects() == null || user.getProjects().isEmpty()) {
            return false;
        }

        return user.getProjects().stream()
                .anyMatch(project -> project.equalsIgnoreCase(projectCode));
    }

    /**
     * Vérifie si l'utilisateur est admin
     */
    private boolean isAdmin(UserPost user) {
        return user.getRoles() != null &&
                user.getRoles().stream()
                        .map(String::toUpperCase)
                        .anyMatch(role -> role.equals("ADMIN"));
    }

    /**
     * Version avec vérification plus détaillée et message d'erreur
     */
    public AuthorizationResult checkAuthorization(String projectCode, String authenticationPrincipal) {
        try {
            UserPost user = userHandler.findByEmail(authenticationPrincipal).block();

            if (user == null) {
                return AuthorizationResult.denied("Utilisateur non trouvé: " + authenticationPrincipal);
            }

            // Vérification des rôles
            if (!hasEditRole(user)) {
                return AuthorizationResult.denied(
                        "Rôle insuffisant. Nécessite ADMIN ou EDITOR. Rôles actuels: " + user.getRoles()
                );
            }

            // Vérification du projet pour non-admin
            if (!isAdmin(user) && !canAccessProject(user, projectCode)) {
                return AuthorizationResult.denied(
                        "Accès non autorisé au projet " + projectCode +
                                ". Projets autorisés: " + user.getProjects()
                );
            }

            return AuthorizationResult.allowed(user);

        } catch (Exception e) {
            log.error("Erreur vérification droits", e);
            return AuthorizationResult.denied("Erreur technique: " + e.getMessage());
        }
    }

    /**
     * Vérifie si l'utilisateur est autorisé pour au moins un projet parmi une liste
     */
    public boolean canModifyAnyProject(String authenticationPrincipal, List<String> projectCodes) {
        try {
            UserPost user = userHandler.findByEmail(authenticationPrincipal).block();

            if (user == null || !hasEditRole(user)) {
                return false;
            }

            // Admin peut tout modifier
            if (isAdmin(user)) {
                return true;
            }

            // Vérifier si l'utilisateur a accès à au moins un des projets
            if (user.getProjects() == null || user.getProjects().isEmpty()) {
                return false;
            }

            return projectCodes.stream()
                    .anyMatch(project -> user.getProjects().contains(project));

        } catch (Exception e) {
            log.error("Erreur vérification droits multiples", e);
            return false;
        }
    }

    /**
     * Récupère la liste des projets autorisés pour un utilisateur
     */
    public List<String> getAllowedProjects(String authenticationPrincipal) {
        try {
            UserPost user = userHandler.findByEmail(authenticationPrincipal).block();

            if (user == null) {
                return List.of();
            }

            // Admin a accès à tous les projets (retourne liste vide = tous)
            if (isAdmin(user)) {
                return List.of(); // ou null selon votre convention
            }

            return user.getProjects() != null ? user.getProjects() : List.of();

        } catch (Exception e) {
            log.error("Erreur récupération projets autorisés", e);
            return List.of();
        }
    }

    /**
     * Classe pour retourner un résultat détaillé
     */
    public static class AuthorizationResult {
        private final boolean allowed;
        private final String message;
        private final UserPost user;

        private AuthorizationResult(boolean allowed, String message, UserPost user) {
            this.allowed = allowed;
            this.message = message;
            this.user = user;
        }

        public static AuthorizationResult allowed(UserPost user) {
            return new AuthorizationResult(true, "OK", user);
        }

        public static AuthorizationResult denied(String message) {
            return new AuthorizationResult(false, message, null);
        }

        public boolean isAllowed() {
            return allowed;
        }

        public String getMessage() {
            return message;
        }

        public UserPost getUser() {
            return user;
        }
    }
}
