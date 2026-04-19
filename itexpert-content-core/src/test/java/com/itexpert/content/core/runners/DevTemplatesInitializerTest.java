package com.itexpert.content.core.runners;

import com.itexpert.content.core.handlers.NodeHandler;
import com.itexpert.content.core.handlers.UserHandler;
import com.itexpert.content.lib.enums.StatusEnum;
import com.itexpert.content.lib.models.Node;
import com.itexpert.content.lib.models.UserPost;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DevTemplatesInitializerTest {

    private NodeHandler nodeHandler;
    private UserHandler userHandler;
    private DevTemplatesInitializer initializer;

    @BeforeEach
    void setUp() {
        nodeHandler = mock(NodeHandler.class);
        userHandler = mock(UserHandler.class);
        initializer = new DevTemplatesInitializer(nodeHandler, userHandler);
    }

    @Nested
    class InitTests {

        @Test
        void init_ShouldImportTemplates_WhenDevEnvExistsAndAdminExistsAndNoTemplates() {
            // Given
            Node devEnv = createDevEnv("0");
            UserPost admin = createAdminUser();

            when(nodeHandler.findByCodeAndStatus("DEV-01", StatusEnum.SNAPSHOT.name()))
                    .thenReturn(Mono.just(devEnv));
            when(userHandler.findByEmail("admin")).thenReturn(Mono.just(admin));
            when(nodeHandler.findChildrenByCodeAndStatus("DEV-01", StatusEnum.SNAPSHOT.name()))
                    .thenReturn(Flux.empty());
            when(nodeHandler.importNodes(anyList(), eq("DEV-01"), eq(true)))
                    .thenReturn(Flux.just(new Node(), new Node()));
            when(nodeHandler.publish("DEV-01", "Nodify")).thenReturn(Mono.just(devEnv));

            // When
            Mono<Void> result = initializer.init();

            // Then
            StepVerifier.create(result)
                    .verifyComplete();

            verify(nodeHandler, times(7)).importNodes(anyList(), eq("DEV-01"), eq(true));
            verify(nodeHandler, times(1)).publish("DEV-01", "Nodify");
        }

        @Test
        void init_ShouldNotImportTemplates_WhenDevEnvDoesNotExist() {
            // Given
            when(nodeHandler.findByCodeAndStatus("DEV-01", StatusEnum.SNAPSHOT.name()))
                    .thenReturn(Mono.empty());

            // When
            Mono<Void> result = initializer.init();

            // Then
            StepVerifier.create(result)
                    .verifyComplete();

            verify(nodeHandler, never()).importNodes(anyList(), anyString(), anyBoolean());
            verify(nodeHandler, never()).publish(anyString(), anyString());
            verify(userHandler, never()).findByEmail(anyString());
        }

        @Test
        void init_ShouldNotImportTemplates_WhenAdminDoesNotExist() {
            // Given
            Node devEnv = createDevEnv("0");

            when(nodeHandler.findByCodeAndStatus("DEV-01", StatusEnum.SNAPSHOT.name()))
                    .thenReturn(Mono.just(devEnv));
            when(userHandler.findByEmail("admin")).thenReturn(Mono.empty());

            // When
            Mono<Void> result = initializer.init();

            // Then
            StepVerifier.create(result)
                    .verifyComplete();

            verify(nodeHandler, never()).importNodes(anyList(), anyString(), anyBoolean());
            verify(nodeHandler, never()).publish(anyString(), anyString());
        }

        @Test
        void init_ShouldNotImportTemplates_WhenTemplatesAlreadyExist() {
            // Given
            Node devEnv = createDevEnv("0");
            UserPost admin = createAdminUser();
            List<Node> existingNodes = createExistingNodes(7);

            when(nodeHandler.findByCodeAndStatus("DEV-01", StatusEnum.SNAPSHOT.name()))
                    .thenReturn(Mono.just(devEnv));
            when(userHandler.findByEmail("admin")).thenReturn(Mono.just(admin));
            when(nodeHandler.findChildrenByCodeAndStatus("DEV-01", StatusEnum.SNAPSHOT.name()))
                    .thenReturn(Flux.fromIterable(existingNodes));

            // When
            Mono<Void> result = initializer.init();

            // Then
            StepVerifier.create(result)
                    .verifyComplete();

            verify(nodeHandler, never()).importNodes(anyList(), anyString(), anyBoolean());
            verify(nodeHandler, never()).publish(anyString(), anyString());
        }

        @Test
        void init_ShouldNotPublish_WhenNoTemplatesImported() {
            // Given
            Node devEnv = createDevEnv("0");
            UserPost admin = createAdminUser();

            when(nodeHandler.findByCodeAndStatus("DEV-01", StatusEnum.SNAPSHOT.name()))
                    .thenReturn(Mono.just(devEnv));
            when(userHandler.findByEmail("admin")).thenReturn(Mono.just(admin));
            when(nodeHandler.findChildrenByCodeAndStatus("DEV-01", StatusEnum.SNAPSHOT.name()))
                    .thenReturn(Flux.empty());
            when(nodeHandler.importNodes(anyList(), eq("DEV-01"), eq(true)))
                    .thenReturn(Flux.empty());

            // When
            Mono<Void> result = initializer.init();

            // Then
            StepVerifier.create(result)
                    .verifyComplete();

            verify(nodeHandler, times(7)).importNodes(anyList(), eq("DEV-01"), eq(true));
            verify(nodeHandler, never()).publish(anyString(), anyString());
        }

        @Test
        void init_ShouldImportTemplates_WhenVersionIsZero() {
            // Given
            Node devEnv = createDevEnv("0");
            UserPost admin = createAdminUser();

            when(nodeHandler.findByCodeAndStatus("DEV-01", StatusEnum.SNAPSHOT.name()))
                    .thenReturn(Mono.just(devEnv));
            when(userHandler.findByEmail("admin")).thenReturn(Mono.just(admin));
            when(nodeHandler.findChildrenByCodeAndStatus("DEV-01", StatusEnum.SNAPSHOT.name()))
                    .thenReturn(Flux.empty());
            when(nodeHandler.importNodes(anyList(), eq("DEV-01"), eq(true)))
                    .thenReturn(Flux.just(new Node(), new Node()));
            when(nodeHandler.publish("DEV-01", "Nodify")).thenReturn(Mono.just(devEnv));

            // When
            Mono<Void> result = initializer.init();

            // Then
            StepVerifier.create(result)
                    .verifyComplete();

            verify(nodeHandler, times(7)).importNodes(anyList(), eq("DEV-01"), eq(true));
            verify(nodeHandler, times(1)).publish("DEV-01", "Nodify");
        }

        @Test
        void init_ShouldNotImportTemplates_WhenVersionIsNotZero() {
            // Given
            Node devEnv = createDevEnv("1");
            UserPost admin = createAdminUser();

            when(nodeHandler.findByCodeAndStatus("DEV-01", StatusEnum.SNAPSHOT.name()))
                    .thenReturn(Mono.just(devEnv));
            when(userHandler.findByEmail("admin")).thenReturn(Mono.just(admin));
            when(nodeHandler.findChildrenByCodeAndStatus("DEV-01", StatusEnum.SNAPSHOT.name()))
                    .thenReturn(Flux.empty());

            // When
            Mono<Void> result = initializer.init();

            // Then
            StepVerifier.create(result)
                    .verifyComplete();

            verify(nodeHandler, never()).importNodes(anyList(), anyString(), anyBoolean());
            verify(nodeHandler, never()).publish(anyString(), anyString());
        }

        @Test
        void init_ShouldHandleError_WhenFindByCodeAndStatusFails() {
            // Given
            when(nodeHandler.findByCodeAndStatus("DEV-01", StatusEnum.SNAPSHOT.name()))
                    .thenReturn(Mono.error(new RuntimeException("Database error")));

            // When
            Mono<Void> result = initializer.init();

            // Then
            StepVerifier.create(result)
                    .expectError(RuntimeException.class)
                    .verify();
        }

        @Test
        void init_ShouldHandleError_WhenFindByEmailFails() {
            // Given
            Node devEnv = createDevEnv("0");

            when(nodeHandler.findByCodeAndStatus("DEV-01", StatusEnum.SNAPSHOT.name()))
                    .thenReturn(Mono.just(devEnv));
            when(userHandler.findByEmail("admin"))
                    .thenReturn(Mono.error(new RuntimeException("User error")));

            // When
            Mono<Void> result = initializer.init();

            // Then
            StepVerifier.create(result)
                    .expectError(RuntimeException.class)
                    .verify();
        }

        @Test
        void init_ShouldHandleError_WhenFindChildrenFails() {
            // Given
            Node devEnv = createDevEnv("0");
            UserPost admin = createAdminUser();

            when(nodeHandler.findByCodeAndStatus("DEV-01", StatusEnum.SNAPSHOT.name()))
                    .thenReturn(Mono.just(devEnv));
            when(userHandler.findByEmail("admin")).thenReturn(Mono.just(admin));
            when(nodeHandler.findChildrenByCodeAndStatus("DEV-01", StatusEnum.SNAPSHOT.name()))
                    .thenReturn(Flux.error(new RuntimeException("Find children error")));

            // When
            Mono<Void> result = initializer.init();

            // Then
            StepVerifier.create(result)
                    .expectError(RuntimeException.class)
                    .verify();
        }

        @Test
        void init_ShouldHandleError_WhenImportNodesFails() {
            // Given
            Node devEnv = createDevEnv("0");
            UserPost admin = createAdminUser();

            when(nodeHandler.findByCodeAndStatus("DEV-01", StatusEnum.SNAPSHOT.name()))
                    .thenReturn(Mono.just(devEnv));
            when(userHandler.findByEmail("admin")).thenReturn(Mono.just(admin));
            when(nodeHandler.findChildrenByCodeAndStatus("DEV-01", StatusEnum.SNAPSHOT.name()))
                    .thenReturn(Flux.empty());
            when(nodeHandler.importNodes(anyList(), eq("DEV-01"), eq(true)))
                    .thenReturn(Flux.error(new RuntimeException("Import error")));

            // When
            Mono<Void> result = initializer.init();

            // Then
            StepVerifier.create(result)
                    .expectError(RuntimeException.class)
                    .verify();
        }

    }

    @Nested
    class EdgeCasesTests {

        @Test
        void init_ShouldImportAllSevenTemplates() {
            // Given
            Node devEnv = createDevEnv("0");
            UserPost admin = createAdminUser();

            when(nodeHandler.findByCodeAndStatus("DEV-01", StatusEnum.SNAPSHOT.name()))
                    .thenReturn(Mono.just(devEnv));
            when(userHandler.findByEmail("admin")).thenReturn(Mono.just(admin));
            when(nodeHandler.findChildrenByCodeAndStatus("DEV-01", StatusEnum.SNAPSHOT.name()))
                    .thenReturn(Flux.empty());
            when(nodeHandler.importNodes(anyList(), eq("DEV-01"), eq(true)))
                    .thenReturn(Flux.just(new Node()));
            when(nodeHandler.publish("DEV-01", "Nodify")).thenReturn(Mono.just(devEnv));

            // When
            Mono<Void> result = initializer.init();

            // Then
            StepVerifier.create(result)
                    .verifyComplete();

            verify(nodeHandler, times(7)).importNodes(anyList(), eq("DEV-01"), eq(true));
            verify(nodeHandler, times(1)).publish("DEV-01", "Nodify");
        }

        @Test
        void init_ShouldHandleNullDevEnv() {
            // Given
            when(nodeHandler.findByCodeAndStatus("DEV-01", StatusEnum.SNAPSHOT.name()))
                    .thenReturn(Mono.empty());

            // When
            Mono<Void> result = initializer.init();

            // Then
            StepVerifier.create(result)
                    .verifyComplete();
        }

        @Test
        void init_ShouldHandleExistingNodesLessThanOrEqualTo6AndVersionZero() {
            // Given
            Node devEnv = createDevEnv("0");
            UserPost admin = createAdminUser();
            List<Node> existingNodes = createExistingNodes(5);

            when(nodeHandler.findByCodeAndStatus("DEV-01", StatusEnum.SNAPSHOT.name()))
                    .thenReturn(Mono.just(devEnv));
            when(userHandler.findByEmail("admin")).thenReturn(Mono.just(admin));
            when(nodeHandler.findChildrenByCodeAndStatus("DEV-01", StatusEnum.SNAPSHOT.name()))
                    .thenReturn(Flux.fromIterable(existingNodes));
            when(nodeHandler.importNodes(anyList(), eq("DEV-01"), eq(true)))
                    .thenReturn(Flux.just(new Node()));
            when(nodeHandler.publish("DEV-01", "Nodify")).thenReturn(Mono.just(devEnv));

            // When
            Mono<Void> result = initializer.init();

            // Then
            StepVerifier.create(result)
                    .verifyComplete();

            verify(nodeHandler, times(7)).importNodes(anyList(), eq("DEV-01"), eq(true));
            verify(nodeHandler, times(1)).publish("DEV-01", "Nodify");
        }

        @Test
        void init_ShouldHandleExistingNodesEqualTo6AndVersionZero() {
            // Given
            Node devEnv = createDevEnv("0");
            UserPost admin = createAdminUser();
            List<Node> existingNodes = createExistingNodes(6);

            when(nodeHandler.findByCodeAndStatus("DEV-01", StatusEnum.SNAPSHOT.name()))
                    .thenReturn(Mono.just(devEnv));
            when(userHandler.findByEmail("admin")).thenReturn(Mono.just(admin));
            when(nodeHandler.findChildrenByCodeAndStatus("DEV-01", StatusEnum.SNAPSHOT.name()))
                    .thenReturn(Flux.fromIterable(existingNodes));
            when(nodeHandler.importNodes(anyList(), eq("DEV-01"), eq(true)))
                    .thenReturn(Flux.just(new Node()));
            when(nodeHandler.publish("DEV-01", "Nodify")).thenReturn(Mono.just(devEnv));

            // When
            Mono<Void> result = initializer.init();

            // Then
            StepVerifier.create(result)
                    .verifyComplete();

            verify(nodeHandler, times(7)).importNodes(anyList(), eq("DEV-01"), eq(true));
            verify(nodeHandler, times(1)).publish("DEV-01", "Nodify");
        }
    }

    // Helper methods
    private Node createDevEnv(String version) {
        Node node = new Node();
        node.setCode("DEV-01");
        node.setVersion(version);
        node.setParentCode(null);
        node.setContents(new ArrayList<>());
        return node;
    }

    private UserPost createAdminUser() {
        UserPost user = new UserPost();
        user.setEmail("admin");
        user.setRoles(List.of("ADMIN"));
        return user;
    }

    private List<Node> createExistingNodes(int count) {
        List<Node> nodes = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Node node = new Node();
            node.setCode("existing-node-" + i);
            nodes.add(node);
        }
        return nodes;
    }
}