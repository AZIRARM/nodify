package com.itexpert.content.api.handlers;

import com.itexpert.content.api.helpers.NodeHelper;
import com.itexpert.content.api.mappers.NodeMapper;
import com.itexpert.content.api.repositories.NodeRepository;
import com.itexpert.content.lib.entities.Node;
import com.itexpert.content.lib.enums.StatusEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NodeHandlerTest {

    @Mock
    private NodeRepository nodeRepository;

    @Mock
    private NodeMapper nodeMapper;

    @Mock
    private NodeHelper nodeHelper;

    @InjectMocks
    private NodeHandler nodeHandler;

    private Node entityNode;
    private com.itexpert.content.lib.models.Node modelNode;

    @BeforeEach
    void setUp() {
        entityNode = new Node();
        entityNode.setCode("test-code");
        entityNode.setStatus(StatusEnum.PUBLISHED);
        entityNode.setSlug("test-slug");

        modelNode = new com.itexpert.content.lib.models.Node();
        modelNode.setCode("test-code");
        modelNode.setStatus(StatusEnum.PUBLISHED);
        modelNode.setSlug("test-slug");
    }

    @Test
    void findAllShouldReturnFilteredNodes() {
        when(nodeRepository.findAll()).thenReturn(Flux.just(entityNode));
        when(nodeMapper.fromEntity(entityNode)).thenReturn(modelNode);

        StepVerifier.create(nodeHandler.findAll(StatusEnum.PUBLISHED))
                .expectNext(modelNode)
                .verifyComplete();
    }

    @Test
    void findAllShouldReturnEmptyWhenNoNodes() {
        when(nodeRepository.findAll()).thenReturn(Flux.empty());

        StepVerifier.create(nodeHandler.findAll(StatusEnum.PUBLISHED))
                .verifyComplete();
    }

    @Test
    void findByCodeAndStatusShouldReturnNode() {
        when(nodeHelper.evaluateNode("test-code", StatusEnum.PUBLISHED)).thenReturn(Mono.just(modelNode));
        when(nodeRepository.findByCodeAndStatus("test-code", StatusEnum.PUBLISHED.name())).thenReturn(Mono.just(entityNode));
        when(nodeMapper.fromEntity(entityNode)).thenReturn(modelNode);

        StepVerifier.create(nodeHandler.findByCodeAndStatus("test-code", StatusEnum.PUBLISHED))
                .expectNext(modelNode)
                .verifyComplete();
    }

    @Test
    void findByCodeAndStatusShouldReturnEmptyWhenHelperFails() {
        when(nodeHelper.evaluateNode("unknown", StatusEnum.PUBLISHED)).thenReturn(Mono.empty());

        StepVerifier.create(nodeHandler.findByCodeAndStatus("unknown", StatusEnum.PUBLISHED))
                .verifyComplete();
    }

    @Test
    void findBySlugAndStatusShouldReturnNode() {
        when(nodeRepository.findBySlugAndStatus("test-slug", StatusEnum.PUBLISHED.name())).thenReturn(Mono.just(entityNode));
        when(nodeHelper.evaluateNode("test-code", StatusEnum.PUBLISHED)).thenReturn(Mono.just(modelNode));
        when(nodeRepository.findByCodeAndStatus("test-code", StatusEnum.PUBLISHED.name())).thenReturn(Mono.just(entityNode));
        when(nodeMapper.fromEntity(entityNode)).thenReturn(modelNode);

        StepVerifier.create(nodeHandler.findBySlugAndStatus("test-slug", StatusEnum.PUBLISHED))
                .expectNext(modelNode)
                .verifyComplete();
    }

    @Test
    void findBySlugAndStatusShouldReturnEmptyWhenSlugNotFound() {
        when(nodeRepository.findBySlugAndStatus("unknown", StatusEnum.PUBLISHED.name())).thenReturn(Mono.empty());

        StepVerifier.create(nodeHandler.findBySlugAndStatus("unknown", StatusEnum.PUBLISHED))
                .verifyComplete();
    }

    @Test
    void findParentsNodesShouldReturnParentNodes() {
        when(nodeRepository.findParentsNodesByStatus(StatusEnum.PUBLISHED.name())).thenReturn(Flux.just(entityNode));
        when(nodeMapper.fromEntity(entityNode)).thenReturn(modelNode);
        when(nodeHelper.evaluateNode(modelNode.getCode(), StatusEnum.PUBLISHED)).thenReturn(Mono.just(modelNode));

        StepVerifier.create(nodeHandler.findParentsNodes(StatusEnum.PUBLISHED))
                .expectNext(modelNode)
                .verifyComplete();
    }

    @Test
    void findParentsNodesShouldReturnEmptyWhenNoParents() {
        when(nodeRepository.findParentsNodesByStatus(StatusEnum.PUBLISHED.name())).thenReturn(Flux.empty());

        StepVerifier.create(nodeHandler.findParentsNodes(StatusEnum.PUBLISHED))
                .verifyComplete();
    }

    @Test
    void findAllByParentCodeShouldReturnChildNodes() {
        when(nodeRepository.findByParentCodeAndStatus("parent-code", StatusEnum.PUBLISHED.name())).thenReturn(Flux.just(entityNode));
        when(nodeMapper.fromEntity(entityNode)).thenReturn(modelNode);

        StepVerifier.create(nodeHandler.findAllByParentCode("parent-code", StatusEnum.PUBLISHED))
                .expectNext(modelNode)
                .verifyComplete();
    }

    @Test
    void findAllByParentCodeShouldReturnEmptyWhenNoChildren() {
        when(nodeRepository.findByParentCodeAndStatus("parent-code", StatusEnum.PUBLISHED.name())).thenReturn(Flux.empty());

        StepVerifier.create(nodeHandler.findAllByParentCode("parent-code", StatusEnum.PUBLISHED))
                .verifyComplete();
    }

    @Test
    void findAllByParentCodeShouldReturnMultipleChildren() {
        Node entityNode2 = new Node();
        entityNode2.setCode("child-code-2");
        entityNode2.setStatus(StatusEnum.PUBLISHED);

        com.itexpert.content.lib.models.Node modelNode2 = new com.itexpert.content.lib.models.Node();
        modelNode2.setCode("child-code-2");
        modelNode2.setStatus(StatusEnum.PUBLISHED);

        when(nodeRepository.findByParentCodeAndStatus("parent-code", StatusEnum.PUBLISHED.name()))
                .thenReturn(Flux.just(entityNode, entityNode2));
        when(nodeMapper.fromEntity(entityNode)).thenReturn(modelNode);
        when(nodeMapper.fromEntity(entityNode2)).thenReturn(modelNode2);

        StepVerifier.create(nodeHandler.findAllByParentCode("parent-code", StatusEnum.PUBLISHED))
                .expectNext(modelNode, modelNode2)
                .verifyComplete();
    }
}