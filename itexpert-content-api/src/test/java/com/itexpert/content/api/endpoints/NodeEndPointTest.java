package com.itexpert.content.api.endpoints;

import com.itexpert.content.api.handlers.NodeHandler;
import com.itexpert.content.lib.enums.StatusEnum;
import com.itexpert.content.lib.models.Node;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NodeEndPointTest {

    @Mock
    private NodeHandler nodeHandler;

    @InjectMocks
    private NodeEndPoint nodeEndPoint;

    private Node node;
    private String code;
    private String slug;

    @BeforeEach
    void setUp() {
        code = "test-code";
        slug = "test-slug";

        node = new Node();
        node.setCode(code);
        node.setSlug(slug);
        node.setStatus(StatusEnum.PUBLISHED);
    }

    @Test
    void findAllShouldReturnAllNodes() {
        when(nodeHandler.findAll(StatusEnum.PUBLISHED)).thenReturn(Flux.just(node));

        StepVerifier.create(nodeEndPoint.findAll(StatusEnum.PUBLISHED))
                .expectNext(node)
                .verifyComplete();
    }

    @Test
    void findAllShouldReturnEmptyWhenNoNodes() {
        when(nodeHandler.findAll(StatusEnum.PUBLISHED)).thenReturn(Flux.empty());

        StepVerifier.create(nodeEndPoint.findAll(StatusEnum.PUBLISHED))
                .verifyComplete();
    }

    @Test
    void findAllWithDraftStatusShouldReturnDraftNodes() {
        node.setStatus(StatusEnum.SNAPSHOT);
        when(nodeHandler.findAll(StatusEnum.SNAPSHOT)).thenReturn(Flux.just(node));

        StepVerifier.create(nodeEndPoint.findAll(StatusEnum.SNAPSHOT))
                .expectNext(node)
                .verifyComplete();
    }

    @Test
    void findByCodeShouldReturnNode() {
        when(nodeHandler.findByCodeAndStatus(code, StatusEnum.PUBLISHED)).thenReturn(Mono.just(node));

        StepVerifier.create(nodeEndPoint.findByCode(code, StatusEnum.PUBLISHED))
                .expectNext(ResponseEntity.ok(node))
                .verifyComplete();
    }

    @Test
    void findByCodeShouldReturnNotFound() {
        when(nodeHandler.findByCodeAndStatus(code, StatusEnum.PUBLISHED)).thenReturn(Mono.empty());

        StepVerifier.create(nodeEndPoint.findByCode(code, StatusEnum.PUBLISHED))
                .expectNext(new ResponseEntity<>(HttpStatus.NOT_FOUND))
                .verifyComplete();
    }

    @Test
    void findBySlugShouldReturnNode() {
        when(nodeHandler.findBySlugAndStatus(slug, StatusEnum.PUBLISHED)).thenReturn(Mono.just(node));

        StepVerifier.create(nodeEndPoint.findBySlug(slug, StatusEnum.PUBLISHED))
                .expectNext(ResponseEntity.ok(node))
                .verifyComplete();
    }

    @Test
    void findBySlugShouldReturnNotFound() {
        when(nodeHandler.findBySlugAndStatus(slug, StatusEnum.PUBLISHED)).thenReturn(Mono.empty());

        StepVerifier.create(nodeEndPoint.findBySlug(slug, StatusEnum.PUBLISHED))
                .expectNext(new ResponseEntity<>(HttpStatus.NOT_FOUND))
                .verifyComplete();
    }

    @Test
    void findParentsNodesShouldReturnParentNodes() {
        when(nodeHandler.findParentsNodes(StatusEnum.PUBLISHED)).thenReturn(Flux.just(node));

        StepVerifier.create(nodeEndPoint.findParentsNodes(StatusEnum.PUBLISHED))
                .expectNext(node)
                .verifyComplete();
    }

    @Test
    void findParentsNodesShouldReturnEmptyWhenNoParents() {
        when(nodeHandler.findParentsNodes(StatusEnum.PUBLISHED)).thenReturn(Flux.empty());

        StepVerifier.create(nodeEndPoint.findParentsNodes(StatusEnum.PUBLISHED))
                .verifyComplete();
    }

    @Test
    void findAllByParentCodeShouldReturnChildNodes() {
        when(nodeHandler.findAllByParentCode(code, StatusEnum.PUBLISHED)).thenReturn(Flux.just(node));

        StepVerifier.create(nodeEndPoint.findAllByParentCode(code, StatusEnum.PUBLISHED))
                .expectNext(node)
                .verifyComplete();
    }

    @Test
    void findAllByParentCodeShouldReturnEmptyWhenNoChildren() {
        when(nodeHandler.findAllByParentCode(code, StatusEnum.PUBLISHED)).thenReturn(Flux.empty());

        StepVerifier.create(nodeEndPoint.findAllByParentCode(code, StatusEnum.PUBLISHED))
                .verifyComplete();
    }
}