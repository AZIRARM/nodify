package com.itexpert.content.core.utils.auth;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import com.itexpert.content.core.handlers.NodeHandler;
import com.itexpert.content.core.handlers.UserHandler;
import com.itexpert.content.core.models.auth.RoleEnum;
import com.itexpert.content.lib.enums.StatusEnum;

import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class SecurityUtils {

    private static UserHandler userHandler;
    private static NodeHandler nodeHandler;

    // Static injection via constructor
    public SecurityUtils(UserHandler userHandler, NodeHandler nodeHandler) {
        SecurityUtils.userHandler = userHandler;
        SecurityUtils.nodeHandler = nodeHandler;
    }

    public static Mono<Authentication> getAuthentication() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication);
    }

    public static Mono<String> getUsername() {
        return getAuthentication()
                .map(Authentication::getName);
    }

    /**
     * Checks if the current user has access to the project associated with the
     * given node code.
     * Operates in a non-blocking reactive way using the ReactiveSecurityContext.
     */
    public static Mono<Boolean> hasProjectAccess(String nodeCode) {
        if (nodeCode == null || nodeCode.isEmpty()) {
            return Mono.just(false);
        }

        return getUsername()
                .flatMap(userHandler::findByEmail)
                .flatMap(user -> {
                    List<String> authorizedProjects = user.getProjects();

                    if (authorizedProjects == null || authorizedProjects.isEmpty()) {
                        return Mono.just(false);
                    }

                    if (authorizedProjects.contains(nodeCode)) {
                        return Mono.just(true);
                    }

                    if (user.getRoles().contains(RoleEnum.ADMIN.name())) {
                        return Mono.just(true);
                    }

                    return Flux.fromIterable(authorizedProjects)
                            .flatMap(nodeHandler::findAllChildren)
                            .any(childNode -> childNode.getCode().equals(nodeCode));
                })
                .defaultIfEmpty(false);
    }

    /**
     * Checks if the user has access to at least one of the projects in the provided
     * list of node codes.
     */
    public static Mono<Boolean> hasAnyProjectAccess(List<String> nodeCodes) {
        return reactor.core.publisher.Flux.fromIterable(nodeCodes)
                .flatMap(SecurityUtils::hasProjectAccess)
                .any(access -> access); // Returns true if any access is true
    }

    /**
     * Internal recursive method to find the root project code by traversing up the
     * parent tree.
     */
    private static Mono<String> findRootProjectCode(String nodeCode, int depth) {
        if (depth > 50) {
            return Mono.empty();
        }

        return nodeHandler.findByCodeAndStatus(nodeCode, StatusEnum.SNAPSHOT.name())
                .flatMap(node -> {
                    // Root is found if there is no parent origin code
                    if (node.getParentCodeOrigin() == null || node.getParentCodeOrigin().isEmpty()) {
                        return Mono.just(node.getCode());
                    }
                    return findRootProjectCode(node.getParentCodeOrigin(), depth + 1);
                });
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