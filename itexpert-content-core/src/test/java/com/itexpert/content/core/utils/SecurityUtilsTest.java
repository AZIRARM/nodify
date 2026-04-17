package com.itexpert.content.core.utils;

import com.itexpert.content.core.handlers.NodeHandler;
import com.itexpert.content.core.handlers.UserHandler;
import com.itexpert.content.core.models.auth.RoleEnum;
import com.itexpert.content.core.utils.auth.SecurityUtils;
import com.itexpert.content.lib.models.Node;
import com.itexpert.content.lib.models.UserPost;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SecurityUtilsTest {

    private UserHandler userHandler;
    private NodeHandler nodeHandler;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        userHandler = mock(UserHandler.class);
        nodeHandler = mock(NodeHandler.class);
        authentication = mock(Authentication.class);

        new SecurityUtils(userHandler, nodeHandler);
    }

    private <T> Mono<T> withMockContext(Mono<T> source) {
        return source.contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
    }

    @Test
    void hasProjectAccess_DirectAccess_ReturnsTrue() {
        when(authentication.getName()).thenReturn("user@test.com");

        UserPost user = new UserPost();
        user.setProjects(List.of("PROJECT_1"));
        user.setRoles(Collections.emptyList());

        when(userHandler.findByEmail("user@test.com")).thenReturn(Mono.just(user));

        StepVerifier.create(withMockContext(SecurityUtils.hasProjectAccess("PROJECT_1")))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void hasProjectAccess_AdminRole_ReturnsTrue() {
        when(authentication.getName()).thenReturn("admin@test.com");

        UserPost user = new UserPost();
        user.setProjects(List.of("OTHER"));
        user.setRoles(List.of(RoleEnum.ADMIN.name()));

        when(userHandler.findByEmail("admin@test.com")).thenReturn(Mono.just(user));

        StepVerifier.create(withMockContext(SecurityUtils.hasProjectAccess("ANY_NODE")))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void hasProjectAccess_ChildNodeAccess_ReturnsTrue() {
        when(authentication.getName()).thenReturn("user@test.com");

        UserPost user = new UserPost();
        user.setProjects(List.of("PARENT_PROJ"));
        user.setRoles(Collections.emptyList());

        Node childNode = new Node();
        childNode.setCode("CHILD_NODE");

        when(userHandler.findByEmail("user@test.com")).thenReturn(Mono.just(user));
        when(nodeHandler.findAllChildren("PARENT_PROJ")).thenReturn(Flux.just(childNode));

        StepVerifier.create(withMockContext(SecurityUtils.hasProjectAccess("CHILD_NODE")))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void hasAnyProjectAccess_ReturnsTrueIfOneMatches() {
        when(authentication.getName()).thenReturn("user@test.com");

        UserPost user = new UserPost();
        user.setProjects(List.of("PROJ_1"));
        user.setRoles(Collections.emptyList());

        when(userHandler.findByEmail("user@test.com")).thenReturn(Mono.just(user));

        // Correction : Initialiser le mock pour retourner un Flux vide par défaut au
        // lieu de null
        when(nodeHandler.findAllChildren(anyString())).thenReturn(Flux.empty());

        StepVerifier.create(withMockContext(SecurityUtils.hasAnyProjectAccess(List.of("UNKNOWN", "PROJ_1"))))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    @SuppressWarnings("unchecked")
    void hasRole_ReturnsTrueWhenAuthorityExists() {
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_USER");
        when(authentication.getAuthorities()).thenReturn((List) List.of(authority));

        StepVerifier.create(withMockContext(SecurityUtils.hasRole("ROLE_USER")))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    @SuppressWarnings("unchecked")
    void hasAnyRole_ReturnsTrueWhenOneRoleMatches() {
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_USER");
        when(authentication.getAuthorities()).thenReturn((List) List.of(authority));

        StepVerifier.create(withMockContext(SecurityUtils.hasAnyRole("ROLE_ADMIN", "ROLE_USER")))
                .expectNext(true)
                .verifyComplete();
    }
}