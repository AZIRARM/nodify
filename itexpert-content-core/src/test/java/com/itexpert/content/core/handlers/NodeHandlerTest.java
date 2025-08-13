package com.itexpert.content.core.handlers;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.itexpert.content.core.helpers.NodeSlugHelper;
import com.itexpert.content.core.helpers.RenameNodeCodesHelper;
import com.itexpert.content.core.mappers.NodeMapper;
import com.itexpert.content.core.repositories.NodeRepository;
import com.itexpert.content.lib.entities.Node;
import com.itexpert.content.lib.enums.StatusEnum;
import com.itexpert.content.lib.models.ContentNode;
import com.itexpert.content.lib.models.Notification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.core.io.ClassPathResource;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class NodeHandlerTest {
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

        Notification notification = Notification.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
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

        when(notificationHandler.create(any(), any(), any(), any(), any(), any())).thenReturn(Mono.just(notification));

    }

    @Test
    void importNodes_parentFound_childExists_archivesAndIncrementsVersion() {
        // GIVEN: parent trouvé et enfant déjà existant
        when(nodeRepository.findByCodeAndStatus("PARENT-DEV", StatusEnum.SNAPSHOT.name()))
                .thenReturn(Mono.just(parentSnapshotEntity));
        when(nodeRepository.findByCodeAndStatus("NODE-PARENT-DEV", StatusEnum.SNAPSHOT.name()))
                .thenReturn(Mono.just(cloneNode(childNode))); // enfant trouvé

        when(nodeRepository.save(any()))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        nodeSlugHelper = mock(NodeSlugHelper.class);

        this.updateConfiguration(nodeSlugHelper);

        when(nodeSlugHelper.update(any(), any()))
                .thenAnswer(new Answer<Mono<com.itexpert.content.lib.models.Node>>() {
                    @Override
                    public Mono<com.itexpert.content.lib.models.Node> answer(InvocationOnMock invocationOnMock) {
                        com.itexpert.content.lib.models.Node node = invocationOnMock.getArgument(0);
                        node.setSlug("my-beautifull-slug-prod");
                        return Mono.just(node);
                    }
                });
        // WHEN
        Flux<com.itexpert.content.lib.models.Node> result = nodeHandler.importNodes(
                List.of(nodeMapper.fromEntity(cloneNode(childNode))), // Liste à importer
                "PARENT-DEV",
                false
        );

        // THEN
        StepVerifier.create(result)
                .assertNext(node -> {
                    assert node.getVersion().equals("1"); // version incrémentée
                    assert node.getStatus().equals(StatusEnum.SNAPSHOT);
                })
                .verifyComplete();

        verify(nodeRepository, atLeastOnce()).save(any());
    }

    @Test
    void importNodes_parentFound_childDoesNotExist_createsWithVersion0() {
        // GIVEN: parent trouvé mais enfant inexistant
        when(nodeRepository.findByCodeAndStatus("PARENT-DEV", StatusEnum.SNAPSHOT.name()))
                .thenReturn(Mono.just(parentSnapshotEntity));
        when(nodeRepository.findByCodeAndStatus("NODE-PARENT-DEV", StatusEnum.SNAPSHOT.name()))
                .thenReturn(Mono.empty());

        when(nodeRepository.save(any()))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        nodeSlugHelper = mock(NodeSlugHelper.class);

        this.updateConfiguration(nodeSlugHelper);

        when(nodeSlugHelper.update(any(), any()))
                .thenAnswer(new Answer<Mono<com.itexpert.content.lib.models.Node>>() {
                    @Override
                    public Mono<com.itexpert.content.lib.models.Node> answer(InvocationOnMock invocationOnMock) {
                        com.itexpert.content.lib.models.Node node = invocationOnMock.getArgument(0);
                        node.setSlug("my-beautifull-slug-prod");
                        return Mono.just(node);
                    }
                });

        // WHEN
        Flux<com.itexpert.content.lib.models.Node> result = nodeHandler.importNodes(
                List.of(nodeMapper.fromEntity(cloneNode(childNode))),
                "PARENT-DEV",
                false
        );

        // THEN
        StepVerifier.create(result)
                .assertNext(node -> {
                    assert node.getVersion().equals("0"); // version incrémentée
                    assert node.getStatus().equals(StatusEnum.SNAPSHOT);
                })
                .verifyComplete();
    }

    @Test
    void importNodes_parentNotFound_createsNodesDirectly() {
        // GIVEN: parent introuvable
        when(nodeRepository.findByCodeAndStatus("PARENT-DEV", StatusEnum.SNAPSHOT.name()))
                .thenReturn(Mono.empty());
        when(nodeRepository.findByCode("NODE-DEV"))
                .thenReturn(Flux.empty());
        when(nodeRepository.save(any()))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        nodeSlugHelper = mock(NodeSlugHelper.class);

        this.updateConfiguration(nodeSlugHelper);

        when(nodeSlugHelper.update(any(), any()))
                .thenAnswer(new Answer<Mono<com.itexpert.content.lib.models.Node>>() {
                    @Override
                    public Mono<com.itexpert.content.lib.models.Node> answer(InvocationOnMock invocationOnMock) {
                        com.itexpert.content.lib.models.Node node = invocationOnMock.getArgument(0);
                        node.setSlug("my-beautifull-slug-prod");
                        return Mono.just(node);
                    }
                });

        // WHEN
        Flux<com.itexpert.content.lib.models.Node> result = nodeHandler.importNodes(
                List.of(nodeMapper.fromEntity(cloneNode(childNode))),
                "PARENT-DEV",
                false
        );

        // THEN
        StepVerifier.create(result)
                .assertNext(node -> {
                    assert node.getVersion().equals("0"); // version incrémentée
                    assert node.getStatus().equals(StatusEnum.SNAPSHOT);
                })
                .verifyComplete();
    }

    @Test
    void importNodesWithSlug_parentNotFound_createsNodesDirectlyWithDifferentSlug() {
        // GIVEN: parent introuvable
        when(nodeRepository.findByCodeAndStatus("PARENT-DEV", StatusEnum.SNAPSHOT.name()))
                .thenReturn(Mono.empty());
        when(nodeRepository.findByCode("NODE-DEV"))
                .thenReturn(Flux.empty());
        when(nodeRepository.save(any()))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        com.itexpert.content.lib.models.Node model = nodeMapper.fromEntity(cloneNode(childNode));
        model.setSlug("my-beautifull-node-slug");


        nodeSlugHelper = mock(NodeSlugHelper.class);

        this.updateConfiguration(nodeSlugHelper);

        when(nodeSlugHelper.update(any(), any()))
                .thenAnswer(new Answer<Mono<com.itexpert.content.lib.models.Node>>() {
                    @Override
                    public Mono<com.itexpert.content.lib.models.Node> answer(InvocationOnMock invocationOnMock) {
                        com.itexpert.content.lib.models.Node node = invocationOnMock.getArgument(0);
                        node.setSlug("my-beautifull-slug-prod");
                        return Mono.just(node);
                    }
                });

        // WHEN
        Flux<com.itexpert.content.lib.models.Node> result = nodeHandler.importNodes(
                List.of(model),
                "PARENT-DEV",
                false
        );

        // THEN
        StepVerifier.create(result)
                .assertNext(node -> {
                    assert node.getVersion().equals("0"); // version incrémentée
                    assert node.getStatus().equals(StatusEnum.SNAPSHOT);
                    assert !node.getSlug().equals(model.getSlug());
                })
                .verifyComplete();
    }


    @Test
    void importNodesBlogFromFile_parentNotFound_childExists_createNodesAndChildes() {
        when(nodeRepository.findByCodeAndStatus(any(), any()))
                .thenReturn(Mono.empty());

        when(nodeRepository.findByCode(any()))
                .thenReturn(Flux.empty());

        when(contentNodeHandler.importContentNode(any()))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        when(nodeRepository.save(any()))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        nodeSlugHelper = mock(NodeSlugHelper.class);

        this.updateConfiguration(nodeSlugHelper);

        when(nodeSlugHelper.update(any(), any()))
                .thenAnswer(new Answer<Mono<com.itexpert.content.lib.models.Node>>() {
                    @Override
                    public Mono<com.itexpert.content.lib.models.Node> answer(InvocationOnMock invocationOnMock) {
                        com.itexpert.content.lib.models.Node node = invocationOnMock.getArgument(0);
                        node.setSlug("my-beautifull-slug-prod");
                        return Mono.just(node);
                    }
                });

        Flux<com.itexpert.content.lib.models.Node> result = nodeHandler.importNodes(
                this.getFromFile("tests/templates/BLOG-DEV.json"), // Liste à importer
                "PARENT-DEV",
                true
        );

        // Assure-toi que le flux est déclenché
        StepVerifier.create(result)
                .expectNextCount(3)  // ou le nombre exact de nodes importés
                .verifyComplete();

        verify(nodeRepository, times(9)).save(any(Node.class));
        verify(contentNodeHandler, times(15)).importContentNode(any(ContentNode.class));
    }

    @Test
    void importNodesLandingPageFromFile_parentNotFound_childExists_createNodesAndChildes() {
        when(nodeRepository.findByCodeAndStatus(any(), any()))
                .thenReturn(Mono.empty());

        when(nodeRepository.findByCode(any()))
                .thenReturn(Flux.empty());

        when(contentNodeHandler.importContentNode(any()))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        when(nodeRepository.save(any()))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        nodeSlugHelper = mock(NodeSlugHelper.class);

        this.updateConfiguration(nodeSlugHelper);

        when(nodeSlugHelper.update(any(), any()))
                .thenAnswer(new Answer<Mono<com.itexpert.content.lib.models.Node>>() {
                    @Override
                    public Mono<com.itexpert.content.lib.models.Node> answer(InvocationOnMock invocationOnMock) {
                        com.itexpert.content.lib.models.Node node = invocationOnMock.getArgument(0);
                        node.setSlug("my-beautifull-slug-prod");
                        return Mono.just(node);
                    }
                });

        List<com.itexpert.content.lib.models.Node> nodes = this.getFromFile("tests/templates/LANDINGPAGE-DEV.json");
        Flux<com.itexpert.content.lib.models.Node> result = nodeHandler.importNodes(
                nodes, // Liste à importer
                "PARENT-DEV",
                true
        );

        // Assure-toi que le flux est déclenché
        StepVerifier.create(result)
                .expectNextCount(3)  // ou le nombre exact de nodes importés
                .verifyComplete();


        List<com.itexpert.content.lib.models.Node> importedNodes = result.collectList().block();

        verify(nodeRepository, times(18)).save(any(Node.class));
        verify(contentNodeHandler, times(40)).importContentNode(any(ContentNode.class));
    }

    private Node cloneNode(Node original) {
        Node clone = new Node();
        clone.setId(original.getId());
        clone.setCode(original.getCode());
        clone.setParentCode(original.getParentCode());
        clone.setVersion(original.getVersion());
        clone.setStatus(original.getStatus());
        clone.setCreationDate(original.getCreationDate());
        clone.setModificationDate(original.getModificationDate());
        return clone;
    }

    private List<com.itexpert.content.lib.models.Node> getFromFile(String template) {
        ClassPathResource resource = new ClassPathResource(template);
        InputStreamReader reader = null;
        try {
            reader = new InputStreamReader(resource.getInputStream());
        } catch (IOException e) {
            return List.of();
        }
        Type listType = new TypeToken<List<com.itexpert.content.lib.models.Node>>() {
        }.getType();
        return (List<com.itexpert.content.lib.models.Node>) new Gson().fromJson(reader, listType);
    }

    private void updateConfiguration(NodeSlugHelper nodeSlugHelper) {
        nodeHandler = new NodeHandler(
                nodeRepository,
                nodeMapper,
                contentNodeHandler,
                notificationHandler,
                renameNodeCodesHelper,
                userHandler,
                nodeSlugHelper
        );
    }
}
