package com.itexpert.content.core.endpoints;

import com.itexpert.content.core.handlers.NodeHandler;
import com.itexpert.content.core.handlers.UserHandler;
import com.itexpert.content.core.models.TreeNode;
import com.itexpert.content.core.models.auth.RoleEnum;
import com.itexpert.content.core.utils.auth.SecurityUtils;
import com.itexpert.content.lib.enums.ContentTypeEnum;
import com.itexpert.content.lib.enums.NotificationEnum;
import com.itexpert.content.lib.enums.StatusEnum;
import com.itexpert.content.lib.models.ContentNode;
import com.itexpert.content.lib.models.Node;
import com.itexpert.content.lib.models.UserPost;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NodeEndPointTest {

    @Mock
    private NodeHandler nodeHandler;

    @Mock
    private UserHandler userHandler;

    @InjectMocks
    private NodeEndPoint nodeEndPoint;

    private Node sampleNode;
    private Node sampleNode2;
    private TreeNode sampleTreeNode;
    private UserPost sampleUserPost;
    private ContentNode sampleContentNode;

    @BeforeEach
    void setUp() {
        sampleNode = new Node();
        sampleNode.setCode("test-node");
        sampleNode.setName("Test Node");
        sampleNode.setParentCode("parent-node");
        sampleNode.setFavorite(true);
        sampleNode.setModifiedBy("test-user");
        sampleNode.setStatus(StatusEnum.PUBLISHED);
        sampleNode.setContents(new ArrayList<>());

        sampleNode2 = new Node();
        sampleNode2.setCode("test-node-2");
        sampleNode2.setName("Test Node 2");
        sampleNode2.setParentCode("parent-node");
        sampleNode2.setFavorite(false);
        sampleNode2.setModifiedBy("test-user");
        sampleNode2.setStatus(StatusEnum.SNAPSHOT);
        sampleNode2.setContents(new ArrayList<>());

        sampleContentNode = new ContentNode();
        sampleContentNode.setType(ContentTypeEnum.JSON);
        sampleContentNode.setContent("Test content?status=SNAPSHOT&other=value");

        sampleTreeNode = new TreeNode();
        sampleTreeNode.setCode("root");
        sampleTreeNode.setName("Root");
        sampleTreeNode.setChildren(new ArrayList<>());

        sampleUserPost = new UserPost();
        sampleUserPost.setEmail("test@example.com");
        sampleUserPost.setProjects(List.of("project1", "project2"));
    }

    @Nested
    class FindAllTests {

        @Test
        void findAll_ShouldReturnNodes_WhenUserHasRole() {
            try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
                securityUtils.when(() -> SecurityUtils.hasAnyRole(any(), any(), any()))
                        .thenReturn(Mono.just(true));
                when(nodeHandler.findAll()).thenReturn(Flux.just(sampleNode, sampleNode2));
                when(nodeHandler.setPublicationStatus(any(Node.class)))
                        .thenReturn(Mono.just(sampleNode))
                        .thenReturn(Mono.just(sampleNode2));

                Flux<Node> result = nodeEndPoint.findAll();

                StepVerifier.create(result)
                        .expectNextCount(2)
                        .verifyComplete();

                verify(nodeHandler, times(2)).setPublicationStatus(any(Node.class));
            }
        }

        @Test
        void findAll_ShouldReturnError_WhenUserHasNoRole() {
            try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
                securityUtils.when(() -> SecurityUtils.hasAnyRole(any(), any(), any()))
                        .thenReturn(Mono.just(false));

                Flux<Node> result = nodeEndPoint.findAll();

                StepVerifier.create(result)
                        .expectErrorMatches(throwable -> throwable.getMessage().equals("Accès refusé"))
                        .verify();
            }
        }
    }

    @Nested
    class FindParentOriginTests {

        @Test
        void findParentOrigin_ShouldReturnNodes_WhenUserHasRole() {
            try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
                securityUtils.when(() -> SecurityUtils.hasAnyRole(any(), any(), any()))
                        .thenReturn(Mono.just(true));
                when(nodeHandler.findParentOrigin()).thenReturn(Flux.just(sampleNode));
                when(nodeHandler.setPublicationStatus(any(Node.class))).thenReturn(Mono.just(sampleNode));

                Flux<Node> result = nodeEndPoint.findParentOrigin();

                StepVerifier.create(result)
                        .expectNextCount(1)
                        .verifyComplete();
            }
        }
    }

    @Nested
    class FindAllByStatusTests {

        @Test
        void findAllByStatus_AsAdmin_ShouldReturnAllNodes() {
            try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
                securityUtils.when(() -> SecurityUtils.hasAnyRole(any(), any(), any()))
                        .thenReturn(Mono.just(true));
                securityUtils.when(() -> SecurityUtils.hasRole(RoleEnum.ADMIN.name()))
                        .thenReturn(Mono.just(true));
                when(nodeHandler.findAllByStatus(StatusEnum.PUBLISHED.name()))
                        .thenReturn(Flux.just(sampleNode));
                when(nodeHandler.setPublicationStatus(any(Node.class))).thenReturn(Mono.just(sampleNode));

                Flux<Node> result = nodeEndPoint.findAllByStatus(StatusEnum.PUBLISHED.name());

                StepVerifier.create(result)
                        .expectNextCount(1)
                        .verifyComplete();
            }
        }

        @Test
        void findAllByStatus_AsEditor_ShouldReturnUserNodes() {
            try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
                securityUtils.when(() -> SecurityUtils.hasAnyRole(any(), any(), any()))
                        .thenReturn(Mono.just(true));
                securityUtils.when(() -> SecurityUtils.hasRole(RoleEnum.ADMIN.name()))
                        .thenReturn(Mono.just(false));
                securityUtils.when(SecurityUtils::getUsername)
                        .thenReturn(Mono.just("test-user"));
                when(nodeHandler.findAllByStatusAndUser(eq(StatusEnum.PUBLISHED.name()), eq("test-user")))
                        .thenReturn(Flux.just(sampleNode));
                when(nodeHandler.setPublicationStatus(any(Node.class))).thenReturn(Mono.just(sampleNode));

                Flux<Node> result = nodeEndPoint.findAllByStatus(StatusEnum.PUBLISHED.name());

                StepVerifier.create(result)
                        .expectNextCount(1)
                        .verifyComplete();
            }
        }
    }

    @Nested
    class PublishedTests {

        @Test
        void published_ShouldReturnPublishedNodes() {
            try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
                securityUtils.when(() -> SecurityUtils.hasAnyRole(any(), any(), any()))
                        .thenReturn(Mono.just(true));
                when(nodeHandler.findAllByStatus(StatusEnum.PUBLISHED.name()))
                        .thenReturn(Flux.just(sampleNode));
                when(nodeHandler.setPublicationStatus(any(Node.class))).thenReturn(Mono.just(sampleNode));

                Flux<Node> result = nodeEndPoint.published();

                StepVerifier.create(result)
                        .expectNextCount(1)
                        .verifyComplete();
            }
        }
    }

    @Nested
    class GetDeletedTests {

        @Test
        void getDeleted_AsAdmin_ShouldReturnDeletedNodes() {
            try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
                securityUtils.when(() -> SecurityUtils.hasAnyRole(RoleEnum.ADMIN.name(), RoleEnum.EDITOR.name()))
                        .thenReturn(Mono.just(true));
                securityUtils.when(() -> SecurityUtils.hasRole(RoleEnum.ADMIN.name()))
                        .thenReturn(Mono.just(true));
                when(nodeHandler.findAllByStatus(StatusEnum.DELETED.name()))
                        .thenReturn(Flux.just(sampleNode));
                when(nodeHandler.setPublicationStatus(any(Node.class))).thenReturn(Mono.just(sampleNode));

                Flux<Node> result = nodeEndPoint.getDeleted("parent-node");

                StepVerifier.create(result)
                        .expectNextCount(1)
                        .verifyComplete();
            }
        }

        @Test
        void getDeleted_AsEditor_ShouldReturnUserDeletedNodes() {
            try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
                // Créer un node DELETED sans parentCode spécifiquement pour ce test
                Node deletedNodeWithoutParent = new Node();
                deletedNodeWithoutParent.setCode("deleted-node");
                deletedNodeWithoutParent.setName("Deleted Node");
                deletedNodeWithoutParent.setParentCode(null); // Important : pas de parent
                deletedNodeWithoutParent.setFavorite(false);
                deletedNodeWithoutParent.setModifiedBy("test-user");
                deletedNodeWithoutParent.setStatus(StatusEnum.DELETED);
                deletedNodeWithoutParent.setContents(new ArrayList<>());

                securityUtils.when(() -> SecurityUtils.hasAnyRole(RoleEnum.ADMIN.name(), RoleEnum.EDITOR.name()))
                        .thenReturn(Mono.just(true));
                securityUtils.when(() -> SecurityUtils.hasRole(RoleEnum.ADMIN.name()))
                        .thenReturn(Mono.just(false));
                securityUtils.when(SecurityUtils::getUsername)
                        .thenReturn(Mono.just("test-user"));
                when(nodeHandler.findDeleted("test-user"))
                        .thenReturn(Flux.just(deletedNodeWithoutParent));
                when(nodeHandler.setPublicationStatus(any(Node.class)))
                        .thenReturn(Mono.just(deletedNodeWithoutParent));

                Flux<Node> result = nodeEndPoint.getDeleted(null);

                StepVerifier.create(result)
                        .expectNext(deletedNodeWithoutParent)
                        .verifyComplete();

                verify(nodeHandler, times(1)).findDeleted("test-user");
            }
        }
    }

    @Nested
    class FindByCodeAndStatusTests {

        @Test
        void findByCodeAndStatus_ShouldReturnNode_WhenFound() {
            try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
                securityUtils.when(() -> SecurityUtils.hasAnyRole(any(), any(), any()))
                        .thenReturn(Mono.just(true));
                when(nodeHandler.findByCodeAndStatus("test-node", StatusEnum.PUBLISHED.name()))
                        .thenReturn(Mono.just(sampleNode));
                when(nodeHandler.setPublicationStatus(any(Node.class))).thenReturn(Mono.just(sampleNode));

                Mono<ResponseEntity<Node>> result = nodeEndPoint.findByCodeAndStatus("test-node",
                        StatusEnum.PUBLISHED.name());

                StepVerifier.create(result)
                        .assertNext(response -> {
                            assertEquals(HttpStatus.OK, response.getStatusCode());
                            assertNotNull(response.getBody());
                        })
                        .verifyComplete();
            }
        }

        @Test
        void findByCodeAndStatus_ShouldReturnNotFound_WhenNodeMissing() {
            try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
                securityUtils.when(() -> SecurityUtils.hasAnyRole(any(), any(), any()))
                        .thenReturn(Mono.just(true));
                when(nodeHandler.findByCodeAndStatus("test-node", StatusEnum.PUBLISHED.name()))
                        .thenReturn(Mono.empty());

                Mono<ResponseEntity<Node>> result = nodeEndPoint.findByCodeAndStatus("test-node",
                        StatusEnum.PUBLISHED.name());

                StepVerifier.create(result)
                        .assertNext(response -> assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode()))
                        .verifyComplete();
            }
        }

        @Test
        void findByCodeAndStatus_ShouldReturnForbidden_WhenNoRole() {
            try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
                securityUtils.when(() -> SecurityUtils.hasAnyRole(any(), any(), any()))
                        .thenReturn(Mono.just(false));

                Mono<ResponseEntity<Node>> result = nodeEndPoint.findByCodeAndStatus("test-node",
                        StatusEnum.PUBLISHED.name());

                StepVerifier.create(result)
                        .assertNext(response -> assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode()))
                        .verifyComplete();
            }
        }
    }

    @Nested
    class DeleteTests {

        @Test
        void delete_AsAdmin_ShouldDeleteNode() {
            try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
                securityUtils.when(() -> SecurityUtils.hasAnyRole(RoleEnum.ADMIN.name(), RoleEnum.EDITOR.name()))
                        .thenReturn(Mono.just(true));
                securityUtils.when(() -> SecurityUtils.hasRole(RoleEnum.EDITOR.name()))
                        .thenReturn(Mono.just(false));
                securityUtils.when(SecurityUtils::getUsername)
                        .thenReturn(Mono.just("admin-user"));
                when(nodeHandler.delete("test-node", "admin-user"))
                        .thenReturn(Mono.just(true));

                Mono<ResponseEntity<Boolean>> result = nodeEndPoint.delete("test-node");

                StepVerifier.create(result)
                        .assertNext(response -> {
                            assertEquals(HttpStatus.OK, response.getStatusCode());
                            assertTrue(response.getBody());
                        })
                        .verifyComplete();
            }
        }

        @Test
        void delete_AsEditorWithAccess_ShouldDeleteNode() {
            try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
                securityUtils.when(() -> SecurityUtils.hasAnyRole(RoleEnum.ADMIN.name(), RoleEnum.EDITOR.name()))
                        .thenReturn(Mono.just(true));
                securityUtils.when(() -> SecurityUtils.hasRole(RoleEnum.EDITOR.name()))
                        .thenReturn(Mono.just(true));
                securityUtils.when(() -> SecurityUtils.hasProjectAccess("test-node"))
                        .thenReturn(Mono.just(true));
                securityUtils.when(SecurityUtils::getUsername)
                        .thenReturn(Mono.just("editor-user"));
                when(nodeHandler.delete("test-node", "editor-user"))
                        .thenReturn(Mono.just(true));

                Mono<ResponseEntity<Boolean>> result = nodeEndPoint.delete("test-node");

                StepVerifier.create(result)
                        .assertNext(response -> {
                            assertEquals(HttpStatus.OK, response.getStatusCode());
                            assertTrue(response.getBody());
                        })
                        .verifyComplete();
            }
        }

        @Test
        void delete_AsEditorWithoutAccess_ShouldReturnForbidden() {
            try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
                securityUtils.when(() -> SecurityUtils.hasAnyRole(RoleEnum.ADMIN.name(), RoleEnum.EDITOR.name()))
                        .thenReturn(Mono.just(true));
                securityUtils.when(() -> SecurityUtils.hasRole(RoleEnum.EDITOR.name()))
                        .thenReturn(Mono.just(true));
                securityUtils.when(() -> SecurityUtils.hasProjectAccess("test-node"))
                        .thenReturn(Mono.just(false));

                Mono<ResponseEntity<Boolean>> result = nodeEndPoint.delete("test-node");

                StepVerifier.create(result)
                        .assertNext(response -> assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode()))
                        .verifyComplete();
            }
        }
    }

    @Nested
    class DeleteDefinitivelyTests {

        @Test
        void deleteDefinitively_AsAdmin_ShouldDeletePermanently() {
            try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
                securityUtils.when(() -> SecurityUtils.hasAnyRole(RoleEnum.ADMIN.name(), RoleEnum.EDITOR.name()))
                        .thenReturn(Mono.just(true));
                securityUtils.when(() -> SecurityUtils.hasRole(RoleEnum.EDITOR.name()))
                        .thenReturn(Mono.just(false));
                when(nodeHandler.deleteDefinitively("test-node"))
                        .thenReturn(Mono.just(true));

                Mono<ResponseEntity<Boolean>> result = nodeEndPoint.deleteDefinitively("test-node");

                StepVerifier.create(result)
                        .assertNext(response -> {
                            assertEquals(HttpStatus.OK, response.getStatusCode());
                            assertTrue(response.getBody());
                        })
                        .verifyComplete();
            }
        }
    }

    @Nested
    class ActivateTests {

        @Test
        void activate_ShouldActivateNode() {
            try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
                securityUtils.when(() -> SecurityUtils.hasAnyRole(RoleEnum.ADMIN.name(), RoleEnum.EDITOR.name()))
                        .thenReturn(Mono.just(true));
                securityUtils.when(() -> SecurityUtils.hasRole(RoleEnum.EDITOR.name()))
                        .thenReturn(Mono.just(false));
                securityUtils.when(SecurityUtils::getUsername)
                        .thenReturn(Mono.just("test-user"));
                when(nodeHandler.activate("test-node", "test-user"))
                        .thenReturn(Mono.just(true));

                Mono<ResponseEntity<Boolean>> result = nodeEndPoint.activate("test-node");

                StepVerifier.create(result)
                        .assertNext(response -> {
                            assertEquals(HttpStatus.OK, response.getStatusCode());
                            assertTrue(response.getBody());
                        })
                        .verifyComplete();
            }
        }
    }

    @Nested
    class PublishTests {

        @Test
        void publish_ShouldPublishNode() {
            try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
                securityUtils.when(() -> SecurityUtils.hasAnyRole(RoleEnum.ADMIN.name(), RoleEnum.EDITOR.name()))
                        .thenReturn(Mono.just(true));
                securityUtils.when(() -> SecurityUtils.hasRole(RoleEnum.EDITOR.name()))
                        .thenReturn(Mono.just(false));
                securityUtils.when(SecurityUtils::getUsername)
                        .thenReturn(Mono.just("test-user"));
                when(nodeHandler.publish("test-node", "test-user"))
                        .thenReturn(Mono.just(sampleNode));
                when(nodeHandler.setPublicationStatus(any(Node.class))).thenReturn(Mono.just(sampleNode));

                Mono<ResponseEntity<Node>> result = nodeEndPoint.publish("test-node");

                StepVerifier.create(result)
                        .assertNext(response -> {
                            assertEquals(HttpStatus.OK, response.getStatusCode());
                            assertNotNull(response.getBody());
                        })
                        .verifyComplete();
            }
        }
    }

    @Nested
    class SaveTests {

        @Test
        void save_AsAdmin_ShouldSaveNode() {
            try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
                securityUtils.when(() -> SecurityUtils.hasAnyRole(RoleEnum.ADMIN.name(), RoleEnum.EDITOR.name()))
                        .thenReturn(Mono.just(true));
                securityUtils.when(() -> SecurityUtils.hasRole(RoleEnum.EDITOR.name()))
                        .thenReturn(Mono.just(false));
                securityUtils.when(SecurityUtils::getUsername)
                        .thenReturn(Mono.just("admin-user"));
                when(nodeHandler.save(any(Node.class)))
                        .thenReturn(Mono.just(sampleNode));
                when(nodeHandler.setPublicationStatus(any(Node.class))).thenReturn(Mono.just(sampleNode));

                Mono<ResponseEntity<Node>> result = nodeEndPoint.save(sampleNode);

                StepVerifier.create(result)
                        .assertNext(response -> {
                            assertEquals(HttpStatus.OK, response.getStatusCode());
                            assertNotNull(response.getBody());
                        })
                        .verifyComplete();
            }
        }
    }

    @Nested
    class ExportAllTests {

        @Test
        void exportAll_ShouldReturnExportData() {
            byte[] exportData = "{\"test\":\"data\"}".getBytes(StandardCharsets.UTF_8);

            try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
                securityUtils.when(() -> SecurityUtils.hasAnyRole(RoleEnum.ADMIN.name(), RoleEnum.EDITOR.name()))
                        .thenReturn(Mono.just(true));
                securityUtils.when(() -> SecurityUtils.hasRole(RoleEnum.EDITOR.name()))
                        .thenReturn(Mono.just(false));
                when(nodeHandler.exportAll("test-node", null))
                        .thenReturn(Mono.just(exportData));

                Mono<ResponseEntity<byte[]>> result = nodeEndPoint.exportAll("test-node", null);

                StepVerifier.create(result)
                        .assertNext(response -> {
                            assertEquals(HttpStatus.OK, response.getStatusCode());
                            assertEquals(MediaType.APPLICATION_OCTET_STREAM, response.getHeaders().getContentType());
                            assertArrayEquals(exportData, response.getBody());
                        })
                        .verifyComplete();
            }
        }
    }

    @Nested
    class ImportTests {

        @Test
        void importNode_ShouldImportNode() {
            when(nodeHandler.importNode(any(Node.class)))
                    .thenReturn(Mono.just(sampleNode));
            when(nodeHandler.setPublicationStatus(any(Node.class))).thenReturn(Mono.just(sampleNode));

            Mono<ResponseEntity<Node>> result = nodeEndPoint.importNode(sampleNode);

            StepVerifier.create(result)
                    .assertNext(response -> {
                        assertEquals(HttpStatus.OK, response.getStatusCode());
                        assertNotNull(response.getBody());
                    })
                    .verifyComplete();
        }

        @Test
        void importNodes_ShouldImportMultipleNodes() {
            List<Node> nodes = List.of(sampleNode, sampleNode2);

            when(nodeHandler.importNodes(eq(nodes), isNull(), eq(true)))
                    .thenReturn(Flux.just(sampleNode, sampleNode2));
            when(nodeHandler.setPublicationStatus(any(Node.class)))
                    .thenReturn(Mono.just(sampleNode))
                    .thenReturn(Mono.just(sampleNode2));

            Flux<Node> result = nodeEndPoint.importNodes(nodes, null, true);

            StepVerifier.create(result)
                    .expectNextCount(2)
                    .verifyComplete();
        }
    }

    @Nested
    class HaveChildsTests {

        @Test
        void haveChilds_ShouldReturnTrue_WhenNodeHasChildren() {
            try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
                securityUtils.when(() -> SecurityUtils.hasAnyRole(any(), any(), any()))
                        .thenReturn(Mono.just(true));
                when(nodeHandler.haveChilds("test-node"))
                        .thenReturn(Mono.just(true));

                Mono<ResponseEntity<Boolean>> result = nodeEndPoint.haveChilds("test-node");

                StepVerifier.create(result)
                        .assertNext(response -> {
                            assertEquals(HttpStatus.OK, response.getStatusCode());
                            assertTrue(response.getBody());
                        })
                        .verifyComplete();
            }
        }
    }

    @Nested
    class HaveContentsTests {

        @Test
        void haveContents_ShouldReturnTrue_WhenNodeHasContents() {
            try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
                securityUtils.when(() -> SecurityUtils.hasAnyRole(any(), any(), any()))
                        .thenReturn(Mono.just(true));
                when(nodeHandler.haveContents("test-node"))
                        .thenReturn(Mono.just(true));

                Mono<ResponseEntity<Boolean>> result = nodeEndPoint.haveContents("test-node");

                StepVerifier.create(result)
                        .assertNext(response -> {
                            assertEquals(HttpStatus.OK, response.getStatusCode());
                            assertTrue(response.getBody());
                        })
                        .verifyComplete();
            }
        }
    }

    @Nested
    class DeployTests {

        @Test
        void deploy_ShouldDeployNode() {
            byte[] exportData = "[{\"code\":\"node1\",\"parentCode\":\"env-code\"}]".getBytes(StandardCharsets.UTF_8);
            Node importedNode = new Node();
            importedNode.setCode("node1");
            importedNode.setParentCode("env-code");

            Node snapshotNode = new Node();
            snapshotNode.setCode("node1");

            try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
                securityUtils.when(() -> SecurityUtils.hasAnyRole(RoleEnum.ADMIN.name(), RoleEnum.EDITOR.name()))
                        .thenReturn(Mono.just(true));
                securityUtils.when(SecurityUtils::getUsername)
                        .thenReturn(Mono.just("test-user"));
                when(nodeHandler.exportAll("test-node", "env-code"))
                        .thenReturn(Mono.just(exportData));
                when(nodeHandler.importNodes(anyList(), eq("env-code"), eq(false)))
                        .thenReturn(Flux.just(importedNode));
                when(nodeHandler.setPublicationStatus(any(Node.class)))
                        .thenReturn(Mono.just(importedNode));
                when(nodeHandler.notify(any(Node.class), eq(NotificationEnum.IMPORT)))
                        .thenReturn(Mono.just(importedNode));
                when(nodeHandler.findByCodeAndStatus("node1", StatusEnum.SNAPSHOT.name()))
                        .thenReturn(Mono.just(snapshotNode));
                when(nodeHandler.publish("node1", "test-user"))
                        .thenReturn(Mono.just(snapshotNode));

                Flux<Node> result = nodeEndPoint.deploy("test-node", "env-code");

                StepVerifier.create(result)
                        .expectNextCount(1)
                        .verifyComplete();
            }
        }
    }

    @Nested
    class DeployVersionTests {

        @Test
        void deployVersion_ShouldDeploySpecificVersion() {
            try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
                securityUtils.when(() -> SecurityUtils.hasAnyRole(RoleEnum.ADMIN.name(), RoleEnum.EDITOR.name()))
                        .thenReturn(Mono.just(true));
                securityUtils.when(() -> SecurityUtils.hasRole(RoleEnum.EDITOR.name()))
                        .thenReturn(Mono.just(false));
                securityUtils.when(SecurityUtils::getUsername)
                        .thenReturn(Mono.just("test-user"));
                when(nodeHandler.publishVersion("test-node", "1.0", "test-user"))
                        .thenReturn(Mono.just(true));

                Mono<ResponseEntity<Boolean>> result = nodeEndPoint.deployVersion("test-node", "1.0", null);

                StepVerifier.create(result)
                        .assertNext(response -> {
                            assertEquals(HttpStatus.OK, response.getStatusCode());
                            assertTrue(response.getBody());
                        })
                        .verifyComplete();
            }
        }
    }

    @Nested
    class SlugExistsTests {

        @Test
        void slugExists_ShouldReturnTrue_WhenSlugExists() {
            try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
                securityUtils.when(() -> SecurityUtils.hasAnyRole(RoleEnum.ADMIN.name(), RoleEnum.EDITOR.name()))
                        .thenReturn(Mono.just(true));
                when(nodeHandler.slugAlreadyExists("test-node", "test-slug"))
                        .thenReturn(Mono.just(true));

                Mono<ResponseEntity<Boolean>> result = nodeEndPoint.slugExists("test-node", "test-slug");

                StepVerifier.create(result)
                        .assertNext(response -> {
                            assertEquals(HttpStatus.OK, response.getStatusCode());
                            assertTrue(response.getBody());
                        })
                        .verifyComplete();
            }
        }
    }

    @Nested
    class GenerateTreeViewTests {

        @Test
        void generateTreeView_AsAdmin_ShouldReturnFullTree() {
            try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
                securityUtils.when(() -> SecurityUtils.hasAnyRole(any(), any(), any()))
                        .thenReturn(Mono.just(true));
                securityUtils.when(() -> SecurityUtils.hasRole(RoleEnum.ADMIN.name()))
                        .thenReturn(Mono.just(true));
                when(nodeHandler.generateTreeView("test-node", List.of()))
                        .thenReturn(Mono.just(sampleTreeNode));

                Mono<ResponseEntity<TreeNode>> result = nodeEndPoint.generateTreeView("test-node");

                StepVerifier.create(result)
                        .assertNext(response -> {
                            assertEquals(HttpStatus.OK, response.getStatusCode());
                            assertNotNull(response.getBody());
                        })
                        .verifyComplete();
            }
        }

        @Test
        void generateTreeView_AsEditor_ShouldReturnFilteredTree() {
            try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
                securityUtils.when(() -> SecurityUtils.hasAnyRole(any(), any(), any()))
                        .thenReturn(Mono.just(true));
                securityUtils.when(() -> SecurityUtils.hasRole(RoleEnum.ADMIN.name()))
                        .thenReturn(Mono.just(false));
                securityUtils.when(SecurityUtils::getUsername)
                        .thenReturn(Mono.just("test@example.com"));
                when(userHandler.findByEmail("test@example.com"))
                        .thenReturn(Mono.just(sampleUserPost));
                when(nodeHandler.generateTreeView(eq("test-node"), anyList()))
                        .thenReturn(Mono.just(sampleTreeNode));

                Mono<ResponseEntity<TreeNode>> result = nodeEndPoint.generateTreeView("test-node");

                StepVerifier.create(result)
                        .assertNext(response -> {
                            assertEquals(HttpStatus.OK, response.getStatusCode());
                            assertNotNull(response.getBody());
                        })
                        .verifyComplete();
            }
        }
    }

    @Nested
    class PropagateMaxHistoryToKeepTests {

        @Test
        void propagateMaxHistoryToKeep_ShouldPropagate() {
            when(nodeHandler.propagateMaxHistoryToKeep("test-node"))
                    .thenReturn(Mono.just(true));

            Mono<ResponseEntity<Boolean>> result = nodeEndPoint.propagateMaxHistoryToKeep("test-node");

            StepVerifier.create(result)
                    .assertNext(response -> {
                        assertEquals(HttpStatus.OK, response.getStatusCode());
                        assertTrue(response.getBody());
                    })
                    .verifyComplete();
        }
    }

    @Nested
    class FindByCodeParentTests {

        @Test
        void findByCodeParent_ShouldReturnChildren() {
            try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
                securityUtils.when(() -> SecurityUtils.hasAnyRole(any(), any(), any()))
                        .thenReturn(Mono.just(true));
                when(nodeHandler.findByCodeParent("parent-node"))
                        .thenReturn(Flux.just(sampleNode, sampleNode2));
                when(nodeHandler.setPublicationStatus(any(Node.class)))
                        .thenReturn(Mono.just(sampleNode))
                        .thenReturn(Mono.just(sampleNode2));

                Flux<Node> result = nodeEndPoint.findByCodeParent("parent-node");

                StepVerifier.create(result)
                        .expectNextCount(2)
                        .verifyComplete();
            }
        }
    }

    @Nested
    class FindAllDescendantsTests {

        @Test
        void findAllDescendants_ShouldReturnAllChildren() {
            try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
                securityUtils.when(() -> SecurityUtils.hasAnyRole(any(), any(), any()))
                        .thenReturn(Mono.just(true));
                when(nodeHandler.findAllChildren("parent-node"))
                        .thenReturn(Flux.just(sampleNode, sampleNode2));
                when(nodeHandler.setPublicationStatus(any(Node.class)))
                        .thenReturn(Mono.just(sampleNode))
                        .thenReturn(Mono.just(sampleNode2));

                Flux<Node> result = nodeEndPoint.findAllDescendants("parent-node");

                StepVerifier.create(result)
                        .expectNextCount(2)
                        .verifyComplete();
            }
        }
    }

    @Nested
    class FindParentsNodesByStatusTests {

        @Test
        void findParentsNodesByStatus_AsAdmin_ShouldReturnAllParents() {
            try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
                securityUtils.when(() -> SecurityUtils.hasAnyRole(any(), any(), any()))
                        .thenReturn(Mono.just(true));
                securityUtils.when(() -> SecurityUtils.hasRole(RoleEnum.ADMIN.name()))
                        .thenReturn(Mono.just(true));
                when(nodeHandler.findParentsNodesByStatus(StatusEnum.PUBLISHED.name()))
                        .thenReturn(Flux.just(sampleNode));
                when(nodeHandler.setPublicationStatus(any(Node.class))).thenReturn(Mono.just(sampleNode));

                Flux<Node> result = nodeEndPoint.findParentsNodesByStatus(StatusEnum.PUBLISHED.name());

                StepVerifier.create(result)
                        .expectNextCount(1)
                        .verifyComplete();
            }
        }
    }

    @Nested
    class RevertTests {

        @Test
        void revert_ShouldRevertNodeToVersion() {
            try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
                securityUtils.when(() -> SecurityUtils.hasAnyRole(RoleEnum.ADMIN.name(), RoleEnum.EDITOR.name()))
                        .thenReturn(Mono.just(true));
                securityUtils.when(() -> SecurityUtils.hasRole(RoleEnum.EDITOR.name()))
                        .thenReturn(Mono.just(false));
                securityUtils.when(SecurityUtils::getUsername)
                        .thenReturn(Mono.just("test-user"));
                when(nodeHandler.revert("test-node", "2.0", "test-user"))
                        .thenReturn(Mono.just(sampleNode));
                when(nodeHandler.setPublicationStatus(any(Node.class))).thenReturn(Mono.just(sampleNode));

                Mono<ResponseEntity<Node>> result = nodeEndPoint.revert("test-node", "2.0");

                StepVerifier.create(result)
                        .assertNext(response -> {
                            assertEquals(HttpStatus.OK, response.getStatusCode());
                            assertNotNull(response.getBody());
                        })
                        .verifyComplete();
            }
        }
    }

    @Nested
    class FindChildrenByCodeAndStatusTests {

        @Test
        void findChildrenByCodeAndStatus_ShouldReturnChildren() {
            try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
                securityUtils.when(() -> SecurityUtils.hasAnyRole(any(), any(), any()))
                        .thenReturn(Mono.just(true));
                when(nodeHandler.findChildrenByCodeAndStatus("parent-node", StatusEnum.PUBLISHED.name()))
                        .thenReturn(Flux.just(sampleNode));
                when(nodeHandler.setPublicationStatus(any(Node.class))).thenReturn(Mono.just(sampleNode));

                Flux<Node> result = nodeEndPoint.findChildrenByCodeAndStatus("parent-node",
                        StatusEnum.PUBLISHED.name());

                StepVerifier.create(result)
                        .expectNextCount(1)
                        .verifyComplete();
            }
        }
    }

    @Nested
    class FindByCodeTests {

        @Test
        void findByCode_ShouldReturnNodes() {
            try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
                securityUtils.when(() -> SecurityUtils.hasAnyRole(any(), any(), any()))
                        .thenReturn(Mono.just(true));
                when(nodeHandler.findByCode("test-node"))
                        .thenReturn(Flux.just(sampleNode));
                when(nodeHandler.setPublicationStatus(any(Node.class))).thenReturn(Mono.just(sampleNode));

                Flux<Node> result = nodeEndPoint.findByCode("test-node");

                StepVerifier.create(result)
                        .expectNextCount(1)
                        .verifyComplete();
            }
        }
    }

    @Nested
    class DeleteDefinitivelyVersionTests {

        @Test
        void deleteDefinitivelyVersion_AsAdmin_ShouldDeleteVersion() {
            try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
                securityUtils.when(() -> SecurityUtils.hasAnyRole(RoleEnum.ADMIN.name(), RoleEnum.EDITOR.name()))
                        .thenReturn(Mono.just(true));
                securityUtils.when(() -> SecurityUtils.hasRole(RoleEnum.EDITOR.name()))
                        .thenReturn(Mono.just(false));
                when(nodeHandler.deleteDefinitivelyVersion("test-node", "1.0"))
                        .thenReturn(Mono.just(true));

                Mono<ResponseEntity<Boolean>> result = nodeEndPoint.deleteDefinitivelyVersion("test-node", "1.0");

                StepVerifier.create(result)
                        .assertNext(response -> {
                            assertEquals(HttpStatus.OK, response.getStatusCode());
                            assertTrue(response.getBody());
                        })
                        .verifyComplete();
            }
        }
    }
}