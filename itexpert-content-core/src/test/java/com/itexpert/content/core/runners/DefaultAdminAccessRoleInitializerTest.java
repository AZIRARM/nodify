package com.itexpert.content.core.runners;

import com.itexpert.content.core.handlers.AccessRoleHandler;
import com.itexpert.content.core.models.AccessRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DefaultAdminAccessRoleInitializerTest {

    private AccessRoleHandler accessRoleHandler;
    private DefaultAdminAccessRoleInitializer initializer;

    @BeforeEach
    void setUp() {
        accessRoleHandler = mock(AccessRoleHandler.class);
        initializer = new DefaultAdminAccessRoleInitializer(accessRoleHandler);
    }

    @Nested
    class InitTests {

        @Test
        void init_ShouldSaveDefaultRoles_WhenNoRolesExist() {
            // Given
            when(accessRoleHandler.findAll()).thenReturn(Flux.empty());
            when(accessRoleHandler.saveAll(anyList())).thenReturn(Flux.just(
                    createRole("ADMIN", "Administrator", "Administrator role"),
                    createRole("EDITOR", "Editor", "Editor role"),
                    createRole("READER", "Reader", "Reader role")));

            // When
            Mono<Void> result = initializer.init();

            // Then
            StepVerifier.create(result)
                    .verifyComplete();

            ArgumentCaptor<List<AccessRole>> captor = ArgumentCaptor.forClass(List.class);
            verify(accessRoleHandler, times(1)).saveAll(captor.capture());

            List<AccessRole> savedRoles = captor.getValue();
            assertEquals(3, savedRoles.size());

            // Verify ADMIN role
            AccessRole adminRole = savedRoles.get(0);
            assertEquals("ADMIN", adminRole.getCode());
            assertEquals("Administrator", adminRole.getName());
            assertEquals("Administrator role", adminRole.getDescription());

            // Verify EDITOR role
            AccessRole editorRole = savedRoles.get(1);
            assertEquals("EDITOR", editorRole.getCode());
            assertEquals("Editor", editorRole.getName());
            assertEquals("Editor role", editorRole.getDescription());

            // Verify READER role
            AccessRole readerRole = savedRoles.get(2);
            assertEquals("READER", readerRole.getCode());
            assertEquals("Reader", readerRole.getName());
            assertEquals("Reader role", readerRole.getDescription());

            verify(accessRoleHandler, times(1)).findAll();
        }

        @Test
        void init_ShouldNotSaveRoles_WhenRolesAlreadyExist() {
            // Given
            AccessRole existingRole = new AccessRole();
            existingRole.setCode("ADMIN");

            when(accessRoleHandler.findAll()).thenReturn(Flux.just(existingRole));

            // When
            Mono<Void> result = initializer.init();

            // Then
            StepVerifier.create(result)
                    .verifyComplete();

            verify(accessRoleHandler, times(1)).findAll();
            verify(accessRoleHandler, never()).saveAll(anyList());
        }

        @Test
        void init_ShouldContinueOnError_WhenSaveFails() {
            // Given
            when(accessRoleHandler.findAll()).thenReturn(Flux.empty());
            when(accessRoleHandler.saveAll(anyList()))
                    .thenReturn(Flux.error(new RuntimeException("Database error")));

            // When
            Mono<Void> result = initializer.init();

            // Then
            StepVerifier.create(result)
                    .verifyComplete();

            verify(accessRoleHandler, times(1)).findAll();
            verify(accessRoleHandler, times(1)).saveAll(anyList());
        }

        @Test
        void init_ShouldHandleEmptyFlux_WhenFindAllReturnsEmpty() {
            // Given
            when(accessRoleHandler.findAll()).thenReturn(Flux.empty());
            when(accessRoleHandler.saveAll(anyList())).thenReturn(Flux.empty());

            // When
            Mono<Void> result = initializer.init();

            // Then
            StepVerifier.create(result)
                    .verifyComplete();

            verify(accessRoleHandler, times(1)).findAll();
            verify(accessRoleHandler, times(1)).saveAll(anyList());
        }

        @Test
        void init_ShouldSaveOnlyNewRoles_WhenSomeRolesExist() {
            // Given
            AccessRole existingAdminRole = new AccessRole();
            existingAdminRole.setCode("ADMIN");

            when(accessRoleHandler.findAll()).thenReturn(Flux.just(existingAdminRole));

            // When
            Mono<Void> result = initializer.init();

            // Then
            StepVerifier.create(result)
                    .verifyComplete();

            verify(accessRoleHandler, times(1)).findAll();
            verify(accessRoleHandler, never()).saveAll(anyList());
        }

        @Test
        void init_ShouldHandleMultipleErrors_WhenSaveAllFailsForSomeRoles() {
            // Given
            when(accessRoleHandler.findAll()).thenReturn(Flux.empty());
            when(accessRoleHandler.saveAll(anyList()))
                    .thenReturn(Flux.just(createRole("ADMIN", "Administrator", "Administrator role"))
                            .concatWith(Flux.error(new RuntimeException("Error saving EDITOR")))
                            .onErrorResume(e -> Mono.empty()));

            // When
            Mono<Void> result = initializer.init();

            // Then
            StepVerifier.create(result)
                    .verifyComplete();

            verify(accessRoleHandler, times(1)).findAll();
            verify(accessRoleHandler, times(1)).saveAll(anyList());
        }
    }

    @Nested
    class RoleCreationTests {

        @Test
        void init_ShouldCreateRolesWithCorrectValues() {
            // Given
            when(accessRoleHandler.findAll()).thenReturn(Flux.empty());

            ArgumentCaptor<List<AccessRole>> captor = ArgumentCaptor.forClass(List.class);
            when(accessRoleHandler.saveAll(captor.capture())).thenReturn(Flux.empty());

            // When
            initializer.init().block();

            // Then
            List<AccessRole> roles = captor.getValue();

            assertNotNull(roles);
            assertEquals(3, roles.size());

            // Verify ADMIN role
            AccessRole adminRole = roles.stream()
                    .filter(r -> "ADMIN".equals(r.getCode()))
                    .findFirst()
                    .orElse(null);
            assertNotNull(adminRole);
            assertEquals("Administrator", adminRole.getName());
            assertEquals("Administrator role", adminRole.getDescription());

            // Verify EDITOR role
            AccessRole editorRole = roles.stream()
                    .filter(r -> "EDITOR".equals(r.getCode()))
                    .findFirst()
                    .orElse(null);
            assertNotNull(editorRole);
            assertEquals("Editor", editorRole.getName());
            assertEquals("Editor role", editorRole.getDescription());

            // Verify READER role
            AccessRole readerRole = roles.stream()
                    .filter(r -> "READER".equals(r.getCode()))
                    .findFirst()
                    .orElse(null);
            assertNotNull(readerRole);
            assertEquals("Reader", readerRole.getName());
            assertEquals("Reader role", readerRole.getDescription());
        }
    }

    @Nested
    class EdgeCasesTests {

        @Test
        void init_ShouldHandleFindAllReturningEmpty() {
            // Given
            when(accessRoleHandler.findAll()).thenReturn(Flux.empty());
            when(accessRoleHandler.saveAll(anyList())).thenReturn(Flux.just(
                    createRole("ADMIN", "Administrator", "Administrator role"),
                    createRole("EDITOR", "Editor", "Editor role"),
                    createRole("READER", "Reader", "Reader role")));

            // When
            Mono<Void> result = initializer.init();

            // Then
            StepVerifier.create(result)
                    .verifyComplete();

            verify(accessRoleHandler, times(1)).saveAll(anyList());
        }

        @Test
        void init_ShouldHandleSaveAllReturningNull() {
            // Given
            when(accessRoleHandler.findAll()).thenReturn(Flux.empty());
            when(accessRoleHandler.saveAll(anyList())).thenReturn(null);

            // When
            Mono<Void> result = initializer.init();

            // Then
            StepVerifier.create(result)
                    .expectError(NullPointerException.class)
                    .verify();
        }

        @Test
        void init_ShouldHandleHasElementsWithEmptyFlux() {
            // Given
            when(accessRoleHandler.findAll()).thenReturn(Flux.empty());
            when(accessRoleHandler.saveAll(anyList())).thenReturn(Flux.just(
                    createRole("ADMIN", "Administrator", "Administrator role")));

            // When
            Mono<Void> result = initializer.init();

            // Then
            StepVerifier.create(result)
                    .verifyComplete();

            verify(accessRoleHandler, times(1)).saveAll(anyList());
        }

        @Test
        void init_ShouldHandleHasElementsWithNonEmptyFlux() {
            // Given
            AccessRole existingRole = new AccessRole();
            existingRole.setCode("EXISTING");

            when(accessRoleHandler.findAll()).thenReturn(Flux.just(existingRole));

            // When
            Mono<Void> result = initializer.init();

            // Then
            StepVerifier.create(result)
                    .verifyComplete();

            verify(accessRoleHandler, never()).saveAll(anyList());
        }
    }

    // Helper method to create AccessRole objects
    private AccessRole createRole(String code, String name, String description) {
        AccessRole role = new AccessRole();
        role.setCode(code);
        role.setName(name);
        role.setDescription(description);
        return role;
    }
}
