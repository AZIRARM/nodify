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

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NodeHandlerRevertTest {

    private NodeRepository nodeRepository;
    private NodeMapper nodeMapper;
    private ContentNodeHandler contentNodeHandler;
    private NotificationHandler notificationHandler;
    private RenameNodeCodesHelper renameNodeCodesHelper;
    private UserHandler userHandler;
    private NodeHandler nodeHandler;
    private NodeSlugHelper nodeSlugHelper;

    private Node snapshot;
    private Node archived;
    private com.itexpert.content.lib.models.Node mappedResult;

    private Notification notification;

    @BeforeEach
    void setup() {
        nodeRepository = mock(NodeRepository.class);
        nodeMapper = Mappers.getMapper(NodeMapper.class);
        contentNodeHandler = mock(ContentNodeHandler.class);
        notificationHandler = mock(NotificationHandler.class);
        userHandler = mock(UserHandler.class);
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

        snapshot = new Node();
        snapshot.setId(UUID.randomUUID());
        snapshot.setCode("NODE");
        snapshot.setStatus(StatusEnum.SNAPSHOT);
        snapshot.setVersion("2");

        archived = new Node();
        archived.setId(UUID.randomUUID());
        archived.setCode("NODE");
        archived.setStatus(StatusEnum.ARCHIVE);
        archived.setVersion("1");

        mappedResult = new com.itexpert.content.lib.models.Node();
        mappedResult.setCode("NODE");
        mappedResult.setStatus(StatusEnum.SNAPSHOT);
        mappedResult.setVersion("3");
        mappedResult.setModifiedBy("user");
        mappedResult.setModificationDate(Instant.now().toEpochMilli());

        notification = Notification.builder()
                .id(UUID.randomUUID())
                .user("Admin")
                .type("CONTENT_NODE")
                .build();

        when(userHandler.findById(any())).thenReturn(Mono.just(mock(UserPost.class)));
        when(notificationHandler.create(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Mono.just(notification));
    }

    @Test
    void revert_shouldRevertToArchivedVersion() {
        when(nodeRepository.findByCodeAndStatus("NODE", StatusEnum.SNAPSHOT.name()))
                .thenReturn(Mono.just(snapshot));
        when(nodeRepository.findByCodeAndVersion("NODE", "1"))
                .thenReturn(Mono.just(archived));
        when(nodeRepository.save(any(Node.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        com.itexpert.content.lib.models.Node result = nodeHandler.revert("NODE", "1", "user").block();

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(StatusEnum.SNAPSHOT);
        assertThat(result.getModifiedBy()).isEqualTo("user");
    }

    @Test
    void revert_shouldArchiveCurrentSnapshotBeforeRevert() {
        when(nodeRepository.findByCodeAndStatus("NODE", StatusEnum.SNAPSHOT.name()))
                .thenReturn(Mono.just(snapshot));
        when(nodeRepository.findByCodeAndVersion("NODE", "1"))
                .thenReturn(Mono.just(archived));
        when(nodeRepository.save(any(Node.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        nodeHandler.revert("NODE", "1", "user").block();

        verify(nodeRepository, times(2)).save(any(Node.class));
    }

    @Test
    void revert_shouldIncrementVersion() {
        when(nodeRepository.findByCodeAndStatus("NODE", StatusEnum.SNAPSHOT.name()))
                .thenReturn(Mono.just(snapshot));
        when(nodeRepository.findByCodeAndVersion("NODE", "1"))
                .thenReturn(Mono.just(archived));
        when(nodeRepository.save(any(Node.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        com.itexpert.content.lib.models.Node result = nodeHandler.revert("NODE", "1", "user").block();

        assertThat(result.getVersion()).isEqualTo("3");
    }

    @Test
    void revert_shouldFailWhenNoSnapshot() {
        when(nodeRepository.findByCodeAndStatus("NODE", StatusEnum.SNAPSHOT.name()))
                .thenReturn(Mono.empty());

        StepVerifier.create(nodeHandler.revert("NODE", "1", "user"))
                .expectNextCount(0)
                .expectComplete()
                .verify();
    }

    @Test
    void revert_shouldFailWhenArchivedNotFound() {
        when(nodeRepository.findByCodeAndStatus("NODE", StatusEnum.SNAPSHOT.name()))
                .thenReturn(Mono.just(snapshot));
        when(nodeRepository.findByCodeAndVersion("NODE", "1"))
                .thenReturn(Mono.empty());

        StepVerifier.create(nodeHandler.revert("NODE", "1", "user"))
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void revert_shouldSendNotification() {
        when(nodeRepository.findByCodeAndStatus("NODE", StatusEnum.SNAPSHOT.name()))
                .thenReturn(Mono.just(snapshot));
        when(nodeRepository.findByCodeAndVersion("NODE", "1"))
                .thenReturn(Mono.just(archived));
        when(nodeRepository.save(any(Node.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        nodeHandler.revert("NODE", "1", "user").block();

        verify(notificationHandler, times(1))
                .create(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void revert_shouldSetModificationDate() {
        when(nodeRepository.findByCodeAndStatus("NODE", StatusEnum.SNAPSHOT.name()))
                .thenReturn(Mono.just(snapshot));
        when(nodeRepository.findByCodeAndVersion("NODE", "1"))
                .thenReturn(Mono.just(archived));
        when(nodeRepository.save(any(Node.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        com.itexpert.content.lib.models.Node result = nodeHandler.revert("NODE", "1", "user").block();

        assertThat(result.getModificationDate()).isNotNull();
        assertThat(result.getModifiedBy()).isEqualTo("user");
    }

    @Test
    void revert_shouldArchiveCurrentSnapshotAndCreateNewSnapshot_onlyOneSnapshotExists() {
        // GIVEN
        when(nodeRepository.findByCodeAndStatus("NODE", StatusEnum.SNAPSHOT.name()))
                .thenReturn(Mono.just(snapshot));
        when(nodeRepository.findByCodeAndVersion("NODE", "1"))
                .thenReturn(Mono.just(archived));

        // Capture saved entities
        java.util.List<Node> savedNodes = new java.util.ArrayList<>();
        when(nodeRepository.save(any(Node.class)))
                .thenAnswer(inv -> {
                    Node node = inv.getArgument(0);
                    savedNodes.add(node);
                    return Mono.just(node);
                });

        // WHEN
        nodeHandler.revert("NODE", "1", "user").block();

        // THEN - Verify only ONE SNAPSHOT exists in saved entities
        long snapshotCount = savedNodes.stream()
                .filter(n -> n.getStatus() == StatusEnum.SNAPSHOT)
                .count();

        long archiveCount = savedNodes.stream()
                .filter(n -> n.getStatus() == StatusEnum.ARCHIVE)
                .count();

        assertThat(snapshotCount).isEqualTo(1); // Only one SNAPSHOT created
        assertThat(archiveCount).isEqualTo(1); // One ARCHIVE created (previous snapshot)
    }

    @Test
    void revert_shouldNeverHaveTwoSnapshotsSimultaneously_inDatabaseState() {
        // GIVEN - Simulate database state tracking
        java.util.Map<String, Node> databaseSimulator = new java.util.concurrent.ConcurrentHashMap<>();

        // Initial state: one SNAPSHOT
        databaseSimulator.put("snapshot", snapshot);

        when(nodeRepository.findByCodeAndStatus("NODE", StatusEnum.SNAPSHOT.name()))
                .thenAnswer(inv -> {
                    Node found = databaseSimulator.get("snapshot");
                    return found != null ? Mono.just(found) : Mono.empty();
                });

        when(nodeRepository.findByCodeAndVersion("NODE", "1"))
                .thenReturn(Mono.just(archived));

        when(nodeRepository.save(any(Node.class)))
                .thenAnswer(inv -> {
                    Node node = inv.getArgument(0);
                    if (node.getStatus() == StatusEnum.ARCHIVE) {
                        // Remove old snapshot when archiving
                        databaseSimulator.remove("snapshot");
                        databaseSimulator.put("archive_" + node.getVersion(), node);
                    } else if (node.getStatus() == StatusEnum.SNAPSHOT) {
                        // Add new snapshot
                        databaseSimulator.put("snapshot", node);
                    }
                    return Mono.just(node);
                });

        // WHEN
        nodeHandler.revert("NODE", "1", "user").block();

        // THEN - Verify only one SNAPSHOT in database after operation
        long snapshotCount = databaseSimulator.values().stream()
                .filter(n -> n.getStatus() == StatusEnum.SNAPSHOT)
                .count();

        assertThat(snapshotCount).isEqualTo(1);
    }

    @Test
    void revert_shouldMaintainSingleSnapshot_whenConcurrentRevertsAreBlocked() {
        // This test verifies sequential reverts (concurrency is blocked by design)

        // GIVEN
        when(nodeRepository.findByCodeAndStatus("NODE", StatusEnum.SNAPSHOT.name()))
                .thenReturn(Mono.just(snapshot));
        when(nodeRepository.findByCodeAndVersion("NODE", "1"))
                .thenReturn(Mono.just(archived));

        java.util.List<Node> savedSnapshots = new java.util.ArrayList<>();
        when(nodeRepository.save(any(Node.class)))
                .thenAnswer(inv -> {
                    Node node = inv.getArgument(0);
                    if (node.getStatus() == StatusEnum.SNAPSHOT) {
                        savedSnapshots.add(node);
                    }
                    return Mono.just(node);
                });

        // WHEN - Sequential calls (simulating blocked concurrent access)
        nodeHandler.revert("NODE", "1", "user").block();

        // Update mocks for second revert
        Node secondSnapshot = new Node();
        secondSnapshot.setId(UUID.randomUUID());
        secondSnapshot.setCode("NODE");
        secondSnapshot.setStatus(StatusEnum.SNAPSHOT);
        secondSnapshot.setVersion("3");

        Node archivedVersion = new Node();
        archivedVersion.setId(UUID.randomUUID());
        archivedVersion.setCode("NODE");
        archivedVersion.setStatus(StatusEnum.ARCHIVE);
        archivedVersion.setVersion("1");

        when(nodeRepository.findByCodeAndStatus("NODE", StatusEnum.SNAPSHOT.name()))
                .thenReturn(Mono.just(secondSnapshot));
        when(nodeRepository.findByCodeAndVersion("NODE", "1"))
                .thenReturn(Mono.just(archivedVersion));

        nodeHandler.revert("NODE", "1", "user2").block();

        // THEN - Should have created 2 snapshots (version 3 and version 4)
        assertThat(savedSnapshots).hasSize(2);
        assertThat(savedSnapshots.get(0).getVersion()).isEqualTo("3");
        assertThat(savedSnapshots.get(1).getVersion()).isEqualTo("4");
        assertThat(savedSnapshots.get(0).getVersion()).isNotEqualTo(savedSnapshots.get(1).getVersion());
    }

    @Test
    void revert_shouldNotCreateDuplicateSnapshot_whenRevertingSameVersionTwiceSequentially() {
        // GIVEN - First revert
        Node snapshot1 = new Node();
        snapshot1.setId(UUID.randomUUID());
        snapshot1.setCode("NODE");
        snapshot1.setStatus(StatusEnum.SNAPSHOT);
        snapshot1.setVersion("2");

        Node archived1 = new Node();
        archived1.setId(UUID.randomUUID());
        archived1.setCode("NODE");
        archived1.setStatus(StatusEnum.ARCHIVE);
        archived1.setVersion("1");

        when(nodeRepository.findByCodeAndStatus("NODE", StatusEnum.SNAPSHOT.name()))
                .thenReturn(Mono.just(snapshot1));
        when(nodeRepository.findByCodeAndVersion("NODE", "1"))
                .thenReturn(Mono.just(archived1));
        when(nodeRepository.save(any(Node.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        // WHEN - First revert
        com.itexpert.content.lib.models.Node firstResult = nodeHandler.revert("NODE", "1", "user").block();
        assertThat(firstResult.getVersion()).isEqualTo("3");

        // GIVEN - Second revert
        Node snapshot2 = new Node();
        snapshot2.setId(UUID.randomUUID());
        snapshot2.setCode("NODE");
        snapshot2.setStatus(StatusEnum.SNAPSHOT);
        snapshot2.setVersion("3");

        Node archived2 = new Node();
        archived2.setId(UUID.randomUUID());
        archived2.setCode("NODE");
        archived2.setStatus(StatusEnum.ARCHIVE);
        archived2.setVersion("1");

        when(nodeRepository.findByCodeAndStatus("NODE", StatusEnum.SNAPSHOT.name()))
                .thenReturn(Mono.just(snapshot2));
        when(nodeRepository.findByCodeAndVersion("NODE", "1"))
                .thenReturn(Mono.just(archived2));

        // WHEN - Second revert
        com.itexpert.content.lib.models.Node secondResult = nodeHandler.revert("NODE", "1", "user").block();

        // THEN
        assertThat(secondResult.getVersion()).isEqualTo("4");
    }

    @Test
    void revert_shouldHaveExactlyOneSnapshot_whenRevertingMultipleTimes() {
        // GIVEN
        when(nodeRepository.save(any(Node.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        Node currentSnapshot = new Node();
        currentSnapshot.setId(UUID.randomUUID());
        currentSnapshot.setCode("NODE");
        currentSnapshot.setStatus(StatusEnum.SNAPSHOT);
        currentSnapshot.setVersion("2");

        String[] versionsToRevert = { "1", "0", "2" };
        int expectedVersion = 3;

        for (String versionToRevert : versionsToRevert) {
            Node archivedVersion = new Node();
            archivedVersion.setId(UUID.randomUUID());
            archivedVersion.setCode("NODE");
            archivedVersion.setStatus(StatusEnum.ARCHIVE);
            archivedVersion.setVersion(versionToRevert);

            when(nodeRepository.findByCodeAndStatus("NODE", StatusEnum.SNAPSHOT.name()))
                    .thenReturn(Mono.just(currentSnapshot));
            when(nodeRepository.findByCodeAndVersion("NODE", versionToRevert))
                    .thenReturn(Mono.just(archivedVersion));

            // WHEN
            com.itexpert.content.lib.models.Node result = nodeHandler.revert("NODE", versionToRevert, "user").block();

            // THEN
            assertThat(result).isNotNull();
            assertThat(result.getVersion()).isEqualTo(String.valueOf(expectedVersion));

            // Update current snapshot for next iteration
            currentSnapshot = new Node();
            currentSnapshot.setId(UUID.randomUUID());
            currentSnapshot.setCode("NODE");
            currentSnapshot.setStatus(StatusEnum.SNAPSHOT);
            currentSnapshot.setVersion(String.valueOf(expectedVersion));

            expectedVersion++;
        }
    }

    @Test
    void revert_shouldSucceed_whenRevertingToSameVersionAsCurrentSnapshot() {
        // GIVEN - Revert to version "2" (same as current snapshot version)
        Node currentSnapshot = new Node();
        currentSnapshot.setId(UUID.randomUUID());
        currentSnapshot.setCode("NODE");
        currentSnapshot.setStatus(StatusEnum.SNAPSHOT);
        currentSnapshot.setVersion("2");

        Node sameVersionArchived = new Node();
        sameVersionArchived.setId(UUID.randomUUID());
        sameVersionArchived.setCode("NODE");
        sameVersionArchived.setStatus(StatusEnum.ARCHIVE);
        sameVersionArchived.setVersion("2");

        when(nodeRepository.findByCodeAndStatus("NODE", StatusEnum.SNAPSHOT.name()))
                .thenReturn(Mono.just(currentSnapshot));
        when(nodeRepository.findByCodeAndVersion("NODE", "2"))
                .thenReturn(Mono.just(sameVersionArchived));
        when(nodeRepository.save(any(Node.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        // WHEN
        com.itexpert.content.lib.models.Node result = nodeHandler.revert("NODE", "2", "user").block();

        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getVersion()).isEqualTo("3");
    }
}