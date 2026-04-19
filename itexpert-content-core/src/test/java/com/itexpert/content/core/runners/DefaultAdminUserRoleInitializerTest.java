package com.itexpert.content.core.runners;

import com.itexpert.content.core.handlers.UserRoleHandler;
import com.itexpert.content.core.models.auth.RoleEnum;
import com.itexpert.content.lib.models.UserRole;
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

class DefaultAdminUserRoleInitializerTest {

    private UserRoleHandler userRoleHandler;
    private DefaultAdminUserRoleInitializer initializer;

    @BeforeEach
    void setUp() {
        userRoleHandler = mock(UserRoleHandler.class);
        initializer = new DefaultAdminUserRoleInitializer(userRoleHandler);
    }

    @Nested
    class InitTests {

        @Test
        void init_ShouldSaveDefaultRoles_WhenNoRolesExist() {
            // Given
            when(userRoleHandler.findAll()).thenReturn(Flux.empty());
            when(userRoleHandler.saveAll(anyList())).thenReturn(Flux.just(
                    createUserRole(RoleEnum.ADMIN.name()),
                    createUserRole(RoleEnum.EDITOR.name()),
                    createUserRole(RoleEnum.READER.name())));

            // When
            Mono<Void> result = initializer.init();

            // Then
            StepVerifier.create(result)
                    .verifyComplete();

            ArgumentCaptor<List<UserRole>> captor = ArgumentCaptor.forClass(List.class);
            verify(userRoleHandler, times(1)).saveAll(captor.capture());

            List<UserRole> savedRoles = captor.getValue();
            assertEquals(3, savedRoles.size());

            // Verify ADMIN role
            UserRole adminRole = savedRoles.get(0);
            assertEquals(RoleEnum.ADMIN.name(), adminRole.getCode());

            // Verify EDITOR role
            UserRole editorRole = savedRoles.get(1);
            assertEquals(RoleEnum.EDITOR.name(), editorRole.getCode());

            // Verify READER role
            UserRole readerRole = savedRoles.get(2);
            assertEquals(RoleEnum.READER.name(), readerRole.getCode());

            verify(userRoleHandler, times(1)).findAll();
        }

        @Test
        void init_ShouldNotSaveRoles_WhenRolesAlreadyExist() {
            // Given
            UserRole existingRole = new UserRole();
            existingRole.setCode(RoleEnum.ADMIN.name());

            when(userRoleHandler.findAll()).thenReturn(Flux.just(existingRole));

            // When
            Mono<Void> result = initializer.init();

            // Then
            StepVerifier.create(result)
                    .verifyComplete();

            verify(userRoleHandler, times(1)).findAll();
            verify(userRoleHandler, never()).saveAll(anyList());
        }

        @Test
        void init_ShouldContinueOnError_WhenSaveFails() {
            // Given
            when(userRoleHandler.findAll()).thenReturn(Flux.empty());
            when(userRoleHandler.saveAll(anyList()))
                    .thenReturn(Flux.error(new RuntimeException("Database error")));

            // When
            Mono<Void> result = initializer.init();

            // Then
            StepVerifier.create(result)
                    .verifyComplete();

            verify(userRoleHandler, times(1)).findAll();
            verify(userRoleHandler, times(1)).saveAll(anyList());
        }

        @Test
        void init_ShouldHandleEmptyFlux_WhenFindAllReturnsEmpty() {
            // Given
            when(userRoleHandler.findAll()).thenReturn(Flux.empty());
            when(userRoleHandler.saveAll(anyList())).thenReturn(Flux.empty());

            // When
            Mono<Void> result = initializer.init();

            // Then
            StepVerifier.create(result)
                    .verifyComplete();

            verify(userRoleHandler, times(1)).findAll();
            verify(userRoleHandler, times(1)).saveAll(anyList());
        }

        @Test
        void init_ShouldSaveOnlyNewRoles_WhenSomeRolesExist() {
            // Given
            UserRole existingAdminRole = new UserRole();
            existingAdminRole.setCode(RoleEnum.ADMIN.name());

            when(userRoleHandler.findAll()).thenReturn(Flux.just(existingAdminRole));

            // When
            Mono<Void> result = initializer.init();

            // Then
            StepVerifier.create(result)
                    .verifyComplete();

            verify(userRoleHandler, times(1)).findAll();
            verify(userRoleHandler, never()).saveAll(anyList());
        }

        @Test
        void init_ShouldHandleMultipleErrors_WhenSaveAllFailsForSomeRoles() {
            // Given
            when(userRoleHandler.findAll()).thenReturn(Flux.empty());
            when(userRoleHandler.saveAll(anyList()))
                    .thenReturn(Flux.just(createUserRole(RoleEnum.ADMIN.name()))
                            .concatWith(Flux.error(new RuntimeException("Error saving EDITOR")))
                            .onErrorResume(e -> Mono.empty()));

            // When
            Mono<Void> result = initializer.init();

            // Then
            StepVerifier.create(result)
                    .verifyComplete();

            verify(userRoleHandler, times(1)).findAll();
            verify(userRoleHandler, times(1)).saveAll(anyList());
        }
    }

    @Nested
    class RoleCreationTests {

        @Test
        void init_ShouldCreateRolesWithCorrectCodes() {
            // Given
            when(userRoleHandler.findAll()).thenReturn(Flux.empty());

            ArgumentCaptor<List<UserRole>> captor = ArgumentCaptor.forClass(List.class);
            when(userRoleHandler.saveAll(captor.capture())).thenReturn(Flux.empty());

            // When
            initializer.init().block();

            // Then
            List<UserRole> roles = captor.getValue();

            assertNotNull(roles);
            assertEquals(3, roles.size());

            // Verify ADMIN role
            boolean hasAdmin = roles.stream()
                    .anyMatch(r -> RoleEnum.ADMIN.name().equals(r.getCode()));
            assertTrue(hasAdmin, "Should contain ADMIN role");

            // Verify EDITOR role
            boolean hasEditor = roles.stream()
                    .anyMatch(r -> RoleEnum.EDITOR.name().equals(r.getCode()));
            assertTrue(hasEditor, "Should contain EDITOR role");

            // Verify READER role
            boolean hasReader = roles.stream()
                    .anyMatch(r -> RoleEnum.READER.name().equals(r.getCode()));
            assertTrue(hasReader, "Should contain READER role");
        }

        @Test
        void init_ShouldCreateExactlyThreeRoles() {
            // Given
            when(userRoleHandler.findAll()).thenReturn(Flux.empty());

            ArgumentCaptor<List<UserRole>> captor = ArgumentCaptor.forClass(List.class);
            when(userRoleHandler.saveAll(captor.capture())).thenReturn(Flux.empty());

            // When
            initializer.init().block();

            // Then
            List<UserRole> roles = captor.getValue();
            assertEquals(3, roles.size(), "Should create exactly 3 roles");
        }
    }

    @Nested
    class EdgeCasesTests {

        @Test
        void init_ShouldHandleFindAllReturningEmpty() {
            // Given
            when(userRoleHandler.findAll()).thenReturn(Flux.empty());
            when(userRoleHandler.saveAll(anyList())).thenReturn(Flux.just(
                    createUserRole(RoleEnum.ADMIN.name()),
                    createUserRole(RoleEnum.EDITOR.name()),
                    createUserRole(RoleEnum.READER.name())));

            // When
            Mono<Void> result = initializer.init();

            // Then
            StepVerifier.create(result)
                    .verifyComplete();

            verify(userRoleHandler, times(1)).saveAll(anyList());
        }

        @Test
        void init_ShouldHandleSaveAllReturningEmpty() {
            // Given
            when(userRoleHandler.findAll()).thenReturn(Flux.empty());
            when(userRoleHandler.saveAll(anyList())).thenReturn(Flux.empty());

            // When
            Mono<Void> result = initializer.init();

            // Then
            StepVerifier.create(result)
                    .verifyComplete();

            verify(userRoleHandler, times(1)).saveAll(anyList());
        }

        @Test
        void init_ShouldHandleHasElementsWithEmptyFlux() {
            // Given
            when(userRoleHandler.findAll()).thenReturn(Flux.empty());
            when(userRoleHandler.saveAll(anyList())).thenReturn(Flux.just(
                    createUserRole(RoleEnum.ADMIN.name())));

            // When
            Mono<Void> result = initializer.init();

            // Then
            StepVerifier.create(result)
                    .verifyComplete();

            verify(userRoleHandler, times(1)).saveAll(anyList());
        }

        @Test
        void init_ShouldHandleHasElementsWithNonEmptyFlux() {
            // Given
            UserRole existingRole = new UserRole();
            existingRole.setCode(RoleEnum.ADMIN.name());

            when(userRoleHandler.findAll()).thenReturn(Flux.just(existingRole));

            // When
            Mono<Void> result = initializer.init();

            // Then
            StepVerifier.create(result)
                    .verifyComplete();

            verify(userRoleHandler, never()).saveAll(anyList());
        }

        @Test
        void init_ShouldHandleSaveErrorOnFirstRole() {
            // Given
            when(userRoleHandler.findAll()).thenReturn(Flux.empty());
            when(userRoleHandler.saveAll(anyList()))
                    .thenReturn(Flux.error(new RuntimeException("Save failed")));

            // When
            Mono<Void> result = initializer.init();

            // Then
            StepVerifier.create(result)
                    .verifyComplete();

            verify(userRoleHandler, times(1)).saveAll(anyList());
        }

        @Test
        void init_ShouldHandleFindAllWithMultipleExistingRoles() {
            // Given
            UserRole existingAdminRole = new UserRole();
            existingAdminRole.setCode(RoleEnum.ADMIN.name());

            UserRole existingEditorRole = new UserRole();
            existingEditorRole.setCode(RoleEnum.EDITOR.name());

            when(userRoleHandler.findAll()).thenReturn(Flux.just(existingAdminRole, existingEditorRole));

            // When
            Mono<Void> result = initializer.init();

            // Then
            StepVerifier.create(result)
                    .verifyComplete();

            verify(userRoleHandler, never()).saveAll(anyList());
        }
    }

    // Helper method to create UserRole objects
    private UserRole createUserRole(String code) {
        UserRole role = new UserRole();
        role.setCode(code);
        return role;
    }
}