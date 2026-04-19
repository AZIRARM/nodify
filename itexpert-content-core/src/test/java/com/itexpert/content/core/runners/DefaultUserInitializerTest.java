package com.itexpert.content.core.runners;

import com.itexpert.content.core.handlers.UserHandler;
import com.itexpert.content.lib.models.UserPost;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DefaultUserInitializerTest {

    private UserHandler userHandler;
    private DefaultUserInitializer initializer;
    private static final String ADMIN_PASSWORD = "admin123";

    @BeforeEach
    void setUp() {
        userHandler = mock(UserHandler.class);
    }

    @Nested
    class InitTests {

        @Test
        void init_ShouldSaveAdminUser_WhenNoUsersExistAndPasswordProvided() {
            // Given
            initializer = new DefaultUserInitializer(ADMIN_PASSWORD, userHandler);

            when(userHandler.findAll()).thenReturn(Flux.empty());
            when(userHandler.save(any(UserPost.class))).thenAnswer(invocation -> {
                UserPost user = invocation.getArgument(0);
                return Mono.just(user);
            });

            // When
            Mono<Void> result = initializer.init();

            // Then
            StepVerifier.create(result)
                    .verifyComplete();

            verify(userHandler, times(1)).findAll();
            verify(userHandler, times(1)).save(argThat(user -> {
                assertEquals("admin", user.getEmail());
                assertEquals("admin", user.getFirstname());
                assertEquals("admin", user.getLastname());
                assertEquals(ADMIN_PASSWORD, user.getPassword());
                assertNotNull(user.getRoles());
                assertEquals(1, user.getRoles().size());
                assertEquals("ADMIN", user.getRoles().get(0));
                return true;
            }));
        }

        @Test
        void init_ShouldNotSaveAdminUser_WhenUsersAlreadyExist() {
            // Given
            initializer = new DefaultUserInitializer(ADMIN_PASSWORD, userHandler);

            UserPost existingUser = new UserPost();
            existingUser.setEmail("existing@test.com");

            when(userHandler.findAll()).thenReturn(Flux.just(existingUser));

            // When
            Mono<Void> result = initializer.init();

            // Then
            StepVerifier.create(result)
                    .verifyComplete();

            verify(userHandler, times(1)).findAll();
            verify(userHandler, never()).save(any(UserPost.class));
        }

        @Test
        void init_ShouldDoNothing_WhenPasswordIsNull() {
            // Given
            initializer = new DefaultUserInitializer(null, userHandler);

            // When
            Mono<Void> result = initializer.init();

            // Then
            StepVerifier.create(result)
                    .verifyComplete();

            verify(userHandler, never()).findAll();
            verify(userHandler, never()).save(any(UserPost.class));
        }

        @Test
        void init_ShouldDoNothing_WhenPasswordIsEmpty() {
            // Given
            initializer = new DefaultUserInitializer("", userHandler);

            // When
            Mono<Void> result = initializer.init();

            // Then
            StepVerifier.create(result)
                    .verifyComplete();

            verify(userHandler, never()).findAll();
            verify(userHandler, never()).save(any(UserPost.class));
        }

        @Test
        void init_ShouldDoNothing_WhenPasswordIsBlank() {
            // Given
            initializer = new DefaultUserInitializer("   ", userHandler);
            when(userHandler.findAll()).thenReturn(Flux.empty());
            when(userHandler.save(any(UserPost.class))).thenReturn(Mono.empty());

            // When
            Mono<Void> result = initializer.init();

            // Then
            StepVerifier.create(result)
                    .verifyComplete();

            verify(userHandler, times(1)).findAll();
            verify(userHandler, times(1)).save(any(UserPost.class));
        }

        @Test
        void init_ShouldHandleError_WhenSaveFails() {
            // Given
            initializer = new DefaultUserInitializer(ADMIN_PASSWORD, userHandler);

            when(userHandler.findAll()).thenReturn(Flux.empty());
            when(userHandler.save(any(UserPost.class)))
                    .thenReturn(Mono.error(new RuntimeException("Database error")));

            // When
            Mono<Void> result = initializer.init();

            // Then
            StepVerifier.create(result)
                    .verifyComplete();

            verify(userHandler, times(1)).findAll();
            verify(userHandler, times(1)).save(any(UserPost.class));
        }

        @Test
        void init_ShouldSetCorrectAdminRoles() {
            // Given
            initializer = new DefaultUserInitializer(ADMIN_PASSWORD, userHandler);

            when(userHandler.findAll()).thenReturn(Flux.empty());
            when(userHandler.save(any(UserPost.class))).thenAnswer(invocation -> {
                UserPost user = invocation.getArgument(0);
                return Mono.just(user);
            });

            // When
            Mono<Void> result = initializer.init();

            // Then
            StepVerifier.create(result)
                    .verifyComplete();

            verify(userHandler, times(1)).save(argThat(user -> {
                List<String> roles = user.getRoles();
                return roles != null &&
                        roles.size() == 1 &&
                        "ADMIN".equals(roles.get(0));
            }));
        }

        @Test
        void init_ShouldSetCorrectAdminEmailAndNames() {
            // Given
            initializer = new DefaultUserInitializer(ADMIN_PASSWORD, userHandler);

            when(userHandler.findAll()).thenReturn(Flux.empty());
            when(userHandler.save(any(UserPost.class))).thenAnswer(invocation -> {
                UserPost user = invocation.getArgument(0);
                return Mono.just(user);
            });

            // When
            Mono<Void> result = initializer.init();

            // Then
            StepVerifier.create(result)
                    .verifyComplete();

            verify(userHandler, times(1)).save(argThat(user -> "admin".equals(user.getEmail()) &&
                    "admin".equals(user.getFirstname()) &&
                    "admin".equals(user.getLastname())));
        }
    }

    @Nested
    class EdgeCasesTests {

        @Test
        void init_ShouldHandleFindAllReturningError() {
            // Given
            initializer = new DefaultUserInitializer(ADMIN_PASSWORD, userHandler);

            when(userHandler.findAll()).thenReturn(Flux.error(new RuntimeException("Find error")));

            // When
            Mono<Void> result = initializer.init();

            // Then
            StepVerifier.create(result)
                    .expectError(RuntimeException.class)
                    .verify();

            verify(userHandler, times(1)).findAll();
            verify(userHandler, never()).save(any(UserPost.class));
        }

        @Test
        void init_ShouldHandleFindAllReturningEmpty() {
            // Given
            initializer = new DefaultUserInitializer(ADMIN_PASSWORD, userHandler);

            when(userHandler.findAll()).thenReturn(Flux.empty());
            when(userHandler.save(any(UserPost.class))).thenReturn(Mono.empty());

            // When
            Mono<Void> result = initializer.init();

            // Then
            StepVerifier.create(result)
                    .verifyComplete();

            verify(userHandler, times(1)).findAll();
            verify(userHandler, times(1)).save(any(UserPost.class));
        }

        @Test
        void init_ShouldHandleMultipleExistingUsers() {
            // Given
            initializer = new DefaultUserInitializer(ADMIN_PASSWORD, userHandler);

            UserPost user1 = new UserPost();
            user1.setEmail("user1@test.com");
            UserPost user2 = new UserPost();
            user2.setEmail("user2@test.com");

            when(userHandler.findAll()).thenReturn(Flux.just(user1, user2));

            // When
            Mono<Void> result = initializer.init();

            // Then
            StepVerifier.create(result)
                    .verifyComplete();

            verify(userHandler, times(1)).findAll();
            verify(userHandler, never()).save(any(UserPost.class));
        }

        @Test
        void init_ShouldHandleSaveReturningEmpty() {
            // Given
            initializer = new DefaultUserInitializer(ADMIN_PASSWORD, userHandler);

            when(userHandler.findAll()).thenReturn(Flux.empty());
            when(userHandler.save(any(UserPost.class))).thenReturn(Mono.empty());

            // When
            Mono<Void> result = initializer.init();

            // Then
            StepVerifier.create(result)
                    .verifyComplete();

            verify(userHandler, times(1)).save(any(UserPost.class));
        }

        @Test
        void init_ShouldHandleSpecialCharactersInPassword() {
            // Given
            String specialPassword = "P@$$w0rd!@#$%^&*()";
            initializer = new DefaultUserInitializer(specialPassword, userHandler);

            when(userHandler.findAll()).thenReturn(Flux.empty());
            when(userHandler.save(any(UserPost.class))).thenAnswer(invocation -> {
                UserPost user = invocation.getArgument(0);
                return Mono.just(user);
            });

            // When
            Mono<Void> result = initializer.init();

            // Then
            StepVerifier.create(result)
                    .verifyComplete();

            verify(userHandler, times(1)).save(argThat(user -> specialPassword.equals(user.getPassword())));
        }

        @Test
        void init_ShouldHandleVeryLongPassword() {
            // Given
            String longPassword = "a".repeat(1000);
            initializer = new DefaultUserInitializer(longPassword, userHandler);

            when(userHandler.findAll()).thenReturn(Flux.empty());
            when(userHandler.save(any(UserPost.class))).thenAnswer(invocation -> {
                UserPost user = invocation.getArgument(0);
                return Mono.just(user);
            });

            // When
            Mono<Void> result = initializer.init();

            // Then
            StepVerifier.create(result)
                    .verifyComplete();

            verify(userHandler, times(1)).save(argThat(user -> longPassword.equals(user.getPassword())));
        }
    }
}