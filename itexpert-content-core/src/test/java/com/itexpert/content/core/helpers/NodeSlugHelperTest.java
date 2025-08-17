package com.itexpert.content.core.helpers;

import com.itexpert.content.core.repositories.ContentNodeRepository;
import com.itexpert.content.core.repositories.NodeRepository;
import com.itexpert.content.lib.entities.ContentNode;
import com.itexpert.content.lib.entities.Node;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NodeSlugHelperTest {
    private NodeRepository nodeRepository;
    private ContentNodeRepository contentNodeRepository;
    private NodeSlugHelper nodeSlugHelper;

    @BeforeEach
    void setup() {
        contentNodeRepository = mock(ContentNodeRepository.class);
        nodeRepository = mock(NodeRepository.class);
        nodeSlugHelper = new NodeSlugHelper(nodeRepository, contentNodeRepository);
    }


    @Test
    void testUpdate_slugExistsInNodeRepository_notUpdateKeepSlug() {
        com.itexpert.content.lib.models.Node node = new com.itexpert.content.lib.models.Node();
        node.setSlug("mySlug");
        node.setId(UUID.randomUUID());
        node.setCode("NODE-CODE");


        when(nodeRepository.findBySlugAndCode(any(), any())).thenReturn(Flux.fromIterable(List.of(new Node())));

        StepVerifier.create(nodeSlugHelper.update(node))
                .assertNext(n -> {
                    assert n.getSlug().equals("mySlug");
                })
                .verifyComplete();
    }

    @Test
    void testUpdate_slugNotExistsInNodeRepository_shouldHaveNewSlug() {
        com.itexpert.content.lib.models.Node node = new com.itexpert.content.lib.models.Node();
        node.setSlug("mySlug");
        node.setId(UUID.randomUUID());
        node.setCode("NODE-CODE");


        when(contentNodeRepository.findAllBySlug("mySlug-0")).thenReturn(Flux.empty());
        when(nodeRepository.findAllBySlug("mySlug-0")).thenReturn(Flux.empty());

        when(nodeRepository.findBySlugAndCode(any(), any())).thenReturn(Flux.empty());

        StepVerifier.create(nodeSlugHelper.update(node))
                .assertNext(n -> {
                    assert n.getSlug().equals("mySlug-0");
                })
                .verifyComplete();
    }

    @Test
    void testUpdate_slugExistForOtherNodeOrContent_shouldHaveNewDifferentSlug() {
        com.itexpert.content.lib.models.Node node = new com.itexpert.content.lib.models.Node();
        node.setSlug("mySlug");
        node.setId(UUID.randomUUID());
        node.setCode("NODE-CODE");


        when(contentNodeRepository.findAllBySlug("mySlug-0")).thenReturn(Flux.fromIterable(List.of(new ContentNode())));
        when(nodeRepository.findAllBySlug("mySlug-0")).thenReturn(Flux.empty());

        when(contentNodeRepository.findAllBySlug("mySlug-1")).thenReturn(Flux.empty());
        when(nodeRepository.findAllBySlug("mySlug-1")).thenReturn(Flux.empty());

        when(nodeRepository.findBySlugAndCode(any(), any())).thenReturn(Flux.empty());

        StepVerifier.create(nodeSlugHelper.update(node))
                .assertNext(n -> {
                    assert n.getSlug().equals("mySlug-1");
                })
                .verifyComplete();
    }

    @Test
    void testUpdate_slugExistForOtherNodeOrContentAndIncrementExist_shouldHaveNewDifferentSlugAndDifferentIncrement() {
        com.itexpert.content.lib.models.Node node = new com.itexpert.content.lib.models.Node();
        node.setSlug("mySlug-3");
        node.setId(UUID.randomUUID());
        node.setCode("NODE-CODE");


        when(contentNodeRepository.findAllBySlug("mySlug-0")).thenReturn(Flux.fromIterable(List.of(new ContentNode())));
        when(nodeRepository.findAllBySlug("mySlug-0")).thenReturn(Flux.empty());

        when(contentNodeRepository.findAllBySlug("mySlug-1")).thenReturn(Flux.empty());
        when(nodeRepository.findAllBySlug("mySlug-1")).thenReturn(Flux.empty());

        when(nodeRepository.findBySlugAndCode(any(), any())).thenReturn(Flux.empty());

        StepVerifier.create(nodeSlugHelper.update(node))
                .assertNext(n -> {
                    assert n.getSlug().equals("mySlug-1");
                })
                .verifyComplete();
    }

    @Test
    void testUpdate_slugExistForOtherNodeOrContentAndIncrementExistButNotDisponible_shouldHaveNewDifferentSlugAndDifferentIncrement() {
        com.itexpert.content.lib.models.Node node = new com.itexpert.content.lib.models.Node();
        node.setSlug("mySlug-1");
        node.setId(UUID.randomUUID());
        node.setCode("NODE-CODE");


        when(contentNodeRepository.findAllBySlug("mySlug-0")).thenReturn(Flux.fromIterable(List.of(new ContentNode())));
        when(nodeRepository.findAllBySlug("mySlug-0")).thenReturn(Flux.empty());

        when(contentNodeRepository.findAllBySlug("mySlug-1")).thenReturn(Flux.fromIterable(List.of(new ContentNode())));
        when(nodeRepository.findAllBySlug("mySlug-1")).thenReturn(Flux.empty());

        when(contentNodeRepository.findAllBySlug("mySlug-2")).thenReturn(Flux.empty());
        when(nodeRepository.findAllBySlug("mySlug-2")).thenReturn(Flux.empty());

        when(nodeRepository.findBySlugAndCode(any(), any())).thenReturn(Flux.empty());

        StepVerifier.create(nodeSlugHelper.update(node))
                .assertNext(n -> {
                    assert n.getSlug().equals("mySlug-2");
                })
                .verifyComplete();
    }
}