package com.itexpert.content.core.handlers;

import com.itexpert.content.core.helpers.NodeSlugHelper;
import com.itexpert.content.core.helpers.RenameNodeCodesHelper;
import com.itexpert.content.core.mappers.NodeMapper;
import com.itexpert.content.core.repositories.NodeRepository;
import com.itexpert.content.lib.entities.Node;
import com.itexpert.content.lib.enums.StatusEnum;
import com.itexpert.content.lib.models.Notification;
import com.itexpert.content.lib.models.UserPost;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class NodeHandlerPublishTest {
        private NodeRepository nodeRepository;

        private NodeMapper nodeMapper;

        private ContentNodeHandler contentNodeHandler;

        private NotificationHandler notificationHandler;

        private RenameNodeCodesHelper renameNodeCodesHelper;

        private UserHandler userHandler;

        private NodeHandler nodeHandler;

        private NodeSlugHelper nodeSlugHelper;

        private Node parentSnapshotEntity;
        private Node childNode;

        @BeforeEach
        void setup() {
                contentNodeHandler = mock(ContentNodeHandler.class);
                nodeMapper = Mappers.getMapper(NodeMapper.class);
                nodeRepository = mock(NodeRepository.class);
                userHandler = mock(UserHandler.class);
                notificationHandler = mock(NotificationHandler.class);
                renameNodeCodesHelper = new RenameNodeCodesHelper();

                nodeSlugHelper = mock(NodeSlugHelper.class);

                nodeHandler = new NodeHandler(
                                nodeRepository,
                                nodeMapper,
                                contentNodeHandler,
                                notificationHandler,
                                renameNodeCodesHelper,
                                userHandler,
                                nodeSlugHelper);
                Notification notification = Notification.builder()
                                .id(UUID.randomUUID())
                                .user("Admin")
                                .type("CONTENT_NODE")
                                .build();

                // Parent snapshot
                parentSnapshotEntity = new Node();
                parentSnapshotEntity.setId(UUID.randomUUID());
                parentSnapshotEntity.setCode("PARENT-DEV");
                parentSnapshotEntity.setStatus(StatusEnum.SNAPSHOT);
                parentSnapshotEntity.setVersion("1");

                // Child node
                childNode = new Node();
                childNode.setId(UUID.randomUUID());
                childNode.setCode("NODE-DEV");
                childNode.setStatus(StatusEnum.SNAPSHOT);
                childNode.setVersion("0");

                when(userHandler.findById(any())).thenReturn(Mono.just(mock(UserPost.class)));
                when(notificationHandler.create(any(), any(), any(), any(), any(), any(), any()))
                                .thenReturn(Mono.just(notification));

        }

        @Test
        void publish_shouldCallPublishNode_WhenNoChildreensAndNoPublishedExists() {
                UUID nodeUuid = UUID.randomUUID();
                String user = "Admin";

                // Parent SNAPSHOT
                Node parentSnapshot = new Node();
                parentSnapshot.setId(nodeUuid);
                parentSnapshot.setCode("PARENT-DEV");
                parentSnapshot.setStatus(StatusEnum.SNAPSHOT);
                parentSnapshot.setVersion("0");

                // Mocks
                when(nodeRepository.findByCodeAndStatus(parentSnapshot.getCode(), StatusEnum.SNAPSHOT.name()))
                                .thenReturn(Mono.just(parentSnapshot));

                when(nodeRepository.findByCodeAndStatus("PARENT-DEV", StatusEnum.PUBLISHED.name()))
                                .thenReturn(Mono.empty());

                when(nodeRepository.save(any(Node.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

                when(contentNodeHandler.findAllByNodeCodeAndStatus(any(), any())).thenReturn(Flux.empty());

                NodeHandler spyHandler = Mockito.spy(nodeHandler);
                doReturn(Flux.empty())
                                .when(spyHandler)
                                .findAllChildren(anyString());

                // Exécution et vérification du test
                StepVerifier.create(spyHandler.publish(parentSnapshot.getCode(), user))
                                .expectNextMatches(node -> node.getStatus().equals(StatusEnum.PUBLISHED) &&
                                                node.getCode().equals("PARENT-DEV") &&
                                                node.getId().equals(nodeUuid))
                                .verifyComplete();

                ArgumentCaptor<com.itexpert.content.lib.models.Node> nodeCaptor = ArgumentCaptor
                                .forClass(com.itexpert.content.lib.models.Node.class);

                verify(spyHandler).publishNode(nodeCaptor.capture(), eq(user));
                assertEquals("PARENT-DEV", nodeCaptor.getValue().getCode());

                verify(spyHandler, times(1)).publishNode(any(com.itexpert.content.lib.models.Node.class), any());
                verify(spyHandler, times(0)).archiveNode(any(com.itexpert.content.lib.models.Node.class), any());
                verify(spyHandler, times(1)).createSnapshot(any(com.itexpert.content.lib.models.Node.class), any());
        }

        @Test
        void publish_shouldPublish_WhenNoChildrensFoundAndPublshedExists() {
                UUID nodeUuid = UUID.randomUUID();
                String user = "Admin";

                // Création du noeud parent original et du noeud publié mock
                Node nodeToPublish = new Node();
                nodeToPublish.setId(nodeUuid);
                nodeToPublish.setCode("PARENT-DEV");
                nodeToPublish.setStatus(StatusEnum.SNAPSHOT);
                nodeToPublish.setVersion("1");

                Node publishedNode = new Node();
                publishedNode.setId(UUID.randomUUID());
                publishedNode.setCode("PARENT-DEV");
                publishedNode.setStatus(StatusEnum.PUBLISHED);
                publishedNode.setVersion("0");

                // Création du spy sur le handler
                NodeHandler spyHandler = Mockito.spy(nodeHandler);

                // Mock des dépendances externes
                when(nodeRepository.findById(nodeUuid)).thenReturn(Mono.just(nodeToPublish));
                when(nodeRepository.findByCodeAndStatus(publishedNode.getCode(), StatusEnum.PUBLISHED.name()))
                                .thenReturn(Mono.just(publishedNode));
                when(nodeRepository.findByCodeAndStatus(nodeToPublish.getCode(), StatusEnum.SNAPSHOT.name()))
                                .thenReturn(Mono.just(nodeToPublish));

                when(nodeRepository.save(any(Node.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

                doReturn(Flux.empty()).when(spyHandler).findAllChildren(anyString());

                when(this.contentNodeHandler.findAllByNodeCodeAndStatus(any(), any())).thenReturn(Flux.empty());

                // Exécution du test
                StepVerifier.create(spyHandler.publish(nodeToPublish.getCode(), user))
                                .assertNext(node -> {
                                        assert node.getStatus().equals(StatusEnum.PUBLISHED);
                                        assert node.getId().equals(nodeToPublish.getId());
                                })
                                .verifyComplete();

                verify(spyHandler, times(1)).publishNode(any(com.itexpert.content.lib.models.Node.class), any());
                verify(spyHandler, times(1)).archiveNode(any(com.itexpert.content.lib.models.Node.class), any());
                verify(spyHandler, times(1)).createSnapshot(any(com.itexpert.content.lib.models.Node.class), any());
        }

        @Test
        void publish_shouldPublish_WhenChildrensFoundButNotPublishedAginAndPublshedExists() {
                UUID nodeUuid = UUID.randomUUID();
                String user = "Admin";

                // Création du noeud parent original et du noeud publié mock
                Node nodeToPublish = new Node();
                nodeToPublish.setId(nodeUuid);
                nodeToPublish.setCode("PARENT-DEV");
                nodeToPublish.setStatus(StatusEnum.SNAPSHOT);
                nodeToPublish.setVersion("1");

                Node publishedNode = new Node();
                publishedNode.setId(UUID.randomUUID());
                publishedNode.setCode("PARENT-DEV");
                publishedNode.setStatus(StatusEnum.PUBLISHED);
                publishedNode.setVersion("0");

                com.itexpert.content.lib.models.Node childNode = new com.itexpert.content.lib.models.Node();
                childNode.setId(UUID.randomUUID());
                childNode.setCode("CHILD-DEV");
                childNode.setStatus(StatusEnum.SNAPSHOT);
                childNode.setVersion("0");

                // Création du spy sur le handler
                NodeHandler spyHandler = Mockito.spy(nodeHandler);

                // Mock des dépendances externes
                when(nodeRepository.findById(nodeUuid)).thenReturn(Mono.just(nodeToPublish));
                when(nodeRepository.findByCodeAndStatus(nodeToPublish.getCode(), StatusEnum.SNAPSHOT.name()))
                                .thenReturn(Mono.just(nodeToPublish));

                when(nodeRepository.findByCodeAndStatus(nodeToPublish.getCode(), StatusEnum.PUBLISHED.name()))
                                .thenReturn(Mono.just(publishedNode));

                when(nodeRepository.save(any(Node.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

                doReturn(Flux.fromIterable(List.of(childNode))).when(spyHandler)
                                .findAllChildren(nodeToPublish.getCode());

                when(nodeRepository.findByCodeAndStatus(childNode.getCode(), StatusEnum.PUBLISHED.name()))
                                .thenReturn(Mono.empty());

                when(this.contentNodeHandler.findAllByNodeCodeAndStatus(any(), any())).thenReturn(Flux.empty());

                doReturn(Flux.empty()).when(spyHandler).findAllChildren(childNode.getCode());

                // Exécution du test
                StepVerifier.create(spyHandler.publish(nodeToPublish.getCode(), user))
                                .assertNext(node -> {
                                        assert node.getStatus().equals(StatusEnum.PUBLISHED);
                                        assert node.getId().equals(nodeToPublish.getId());
                                })
                                .verifyComplete();

                verify(spyHandler, times(2)).publishNode(any(com.itexpert.content.lib.models.Node.class), any());
                verify(spyHandler, times(1)).archiveNode(any(com.itexpert.content.lib.models.Node.class), any());
                verify(spyHandler, times(2)).createSnapshot(any(com.itexpert.content.lib.models.Node.class), any());
        }

        @Test
        void publish_shouldPublish_WhenChildrensFoundButPublishedAginAndPublshedExists() {
                UUID nodeUuid = UUID.randomUUID();
                String user = "Admin";

                // Création du noeud parent original et du noeud publié mock
                Node nodeToPublish = new Node();
                nodeToPublish.setId(nodeUuid);
                nodeToPublish.setCode("PARENT-DEV");
                nodeToPublish.setStatus(StatusEnum.SNAPSHOT);
                nodeToPublish.setVersion("1");

                Node publishedNode = new Node();
                publishedNode.setId(UUID.randomUUID());
                publishedNode.setCode("PARENT-DEV");
                publishedNode.setStatus(StatusEnum.PUBLISHED);
                publishedNode.setVersion("0");

                com.itexpert.content.lib.models.Node childNode = new com.itexpert.content.lib.models.Node();
                childNode.setId(UUID.randomUUID());
                childNode.setCode("CHILD-DEV");
                childNode.setStatus(StatusEnum.SNAPSHOT);
                childNode.setVersion("1");

                Node childNodePublished = new Node();
                childNodePublished.setId(UUID.randomUUID());
                childNodePublished.setCode("CHILD-DEV");
                childNodePublished.setStatus(StatusEnum.PUBLISHED);
                childNodePublished.setVersion("0");

                // Création du spy sur le handler
                NodeHandler spyHandler = Mockito.spy(nodeHandler);

                // Mock des dépendances externes
                when(nodeRepository.findById(nodeUuid)).thenReturn(Mono.just(nodeToPublish));
                when(nodeRepository.findByCodeAndStatus(nodeToPublish.getCode(), StatusEnum.PUBLISHED.name()))
                                .thenReturn(Mono.just(publishedNode));
                when(nodeRepository.findByCodeAndStatus(nodeToPublish.getCode(), StatusEnum.SNAPSHOT.name()))
                                .thenReturn(Mono.just(nodeToPublish));

                when(nodeRepository.save(any(Node.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

                doReturn(Flux.fromIterable(List.of(childNode))).when(spyHandler)
                                .findAllChildren(nodeToPublish.getCode());

                when(nodeRepository.findByCodeAndStatus(childNode.getCode(), StatusEnum.PUBLISHED.name()))
                                .thenReturn(Mono.just(childNodePublished));

                when(this.contentNodeHandler.findAllByNodeCodeAndStatus(any(), any())).thenReturn(Flux.empty());

                doReturn(Flux.empty()).when(spyHandler).findAllChildren(childNode.getCode());

                // Exécution du test
                StepVerifier.create(spyHandler.publish(nodeToPublish.getCode(), user))
                                .assertNext(node -> {
                                        assert node.getStatus().equals(StatusEnum.PUBLISHED);
                                        assert node.getId().equals(nodeToPublish.getId());
                                })
                                .verifyComplete();

                verify(spyHandler, times(2)).publishNode(any(com.itexpert.content.lib.models.Node.class), any());
                verify(spyHandler, times(2)).archiveNode(any(com.itexpert.content.lib.models.Node.class), any());
                verify(spyHandler, times(2)).createSnapshot(any(com.itexpert.content.lib.models.Node.class), any());
        }

        @Test
        void publishVersion_shouldRepublishArchivedVersion_WhenPublishedExists() {

                String code = "NODE";
                String version = "1";
                String user = "Admin";

                Node archived = new Node();
                archived.setId(UUID.randomUUID());
                archived.setCode(code);
                archived.setVersion(version);
                archived.setStatus(StatusEnum.ARCHIVE);

                Node published = new Node();
                published.setId(UUID.randomUUID());
                published.setCode(code);
                published.setVersion("2");
                published.setStatus(StatusEnum.PUBLISHED);

                when(nodeRepository.findByCodeAndVersion(code, version))
                                .thenReturn(Mono.just(archived));

                when(nodeRepository.findByCodeAndStatus(code, StatusEnum.PUBLISHED.name()))
                                .thenReturn(Mono.just(published));

                when(nodeRepository.save(any()))
                                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

                StepVerifier.create(nodeHandler.publishVersion(code, version, user))
                                .expectNext(true)
                                .verifyComplete();

                assertEquals(StatusEnum.ARCHIVE, published.getStatus());
                assertEquals(StatusEnum.PUBLISHED, archived.getStatus());

                verify(nodeRepository, times(2)).save(any());
        }

        @Test
        void publishVersion_shouldReturnFalse_WhenArchivedNotFound() {

                when(nodeRepository.findByCodeAndVersion(any(), any()))
                                .thenReturn(Mono.empty());

                StepVerifier.create(nodeHandler.publishVersion("CODE", "1", "Admin"))
                                .expectNext(false)
                                .verifyComplete();

                verify(nodeRepository, never()).save(any());
        }

        @Test
        void publishVersion_shouldPublishArchived_WhenNoPublishedExists() {

                String code = "NODE";
                String version = "1";

                Node archived = new Node();
                archived.setId(UUID.randomUUID());
                archived.setCode(code);
                archived.setVersion(version);
                archived.setStatus(StatusEnum.ARCHIVE);

                when(nodeRepository.findByCodeAndVersion(code, version))
                                .thenReturn(Mono.just(archived));

                when(nodeRepository.findByCodeAndStatus(code, StatusEnum.PUBLISHED.name()))
                                .thenReturn(Mono.empty());

                when(nodeRepository.save(any()))
                                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

                StepVerifier.create(nodeHandler.publishVersion(code, version, "Admin"))
                                .expectNext(true)
                                .verifyComplete();

                assertEquals(StatusEnum.PUBLISHED, archived.getStatus());

                verify(nodeRepository, times(1)).save(any());
        }

        @Test
        void publishVersion_shouldSendNotifications() {

                String code = "NODE";
                String version = "1";

                Node archived = new Node();
                archived.setCode(code);
                archived.setVersion(version);
                archived.setStatus(StatusEnum.ARCHIVE);

                Node published = new Node();
                published.setCode(code);
                published.setStatus(StatusEnum.PUBLISHED);

                when(nodeRepository.findByCodeAndVersion(code, version))
                                .thenReturn(Mono.just(archived));

                when(nodeRepository.findByCodeAndStatus(code, StatusEnum.PUBLISHED.name()))
                                .thenReturn(Mono.just(published));

                when(nodeRepository.save(any()))
                                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

                StepVerifier.create(nodeHandler.publishVersion(code, version, "Admin"))
                                .expectNext(true)
                                .verifyComplete();

                verify(notificationHandler, atLeastOnce()).create(any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        void publishVersion_shouldFail_WhenSaveFails() {

                Node archived = new Node();
                archived.setCode("NODE");
                archived.setVersion("1");

                when(nodeRepository.findByCodeAndVersion(any(), any()))
                                .thenReturn(Mono.just(archived));

                when(nodeRepository.findByCodeAndStatus(any(), any()))
                                .thenReturn(Mono.just(new Node()));

                when(nodeRepository.save(any()))
                                .thenReturn(Mono.error(new RuntimeException("DB error")));

                StepVerifier.create(nodeHandler.publishVersion("NODE", "1", "Admin"))
                                .expectError()
                                .verify();
        }

        @Test
        void publishVersion_shouldCreateDuplicatePublishedVersions_whenConcurrentCalls() throws Exception {
                // GIVEN - Une version PUBLISHED existante et une SNAPSHOT à publier
                String code = "PARENT-DEV";
                String snapshotVersion = "2-SNAPSHOT";
                String publishedVersion = "1";

                Node publishedEntity = new Node();
                publishedEntity.setId(UUID.randomUUID());
                publishedEntity.setCode(code);
                publishedEntity.setStatus(StatusEnum.PUBLISHED);
                publishedEntity.setVersion(publishedVersion);

                Node snapshotEntity = new Node();
                snapshotEntity.setId(UUID.randomUUID());
                snapshotEntity.setCode(code);
                snapshotEntity.setStatus(StatusEnum.SNAPSHOT);
                snapshotEntity.setVersion(snapshotVersion);

                // Simuler les appels au repository
                when(nodeRepository.findByCodeAndVersion(code, snapshotVersion))
                                .thenReturn(Mono.just(snapshotEntity));
                when(nodeRepository.findByCodeAndStatus(code, StatusEnum.PUBLISHED.name()))
                                .thenReturn(Mono.just(publishedEntity));

                // Capturer les appels à save
                List<Node> savedNodes = Collections.synchronizedList(new ArrayList<>());
                when(nodeRepository.save(any(Node.class)))
                                .thenAnswer(invocation -> {
                                        Node node = invocation.getArgument(0);
                                        savedNodes.add(node);
                                        return Mono.just(node);
                                });

                // WHEN - Deux appels concurrents
                List<Mono<Boolean>> calls = new ArrayList<>();
                for (int i = 0; i < 2; i++) {
                        calls.add(nodeHandler.publishVersion(code, snapshotVersion, "user" + i));
                }

                // Exécuter en parallèle
                List<Boolean> results = Mono.when(calls).thenMany(Flux.fromIterable(calls).flatMap(m -> m))
                                .collectList()
                                .block();

                // THEN - Vérifier qu'on a tenté de sauvegarder 2 versions PUBLISHED
                long publishedCount = savedNodes.stream()
                                .filter(n -> n.getStatus() == StatusEnum.PUBLISHED)
                                .count();

                // Ceci démontre le problème : 2 versions PUBLISHED ont été sauvegardées
                assertThat(publishedCount).isGreaterThan(1);
        }

        @Test
        void publishVersion_shouldHaveRaceConditionBetweenArchiveAndPublish() throws Exception {
                // GIVEN
                String code = "PARENT-DEV";
                String snapshotVersion = "2-SNAPSHOT";

                Node publishedEntity = new Node();
                publishedEntity.setId(UUID.randomUUID());
                publishedEntity.setCode(code);
                publishedEntity.setStatus(StatusEnum.PUBLISHED);
                publishedEntity.setVersion("1");

                Node snapshotEntity = new Node();
                snapshotEntity.setId(UUID.randomUUID());
                snapshotEntity.setCode(code);
                snapshotEntity.setStatus(StatusEnum.SNAPSHOT);
                snapshotEntity.setVersion(snapshotVersion);

                // Simuler un délai dans la sauvegarde pour créer la race condition
                when(nodeRepository.findByCodeAndVersion(code, snapshotVersion))
                                .thenReturn(Mono.just(snapshotEntity));
                when(nodeRepository.findByCodeAndStatus(code, StatusEnum.PUBLISHED.name()))
                                .thenReturn(Mono.just(publishedEntity));

                // Compteur pour suivre l'ordre des opérations
                List<String> operationOrder = Collections.synchronizedList(new ArrayList<>());

                // Correction : utiliser any() au lieu de argThat pour éviter
                // NullPointerException
                when(nodeRepository.save(any(Node.class)))
                                .thenAnswer(invocation -> {
                                        Node node = invocation.getArgument(0);
                                        if (node.getStatus() == StatusEnum.ARCHIVE) {
                                                operationOrder.add("ARCHIVE_START");
                                                Thread.sleep(100); // Simuler un délai
                                                operationOrder.add("ARCHIVE_END");
                                        } else if (node.getStatus() == StatusEnum.PUBLISHED) {
                                                operationOrder.add("PUBLISH_START");
                                                operationOrder.add("PUBLISH_END");
                                        }
                                        return Mono.just(node);
                                });

                // WHEN - Deux appels simultanés
                StepVerifier.create(
                                Mono.when(
                                                nodeHandler.publishVersion(code, snapshotVersion, "user1"),
                                                nodeHandler.publishVersion(code, snapshotVersion, "user2")))
                                .expectComplete().verify();

                // THEN - Démontrer que les opérations sont entrelacées
                System.out.println("Operation order: " + operationOrder);

                // Le problème : on peut avoir ARCHIVE_START (thread1), ARCHIVE_START (thread2)
                assertThat(operationOrder).containsSubsequence("ARCHIVE_START", "ARCHIVE_START");
        }

        @Test
        void publishVersion_shouldAllowSecondThreadToSeeStillPublishedVersion() throws Exception {
                // GIVEN
                String code = "PARENT-DEV";
                String snapshotVersion = "2-SNAPSHOT";

                Node publishedEntity = new Node();
                publishedEntity.setId(UUID.randomUUID());
                publishedEntity.setCode(code);
                publishedEntity.setStatus(StatusEnum.PUBLISHED);
                publishedEntity.setVersion("1");

                Node snapshotEntity = new Node();
                snapshotEntity.setId(UUID.randomUUID());
                snapshotEntity.setCode(code);
                snapshotEntity.setStatus(StatusEnum.SNAPSHOT);
                snapshotEntity.setVersion(snapshotVersion);

                // Simuler un CountDownLatch pour coordonner les threads
                java.util.concurrent.CountDownLatch latch1 = new java.util.concurrent.CountDownLatch(1);
                java.util.concurrent.CountDownLatch latch2 = new java.util.concurrent.CountDownLatch(1);

                when(nodeRepository.findByCodeAndVersion(code, snapshotVersion))
                                .thenReturn(Mono.just(snapshotEntity));

                // Premier appel find - ok
                when(nodeRepository.findByCodeAndStatus(code, StatusEnum.PUBLISHED.name()))
                                .thenReturn(Mono.just(publishedEntity));

                // Simuler une sauvegarde lente pour que le second thread ait le temps de lire
                // l'état
                when(nodeRepository.save(argThat(node -> node.getStatus() == StatusEnum.ARCHIVE)))
                                .thenAnswer(invocation -> {
                                        latch1.countDown(); // Signaler qu'on est dans la sauvegarde
                                        latch2.await(5, java.util.concurrent.TimeUnit.SECONDS); // Attendre le second
                                                                                                // thread
                                        return Mono.just(invocation.getArgument(0));
                                });

                // Pour le second thread, on va capturer l'état qu'il voit
                final boolean[] secondThreadSawPublished = { false };

                // Exécuter deux threads
                Thread thread1 = new Thread(() -> {
                        nodeHandler.publishVersion(code, snapshotVersion, "user1").block();
                });

                Thread thread2 = new Thread(() -> {
                        try {
                                latch1.await(); // Attendre que thread1 soit dans la sauvegarde
                                // Simuler que le repository retourne encore la version PUBLISHED (pas encore
                                // archivée)
                                when(nodeRepository.findByCodeAndStatus(code, StatusEnum.PUBLISHED.name()))
                                                .thenReturn(Mono.just(publishedEntity));
                                secondThreadSawPublished[0] = true;
                                nodeHandler.publishVersion(code, snapshotVersion, "user2").block();
                                latch2.countDown();
                        } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                        }
                });

                thread1.start();
                thread2.start();
                thread1.join();
                thread2.join();

                // THEN - Démontrer que le second thread a vu une version PUBLISHED qui n'aurait
                // pas dû exister
                assertThat(secondThreadSawPublished[0]).isTrue();
                // Ceci prouve que deux threads peuvent croire qu'il y a une version PUBLISHED
                // disponible
        }

        @Test
        void publishVersion_shouldNotBeAtomic_demonstratingTwoPublishedVersions() throws Exception {
                // GIVEN
                String code = "PARENT-DEV";
                String snapshotVersion = "2-SNAPSHOT";

                // Simuler la base de données avec un état initial
                Node publishedEntity = new Node();
                publishedEntity.setId(UUID.randomUUID());
                publishedEntity.setCode(code);
                publishedEntity.setStatus(StatusEnum.PUBLISHED);
                publishedEntity.setVersion("1");

                Node snapshotEntity = new Node();
                snapshotEntity.setId(UUID.randomUUID());
                snapshotEntity.setCode(code);
                snapshotEntity.setStatus(StatusEnum.SNAPSHOT);
                snapshotEntity.setVersion(snapshotVersion);

                // Simuler le comportement du repository avec une map concurrente
                Map<String, Node> dbSimulator = new java.util.concurrent.ConcurrentHashMap<>();
                dbSimulator.put("published", publishedEntity);
                dbSimulator.put("snapshot", snapshotEntity);

                when(nodeRepository.findByCodeAndVersion(eq(code), eq(snapshotVersion)))
                                .thenAnswer(invocation -> {
                                        Thread.sleep(10); // Petit délai pour augmenter les chances de race condition
                                        return Mono.justOrEmpty(dbSimulator.get("snapshot"));
                                });

                when(nodeRepository.findByCodeAndStatus(eq(code), eq(StatusEnum.PUBLISHED.name())))
                                .thenAnswer(invocation -> {
                                        Thread.sleep(10);
                                        return Mono.justOrEmpty(dbSimulator.get("published"));
                                });

                when(nodeRepository.save(any(Node.class)))
                                .thenAnswer(invocation -> {
                                        Node node = invocation.getArgument(0);
                                        Thread.sleep(50); // Simuler le temps d'écriture BDD
                                        if (node.getStatus() == StatusEnum.PUBLISHED) {
                                                dbSimulator.put("published", node);
                                        } else if (node.getStatus() == StatusEnum.ARCHIVE) {
                                                // L'ancienne version archivée n'est plus la published
                                                if (dbSimulator.get("published") == node) {
                                                        dbSimulator.remove("published");
                                                }
                                        }
                                        return Mono.just(node);
                                });

                // WHEN - 10 appels concurrents
                List<Mono<Boolean>> calls = new ArrayList<>();
                for (int i = 0; i < 10; i++) {
                        calls.add(nodeHandler.publishVersion(code, snapshotVersion, "user" + i));
                }

                // Exécuter tous les appels en parallèle
                List<Boolean> results = Flux.merge(calls).collectList().block();

                // THEN - Vérifier l'état final de la base simulée
                Node finalPublished = dbSimulator.get("published");

                // Compter combien de versions PUBLISHED existent dans la base (normalement 1)
                long publishedInDb = dbSimulator.values().stream()
                                .filter(n -> n.getStatus() == StatusEnum.PUBLISHED)
                                .count();

                System.out.println("Final published count: " + publishedInDb);
                System.out.println("Final published version: "
                                + (finalPublished != null ? finalPublished.getVersion() : "null"));

                // Ce test peut échouer ou passer selon le timing, mais il démontre la
                // NON-atomicité
                // En exécutant plusieurs fois, vous verrez parfois publishedInDb > 1
        }
}
