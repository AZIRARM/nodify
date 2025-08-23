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

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
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
                nodeSlugHelper
        );
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
        when(notificationHandler.create(any(), any(), any(), any(), any(), any())).thenReturn(Mono.just(notification));

    }

    @Test
    void publish_shouldCallPublishNode_WhenNoChildreensAndNoPublishedExists() {
        UUID nodeUuid = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        // Parent SNAPSHOT
        Node parentSnapshot = new Node();
        parentSnapshot.setId(nodeUuid);
        parentSnapshot.setCode("PARENT-DEV");
        parentSnapshot.setStatus(StatusEnum.SNAPSHOT);
        parentSnapshot.setVersion("0");

        // Mocks
        when(nodeRepository.findById(nodeUuid)).thenReturn(Mono.just(parentSnapshot));

        when(nodeRepository.findByCodeAndStatus("PARENT-DEV", StatusEnum.PUBLISHED.name()))
                .thenReturn(Mono.empty());

        when(nodeRepository.save(any(Node.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        when(contentNodeHandler.findAllByNodeCodeAndStatus(any(), any())).thenReturn(Flux.empty());

        NodeHandler spyHandler = Mockito.spy(nodeHandler);
        doReturn(Flux.empty())
                .when(spyHandler)
                .findAllChildren(anyString());

        // Exécution et vérification du test
        StepVerifier.create(spyHandler.publish(nodeUuid, userId))
                .expectNextMatches(node ->
                        node.getStatus().equals(StatusEnum.PUBLISHED) &&
                                node.getCode().equals("PARENT-DEV") &&
                                node.getId().equals(nodeUuid)
                )
                .verifyComplete();

        ArgumentCaptor<com.itexpert.content.lib.models.Node> nodeCaptor =
                ArgumentCaptor.forClass(com.itexpert.content.lib.models.Node.class);

        verify(spyHandler).publishNode(nodeCaptor.capture(), eq(userId));
        assertEquals("PARENT-DEV", nodeCaptor.getValue().getCode());


        verify(spyHandler, times(1)).publishNode(any(com.itexpert.content.lib.models.Node.class), any(UUID.class));
        verify(spyHandler, times(0)).archiveNode(any(com.itexpert.content.lib.models.Node.class), any(UUID.class));
        verify(spyHandler, times(1)).createSnapshot(any(com.itexpert.content.lib.models.Node.class), any(UUID.class));
    }

    @Test
    void publish_shouldPublish_WhenNoChildrensFoundAndPublshedExists() {
        UUID nodeUuid = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

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
        when(nodeRepository.findByCodeAndStatus(any(), any()))
                .thenReturn(Mono.just(publishedNode));

        when(nodeRepository.save(any(Node.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        doReturn(Flux.empty()).when(spyHandler).findAllChildren(anyString());

        when(this.contentNodeHandler.findAllByNodeCodeAndStatus(any(), any())).thenReturn(Flux.empty());


        // Exécution du test
        StepVerifier.create(spyHandler.publish(nodeUuid, userId))
                .assertNext(node -> {
                    assert node.getStatus().equals(StatusEnum.PUBLISHED);
                    assert node.getId().equals(nodeToPublish.getId());
                })
                .verifyComplete();

        verify(spyHandler, times(1)).publishNode(any(com.itexpert.content.lib.models.Node.class), any(UUID.class));
        verify(spyHandler, times(1)).archiveNode(any(com.itexpert.content.lib.models.Node.class), any(UUID.class));
        verify(spyHandler, times(1)).createSnapshot(any(com.itexpert.content.lib.models.Node.class), any(UUID.class));
    }

    @Test
    void publish_shouldPublish_WhenChildrensFoundButNotPublishedAginAndPublshedExists() {
        UUID nodeUuid = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

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
        when(nodeRepository.findByCodeAndStatus(nodeToPublish.getCode(), StatusEnum.PUBLISHED.name()))
                .thenReturn(Mono.just(publishedNode));

        when(nodeRepository.save(any(Node.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        doReturn(Flux.fromIterable(List.of(childNode))).when(spyHandler).findAllChildren(nodeToPublish.getCode());

        when(nodeRepository.findByCodeAndStatus(childNode.getCode(), StatusEnum.PUBLISHED.name()))
                .thenReturn(Mono.empty());

        when(this.contentNodeHandler.findAllByNodeCodeAndStatus(any(), any())).thenReturn(Flux.empty());

        doReturn(Flux.empty()).when(spyHandler).findAllChildren(childNode.getCode());

        // Exécution du test
        StepVerifier.create(spyHandler.publish(nodeUuid, userId))
                .assertNext(node -> {
                    assert node.getStatus().equals(StatusEnum.PUBLISHED);
                    assert node.getId().equals(nodeToPublish.getId());
                })
                .verifyComplete();

        verify(spyHandler, times(2)).publishNode(any(com.itexpert.content.lib.models.Node.class), any(UUID.class));
        verify(spyHandler, times(1)).archiveNode(any(com.itexpert.content.lib.models.Node.class), any(UUID.class));
        verify(spyHandler, times(2)).createSnapshot(any(com.itexpert.content.lib.models.Node.class), any(UUID.class));
    }


    @Test
    void publish_shouldPublish_WhenChildrensFoundButPublishedAginAndPublshedExists() {
        UUID nodeUuid = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

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

        when(nodeRepository.save(any(Node.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        doReturn(Flux.fromIterable(List.of(childNode))).when(spyHandler).findAllChildren(nodeToPublish.getCode());

        when(nodeRepository.findByCodeAndStatus(childNode.getCode(), StatusEnum.PUBLISHED.name()))
                .thenReturn(Mono.just(childNodePublished));

        when(this.contentNodeHandler.findAllByNodeCodeAndStatus(any(), any())).thenReturn(Flux.empty());

        doReturn(Flux.empty()).when(spyHandler).findAllChildren(childNode.getCode());

        // Exécution du test
        StepVerifier.create(spyHandler.publish(nodeUuid, userId))
                .assertNext(node -> {
                    assert node.getStatus().equals(StatusEnum.PUBLISHED);
                    assert node.getId().equals(nodeToPublish.getId());
                })
                .verifyComplete();

        verify(spyHandler, times(2)).publishNode(any(com.itexpert.content.lib.models.Node.class), any(UUID.class));
        verify(spyHandler, times(2)).archiveNode(any(com.itexpert.content.lib.models.Node.class), any(UUID.class));
        verify(spyHandler, times(2)).createSnapshot(any(com.itexpert.content.lib.models.Node.class), any(UUID.class));
    }
}
