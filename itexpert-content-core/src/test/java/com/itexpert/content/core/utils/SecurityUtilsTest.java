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

        StepVerifier.create(withMockContext(SecurityUtils.hasProjectAccess("PROJECT_1", null)))
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

        StepVerifier.create(withMockContext(SecurityUtils.hasProjectAccess("ANY_NODE", null)))
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

        StepVerifier.create(withMockContext(SecurityUtils.hasProjectAccess("CHILD_NODE", null)))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void hasProjectAccess_NewNodeWithParentAccess_ReturnsTrue() {
        when(authentication.getName()).thenReturn("user@test.com");

        UserPost user = new UserPost();
        user.setProjects(List.of("PARENT_PROJ"));
        user.setRoles(Collections.emptyList());

        when(userHandler.findByEmail("user@test.com")).thenReturn(Mono.just(user));

        StepVerifier.create(withMockContext(SecurityUtils.hasProjectAccess("NEW_NODE", "PARENT_PROJ")))
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
        when(nodeHandler.findAllChildren(anyString())).thenReturn(Flux.empty());

        StepVerifier.create(withMockContext(SecurityUtils.hasAnyProjectAccess(List.of("UNKNOWN", "PROJ_1"))))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void hasAnyProjectAccess_NoAccess_ReturnsFalse() {
        when(authentication.getName()).thenReturn("user@test.com");

        UserPost user = new UserPost();
        user.setProjects(List.of("PROJ_1"));
        user.setRoles(Collections.emptyList());

        when(userHandler.findByEmail("user@test.com")).thenReturn(Mono.just(user));
        when(nodeHandler.findAllChildren(anyString())).thenReturn(Flux.empty());

        StepVerifier.create(withMockContext(SecurityUtils.hasAnyProjectAccess(List.of("UNKNOWN_1", "UNKNOWN_2"))))
                .expectNext(false)
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
    void hasRole_ReturnsFalseWhenAuthorityDoesNotExist() {
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_USER");
        when(authentication.getAuthorities()).thenReturn((List) List.of(authority));

        StepVerifier.create(withMockContext(SecurityUtils.hasRole("ROLE_ADMIN")))
                .expectNext(false)
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

    @Test
    @SuppressWarnings("unchecked")
    void hasAnyRole_ReturnsFalseWhenNoRoleMatches() {
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_USER");
        when(authentication.getAuthorities()).thenReturn((List) List.of(authority));

        StepVerifier.create(withMockContext(SecurityUtils.hasAnyRole("ROLE_ADMIN", "ROLE_MANAGER")))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    @SuppressWarnings("unchecked")
    void hasAllRoles_ReturnsTrueWhenAllRolesMatch() {
        SimpleGrantedAuthority authority1 = new SimpleGrantedAuthority("ROLE_ADMIN");
        SimpleGrantedAuthority authority2 = new SimpleGrantedAuthority("ROLE_USER");
        when(authentication.getAuthorities()).thenReturn((List) List.of(authority1, authority2));

        StepVerifier.create(withMockContext(SecurityUtils.hasAllRoles("ROLE_ADMIN", "ROLE_USER")))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    @SuppressWarnings("unchecked")
    void hasAllRoles_ReturnsFalseWhenNotAllRolesMatch() {
        SimpleGrantedAuthority authority1 = new SimpleGrantedAuthority("ROLE_ADMIN");
        when(authentication.getAuthorities()).thenReturn((List) List.of(authority1));

        StepVerifier.create(withMockContext(SecurityUtils.hasAllRoles("ROLE_ADMIN", "ROLE_USER")))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void hasProjectAccess_NewNodeWithGrandParentAccess_ReturnsTrue() {
        when(authentication.getName()).thenReturn("user@test.com");

        UserPost user = new UserPost();
        user.setProjects(List.of("GRAND_PARENT_PROJ"));
        user.setRoles(Collections.emptyList());

        Node parentNode = new Node();
        parentNode.setCode("PARENT_PROJ");
        parentNode.setParentCodeOrigin("GRAND_PARENT_PROJ");

        Node grandParentNode = new Node();
        grandParentNode.setCode("GRAND_PARENT_PROJ");
        grandParentNode.setParentCodeOrigin(null);

        when(userHandler.findByEmail("user@test.com")).thenReturn(Mono.just(user));
        // Simuler la recherche des enfants du projet autorisé (GRAND_PARENT_PROJ)
        // Le nouveau nœud (NEW_NODE) n'est pas encore en base, mais son parent direct
        // (PARENT_PROJ)
        // est un enfant de GRAND_PARENT_PROJ
        when(nodeHandler.findAllChildren("GRAND_PARENT_PROJ")).thenReturn(Flux.just(parentNode));

        StepVerifier.create(withMockContext(SecurityUtils.hasProjectAccess("NEW_NODE", "PARENT_PROJ")))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void hasProjectAccess_NewNodeWithGreatGrandParentAccess_ReturnsTrue() {
        when(authentication.getName()).thenReturn("user@test.com");

        UserPost user = new UserPost();
        user.setProjects(List.of("GREAT_GRAND_PARENT_PROJ"));
        user.setRoles(Collections.emptyList());

        Node grandParentNode = new Node();
        grandParentNode.setCode("GRAND_PARENT_PROJ");
        grandParentNode.setParentCodeOrigin("GREAT_GRAND_PARENT_PROJ");

        Node parentNode = new Node();
        parentNode.setCode("PARENT_PROJ");
        parentNode.setParentCodeOrigin("GRAND_PARENT_PROJ");

        when(userHandler.findByEmail("user@test.com")).thenReturn(Mono.just(user));
        when(nodeHandler.findAllChildren("GREAT_GRAND_PARENT_PROJ")).thenReturn(Flux.just(grandParentNode, parentNode));

        StepVerifier.create(withMockContext(SecurityUtils.hasProjectAccess("NEW_NODE", "PARENT_PROJ")))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void hasProjectAccess_NewNodeWithParentAccessViaChildCheck_ReturnsTrue() {
        when(authentication.getName()).thenReturn("user@test.com");

        UserPost user = new UserPost();
        user.setProjects(List.of("PROJECT_A"));
        user.setRoles(Collections.emptyList());

        Node childNode = new Node();
        childNode.setCode("CHILD_OF_PROJECT_A");

        Node grandChildNode = new Node();
        grandChildNode.setCode("GRAND_CHILD_NODE");
        grandChildNode.setParentCodeOrigin("CHILD_OF_PROJECT_A");

        when(userHandler.findByEmail("user@test.com")).thenReturn(Mono.just(user));
        when(nodeHandler.findAllChildren("PROJECT_A")).thenReturn(Flux.just(childNode, grandChildNode));

        // Test avec un parent qui est un enfant indirect (2ème niveau)
        StepVerifier.create(withMockContext(SecurityUtils.hasProjectAccess("NEW_NODE", "GRAND_CHILD_NODE")))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void hasProjectAccess_NewNodeWithParentNotInAuthorizedProjects_ReturnsFalse() {
        when(authentication.getName()).thenReturn("user@test.com");

        UserPost user = new UserPost();
        user.setProjects(List.of("PROJECT_A", "PROJECT_B"));
        user.setRoles(Collections.emptyList());

        Node childOfProjectA = new Node();
        childOfProjectA.setCode("CHILD_A");
        childOfProjectA.setParentCodeOrigin("PROJECT_A");

        Node childOfProjectB = new Node();
        childOfProjectB.setCode("CHILD_B");
        childOfProjectB.setParentCodeOrigin("PROJECT_B");

        when(userHandler.findByEmail("user@test.com")).thenReturn(Mono.just(user));
        when(nodeHandler.findAllChildren("PROJECT_A")).thenReturn(Flux.just(childOfProjectA));
        when(nodeHandler.findAllChildren("PROJECT_B")).thenReturn(Flux.just(childOfProjectB));

        // Le parent "UNKNOWN_PARENT" n'est ni un projet autorisé ni un enfant de projet
        // autorisé
        StepVerifier.create(withMockContext(SecurityUtils.hasProjectAccess("NEW_NODE", "UNKNOWN_PARENT")))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void hasProjectAccess_NewNodeWithParentThatIsChildOfAuthorizedProject_ReturnsTrue() {
        when(authentication.getName()).thenReturn("user@test.com");

        UserPost user = new UserPost();
        user.setProjects(List.of("ROOT_PROJECT"));
        user.setRoles(Collections.emptyList());

        Node level1Node = new Node();
        level1Node.setCode("LEVEL_1");
        level1Node.setParentCodeOrigin("ROOT_PROJECT");

        Node level2Node = new Node();
        level2Node.setCode("LEVEL_2");
        level2Node.setParentCodeOrigin("LEVEL_1");

        Node level3Node = new Node();
        level3Node.setCode("LEVEL_3");
        level3Node.setParentCodeOrigin("LEVEL_2");

        when(userHandler.findByEmail("user@test.com")).thenReturn(Mono.just(user));
        when(nodeHandler.findAllChildren("ROOT_PROJECT")).thenReturn(Flux.just(level1Node, level2Node, level3Node));

        // Le nouveau nœud aura LEVEL_3 comme parent, qui est un enfant au 3ème niveau
        // du projet autorisé
        StepVerifier.create(withMockContext(SecurityUtils.hasProjectAccess("BRAND_NEW_NODE", "LEVEL_3")))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void hasProjectAccess_NewNodeWithMultipleParentsHierarchy_AdminHasAccess() {
        when(authentication.getName()).thenReturn("admin@test.com");

        UserPost user = new UserPost();
        user.setProjects(List.of("SOME_PROJECT"));
        user.setRoles(List.of(RoleEnum.ADMIN.name())); // Admin ignore les restrictions

        when(userHandler.findByEmail("admin@test.com")).thenReturn(Mono.just(user));

        // Même avec un parent inconnu, l'admin doit avoir accès
        StepVerifier.create(withMockContext(SecurityUtils.hasProjectAccess("NEW_NODE", "ANY_PARENT")))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void hasProjectAccess_NewNodeWithoutParentAccess_ReturnsFalse() {
        when(authentication.getName()).thenReturn("user@test.com");

        UserPost user = new UserPost();
        user.setProjects(List.of("OTHER_PROJ"));
        user.setRoles(Collections.emptyList());

        when(userHandler.findByEmail("user@test.com")).thenReturn(Mono.just(user));
        when(nodeHandler.findAllChildren(anyString())).thenReturn(Flux.empty());

        StepVerifier.create(withMockContext(SecurityUtils.hasProjectAccess("NEW_NODE", "PARENT_PROJ")))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void hasProjectAccess_NewNodeWithNullParent_ReturnsFalse() {
        when(authentication.getName()).thenReturn("user@test.com");

        UserPost user = new UserPost();
        user.setProjects(List.of("PROJECT_A"));
        user.setRoles(Collections.emptyList());

        when(userHandler.findByEmail("user@test.com")).thenReturn(Mono.just(user));
        when(nodeHandler.findAllChildren(anyString())).thenReturn(Flux.empty());

        StepVerifier.create(withMockContext(SecurityUtils.hasProjectAccess("NEW_NODE", null)))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void hasProjectAccess_NewNodeWithEmptyParent_ReturnsFalse() {
        when(authentication.getName()).thenReturn("user@test.com");

        UserPost user = new UserPost();
        user.setProjects(List.of("PROJECT_A"));
        user.setRoles(Collections.emptyList());

        when(userHandler.findByEmail("user@test.com")).thenReturn(Mono.just(user));
        when(nodeHandler.findAllChildren(anyString())).thenReturn(Flux.empty());

        StepVerifier.create(withMockContext(SecurityUtils.hasProjectAccess("NEW_NODE", "")))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void hasProjectAccess_NewNodeWithParentBelongsToAuthorizedProjects_ReturnsTrue() {
        when(authentication.getName()).thenReturn("user@test.com");

        UserPost user = new UserPost();
        user.setProjects(List.of("PARENT_PROJ"));
        user.setRoles(Collections.emptyList());

        when(userHandler.findByEmail("user@test.com")).thenReturn(Mono.just(user));
        // Pas besoin de mock pour findAllChildren car on vérifie d'abord
        // authorizedProjects.contains(parentCode)

        StepVerifier.create(withMockContext(SecurityUtils.hasProjectAccess("NEW_NODE", "PARENT_PROJ")))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void hasProjectAccess_NewNodeWithParentDoesNotBelongToAuthorizedProjects_ReturnsFalse() {
        when(authentication.getName()).thenReturn("user@test.com");

        UserPost user = new UserPost();
        user.setProjects(List.of("PROJECT_A", "PROJECT_B"));
        user.setRoles(Collections.emptyList());

        when(userHandler.findByEmail("user@test.com")).thenReturn(Mono.just(user));
        // Mock pour findAllChildren qui ne trouvera pas "PARENT_PROJ" comme enfant
        when(nodeHandler.findAllChildren("PROJECT_A")).thenReturn(Flux.empty());
        when(nodeHandler.findAllChildren("PROJECT_B")).thenReturn(Flux.empty());

        StepVerifier.create(withMockContext(SecurityUtils.hasProjectAccess("NEW_NODE", "PARENT_PROJ")))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void hasProjectAccess_NewNodeWithParentIsChildOfAuthorizedProjects_ReturnsTrue() {
        when(authentication.getName()).thenReturn("user@test.com");

        UserPost user = new UserPost();
        user.setProjects(List.of("ROOT_PROJECT"));
        user.setRoles(Collections.emptyList());

        Node childNode = new Node();
        childNode.setCode("PARENT_PROJ");
        childNode.setParentCodeOrigin("ROOT_PROJECT");

        when(userHandler.findByEmail("user@test.com")).thenReturn(Mono.just(user));
        when(nodeHandler.findAllChildren("ROOT_PROJECT")).thenReturn(Flux.just(childNode));

        StepVerifier.create(withMockContext(SecurityUtils.hasProjectAccess("NEW_NODE", "PARENT_PROJ")))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void hasProjectAccess_NewNodeWithParentIsNotChildOfAuthorizedProjects_ReturnsFalse() {
        when(authentication.getName()).thenReturn("user@test.com");

        UserPost user = new UserPost();
        user.setProjects(List.of("ROOT_PROJECT"));
        user.setRoles(Collections.emptyList());

        Node anotherChild = new Node();
        anotherChild.setCode("OTHER_CHILD");
        anotherChild.setParentCodeOrigin("ROOT_PROJECT");

        when(userHandler.findByEmail("user@test.com")).thenReturn(Mono.just(user));
        when(nodeHandler.findAllChildren("ROOT_PROJECT")).thenReturn(Flux.just(anotherChild));

        StepVerifier.create(withMockContext(SecurityUtils.hasProjectAccess("NEW_NODE", "PARENT_PROJ")))
                .expectNext(false)
                .verifyComplete();
    }
}