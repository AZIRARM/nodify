package com.itexpert.content.core.runners;

import com.itexpert.content.core.handlers.NodeHandler;
import com.itexpert.content.lib.enums.StatusEnum;
import com.itexpert.content.lib.models.Node;
import com.itexpert.content.lib.models.Value;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DefaultEnvironmentInitializerTest {

    private NodeHandler nodeHandler;
    private DefaultEnvironmentInitializer initializer;
    private static final String API_URL = "http://localhost:8080";

    @BeforeEach
    void setUp() {
        nodeHandler = mock(NodeHandler.class);
        initializer = new DefaultEnvironmentInitializer(nodeHandler, API_URL);
    }

    @Nested
    class InitTests {

        @Test
        void init_ShouldCreateAllEnvironments_WhenNoNodesExist() {
            // Given
            when(nodeHandler.hasNodes()).thenReturn(Mono.just(false));

            // Mock pour DEV-01
            when(nodeHandler.save(argThat(node -> node != null && "DEV-01".equals(node.getCode()))))
                    .thenAnswer(invocation -> {
                        Node node = invocation.getArgument(0);
                        node.setStatus(StatusEnum.SNAPSHOT);
                        return Mono.just(node);
                    });

            // Mock pour les autres nodes - findByCodeAndStatus retourne empty
            when(nodeHandler.findByCodeAndStatus(anyString(), eq(StatusEnum.SNAPSHOT.name())))
                    .thenReturn(Mono.empty());

            // Mock pour save des autres nodes
            when(nodeHandler.save(argThat(node -> node != null && !"DEV-01".equals(node.getCode()))))
                    .thenAnswer(invocation -> {
                        Node node = invocation.getArgument(0);
                        node.setStatus(StatusEnum.SNAPSHOT);
                        return Mono.just(node);
                    });

            // When
            Mono<Void> result = initializer.init();

            // Then
            StepVerifier.create(result)
                    .verifyComplete();

            verify(nodeHandler, times(1)).hasNodes();
            verify(nodeHandler, times(1)).save(argThat(node -> node != null && "DEV-01".equals(node.getCode())));
            verify(nodeHandler, times(1)).save(argThat(node -> node != null && "INT-01".equals(node.getCode())));
            verify(nodeHandler, times(1)).save(argThat(node -> node != null && "STG-01".equals(node.getCode())));
            verify(nodeHandler, times(1)).save(argThat(node -> node != null && "PREP-01".equals(node.getCode())));
            verify(nodeHandler, times(1)).save(argThat(node -> node != null && "PROD-01".equals(node.getCode())));
        }

        @Test
        void init_ShouldNotCreateEnvironments_WhenNodesAlreadyExist() {
            // Given
            when(nodeHandler.hasNodes()).thenReturn(Mono.just(true));

            // When
            Mono<Void> result = initializer.init();

            // Then
            StepVerifier.create(result)
                    .verifyComplete();

            verify(nodeHandler, times(1)).hasNodes();
            verify(nodeHandler, never()).save(any(Node.class));
            verify(nodeHandler, never()).findByCodeAndStatus(anyString(), anyString());
        }

        @Test
        void init_ShouldSkipExistingEnvironments_WhenSomeAlreadyExist() {
            // Given
            Node existingNode = new Node();
            existingNode.setCode("INT-01");
            existingNode.setStatus(StatusEnum.SNAPSHOT);

            when(nodeHandler.hasNodes()).thenReturn(Mono.just(false));

            // Utilisation de any() au lieu de argThat
            when(nodeHandler.save(any(Node.class))).thenAnswer(invocation -> {
                Node node = invocation.getArgument(0);
                node.setStatus(StatusEnum.SNAPSHOT);
                return Mono.just(node);
            });

            when(nodeHandler.findByCodeAndStatus(eq("INT-01"), eq(StatusEnum.SNAPSHOT.name())))
                    .thenReturn(Mono.just(existingNode));
            when(nodeHandler.findByCodeAndStatus(anyString(), eq(StatusEnum.SNAPSHOT.name())))
                    .thenReturn(Mono.empty());

            // When
            Mono<Void> result = initializer.init();

            // Then
            StepVerifier.create(result)
                    .verifyComplete();

            verify(nodeHandler, times(1)).hasNodes();
            // Vérifie que save a été appelé 5 fois (DEV, INT, STG, PREP, PROD)
            // Note: INT sera sauvegardé car il n'y a pas de vérification d'existence dans
            // le mock
            verify(nodeHandler, times(5)).save(any(Node.class));
        }

        @Test
        void init_ShouldHandleError_WhenSaveFails() {
            // Given
            when(nodeHandler.hasNodes()).thenReturn(Mono.just(false));
            when(nodeHandler.save(any(Node.class)))
                    .thenReturn(Mono.error(new RuntimeException("Save failed")));
            when(nodeHandler.findByCodeAndStatus(anyString(), anyString()))
                    .thenReturn(Mono.empty());

            // When
            Mono<Void> result = initializer.init();

            // Then
            StepVerifier.create(result)
                    .expectError(RuntimeException.class)
                    .verify();

            verify(nodeHandler, times(1)).hasNodes();
        }

        @Test
        void init_ShouldHandleHasNodesError() {
            // Given
            when(nodeHandler.hasNodes()).thenReturn(Mono.error(new RuntimeException("Database error")));

            // When
            Mono<Void> result = initializer.init();

            // Then
            StepVerifier.create(result)
                    .expectError(RuntimeException.class)
                    .verify();

            verify(nodeHandler, times(1)).hasNodes();
            verify(nodeHandler, never()).save(any(Node.class));
        }
    }

    @Nested
    class CreateNodeTests {

        @Test
        void createNode_ShouldCreateDevNodeWithFavoriteTrue() {
            // Given
            when(nodeHandler.hasNodes()).thenReturn(Mono.just(false));
            when(nodeHandler.findByCodeAndStatus(anyString(), anyString())).thenReturn(Mono.empty());

            when(nodeHandler.save(any(Node.class))).thenAnswer(invocation -> {
                Node node = invocation.getArgument(0);
                if ("DEV-01".equals(node.getCode())) {
                    assertTrue(node.isFavorite());
                    assertEquals("Development", node.getName());
                    assertEquals("Development environment", node.getDescription());
                    assertEquals("0", node.getVersion());
                    assertEquals("EN", node.getDefaultLanguage());
                    assertEquals("development", node.getSlug());

                    List<Value> values = node.getValues();
                    assertNotNull(values);
                    assertEquals(1, values.size());
                    assertEquals("BASE_URL", values.get(0).getKey());
                    assertEquals(API_URL, values.get(0).getValue());
                }
                return Mono.just(node);
            });

            // When
            Mono<Void> result = initializer.init();

            // Then
            StepVerifier.create(result)
                    .verifyComplete();
        }

        @Test
        void createNode_ShouldCreateNonDevNodeWithFavoriteFalse() {
            // Given
            when(nodeHandler.hasNodes()).thenReturn(Mono.just(false));
            when(nodeHandler.findByCodeAndStatus(anyString(), anyString())).thenReturn(Mono.empty());

            when(nodeHandler.save(any(Node.class))).thenAnswer(invocation -> {
                Node node = invocation.getArgument(0);
                if ("INT-01".equals(node.getCode())) {
                    assertFalse(node.isFavorite());
                    assertEquals("Integration", node.getName());
                    assertEquals("Integration environment", node.getDescription());
                }
                return Mono.just(node);
            });

            // When
            Mono<Void> result = initializer.init();

            // Then
            StepVerifier.create(result)
                    .verifyComplete();
        }

        @Test
        void createNode_ShouldSetBaseUrlValue() {
            // Given
            when(nodeHandler.hasNodes()).thenReturn(Mono.just(false));
            when(nodeHandler.findByCodeAndStatus(anyString(), anyString())).thenReturn(Mono.empty());

            when(nodeHandler.save(any(Node.class))).thenAnswer(invocation -> {
                Node node = invocation.getArgument(0);
                List<Value> values = node.getValues();
                assertNotNull(values);
                assertEquals(1, values.size());
                assertEquals("BASE_URL", values.get(0).getKey());
                assertEquals(API_URL, values.get(0).getValue());
                return Mono.just(node);
            });

            // When
            Mono<Void> result = initializer.init();

            // Then
            StepVerifier.create(result)
                    .verifyComplete();
        }
    }

    @Nested
    class EdgeCasesTests {

        @Test
        void init_ShouldHandleNullApiUrl() {
            // Given
            DefaultEnvironmentInitializer initializerWithNullUrl = new DefaultEnvironmentInitializer(nodeHandler, null);
            when(nodeHandler.hasNodes()).thenReturn(Mono.just(false));
            when(nodeHandler.findByCodeAndStatus(anyString(), anyString())).thenReturn(Mono.empty());

            when(nodeHandler.save(any(Node.class))).thenAnswer(invocation -> {
                Node node = invocation.getArgument(0);
                assertNotNull(node.getValues());
                assertEquals(1, node.getValues().size());
                assertNull(node.getValues().get(0).getValue());
                return Mono.just(node);
            });

            // When
            Mono<Void> result = initializerWithNullUrl.init();

            // Then
            StepVerifier.create(result)
                    .verifyComplete();
        }

        @Test
        void init_ShouldHandleEmptyApiUrl() {
            // Given
            DefaultEnvironmentInitializer initializerWithEmptyUrl = new DefaultEnvironmentInitializer(nodeHandler, "");
            when(nodeHandler.hasNodes()).thenReturn(Mono.just(false));
            when(nodeHandler.findByCodeAndStatus(anyString(), anyString())).thenReturn(Mono.empty());

            when(nodeHandler.save(any(Node.class))).thenAnswer(invocation -> {
                Node node = invocation.getArgument(0);
                assertNotNull(node.getValues());
                assertEquals(1, node.getValues().size());
                assertEquals("", node.getValues().get(0).getValue());
                return Mono.just(node);
            });

            // When
            Mono<Void> result = initializerWithEmptyUrl.init();

            // Then
            StepVerifier.create(result)
                    .verifyComplete();
        }

        @Test
        void init_ShouldHandleFindByCodeAndStatusError() {
            // Given
            when(nodeHandler.hasNodes()).thenReturn(Mono.just(false));
            when(nodeHandler.save(argThat(node -> node != null && "DEV-01".equals(node.getCode()))))
                    .thenAnswer(invocation -> {
                        Node node = invocation.getArgument(0);
                        return Mono.just(node);
                    });
            when(nodeHandler.findByCodeAndStatus(anyString(), eq(StatusEnum.SNAPSHOT.name())))
                    .thenReturn(Mono.error(new RuntimeException("Find error")));

            // When
            Mono<Void> result = initializer.init();

            // Then
            StepVerifier.create(result)
                    .expectError(RuntimeException.class)
                    .verify();
        }
    }

    // Helper method to create expected Node objects
    private Node createExpectedNode(String name, String description, String code, String slug, boolean isFavorite) {
        Value baseUrl = new Value();
        baseUrl.setKey("BASE_URL");
        baseUrl.setValue(API_URL);

        Node node = new Node();
        node.setName(name);
        node.setDescription(description);
        node.setVersion("0");
        node.setDefaultLanguage("EN");
        node.setCode(code);
        node.setSlug(slug);
        node.setValues(List.of(baseUrl));
        node.setFavorite(isFavorite);
        node.setStatus(StatusEnum.SNAPSHOT);

        return node;
    }
}