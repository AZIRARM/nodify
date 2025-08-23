package com.itexpert.content.core.helpers;

import com.itexpert.content.core.handlers.SlugHandler;
import com.itexpert.content.core.mappers.ContentNodeMapper;
import com.itexpert.content.core.mappers.NodeMapper;
import com.itexpert.content.core.repositories.ContentNodeRepository;
import com.itexpert.content.core.repositories.NodeRepository;
import com.itexpert.content.lib.entities.ContentNode;
import com.itexpert.content.lib.entities.Node;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
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
    private NodeMapper nodeMapper;
    private SlugHandler slugHandler;

    @BeforeEach
    void setup() {
        contentNodeRepository = mock(ContentNodeRepository.class);
        nodeRepository = mock(NodeRepository.class);
        nodeMapper = Mappers.getMapper(NodeMapper.class);
        slugHandler = new SlugHandler(nodeRepository, contentNodeRepository);

        nodeSlugHelper = new NodeSlugHelper(nodeRepository, nodeMapper, contentNodeRepository, slugHandler);
    }


    @Test
    void testUpdate_slugExistsInNodeRepository_notUpdateKeepSlug() {
        com.itexpert.content.lib.models.Node node = new com.itexpert.content.lib.models.Node();
        node.setSlug("mySlug");
        node.setId(UUID.randomUUID());
        node.setCode("NODE-CODE");

        com.itexpert.content.lib.entities.Node node0 = new com.itexpert.content.lib.entities.Node();
        node0.setSlug("mySlug");
        node0.setId(UUID.randomUUID());
        node0.setCode("NODE-CODE");

        when(nodeRepository.findBySlug("mySlug")).thenReturn(Flux.just(node0));
        when(contentNodeRepository.findBySlug("mySlug")).thenReturn(Flux.empty());

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

        when(contentNodeRepository.findBySlug("mySlug")).thenReturn(Flux.empty());
        when(nodeRepository.findBySlug("mySlug")).thenReturn(Flux.empty());

        when(nodeRepository.findBySlugAndCode(any(), any())).thenReturn(Flux.empty());

        StepVerifier.create(nodeSlugHelper.update(node))
                .assertNext(n -> {
                    assert n.getSlug().equals("mySlug");
                })
                .verifyComplete();
    }

    @Test
    void testUpdate_slugExistForOtherNodeOrContent_shouldHaveNewDifferentSlug() {
        com.itexpert.content.lib.models.Node node = new com.itexpert.content.lib.models.Node();
        node.setSlug("mySlug");
        node.setId(UUID.randomUUID());
        node.setCode("NODE-CODE");

        com.itexpert.content.lib.entities.ContentNode content3 = new com.itexpert.content.lib.entities.ContentNode();
        content3.setSlug("mySlug-XXX");
        content3.setId(UUID.randomUUID());
        content3.setCode("XXXX");

        when(contentNodeRepository.findBySlug("mySlug")).thenReturn(Flux.fromIterable(List.of(content3)));
        when(nodeRepository.findBySlug("mySlug")).thenReturn(Flux.empty());


        when(contentNodeRepository.findBySlug("mySlug-1")).thenReturn(Flux.fromIterable(List.of(content3)));
        when(nodeRepository.findBySlug("mySlug-1")).thenReturn(Flux.empty());

        when(contentNodeRepository.findBySlug("mySlug-2")).thenReturn(Flux.empty());
        when(nodeRepository.findBySlug("mySlug-2")).thenReturn(Flux.empty());

        when(nodeRepository.findBySlugAndCode(any(), any())).thenReturn(Flux.empty());

        StepVerifier.create(nodeSlugHelper.update(node))
                .assertNext(n -> {
                    assert n.getSlug().equals("mySlug-2");
                })
                .verifyComplete();
    }

    @Test
    void testUpdate_slugExistForOtherNodeOrContentAndIncrementExist_shouldHaveNewDifferentSlugAndDifferentIncrement() {
        com.itexpert.content.lib.models.Node node = new com.itexpert.content.lib.models.Node();
        node.setSlug("mySlug-3");
        node.setId(UUID.randomUUID());
        node.setCode("NODE-CODE");

        com.itexpert.content.lib.entities.ContentNode content3 = new com.itexpert.content.lib.entities.ContentNode();
        content3.setSlug("mySlug-XXX");
        content3.setId(UUID.randomUUID());
        content3.setCode("XXXX");

        when(contentNodeRepository.findBySlug("mySlug-3")).thenReturn(Flux.just(content3));
        when(nodeRepository.findBySlug("mySlug-3")).thenReturn(Flux.empty());

        when(contentNodeRepository.findBySlug("mySlug-4")).thenReturn(Flux.just(content3));
        when(nodeRepository.findBySlug("mySlug-4")).thenReturn(Flux.empty());

        when(contentNodeRepository.findBySlug("mySlug-5")).thenReturn(Flux.empty());
        when(nodeRepository.findBySlug("mySlug-5")).thenReturn(Flux.empty());

        when(nodeRepository.findBySlugAndCode(any(), any())).thenReturn(Flux.empty());

        StepVerifier.create(nodeSlugHelper.update(node))
                .assertNext(n -> {
                    assert n.getSlug().equals("mySlug-5");
                })
                .verifyComplete();
    }

    @Test
    void testUpdate_slugExistForOtherNodeOrContentAndIncrementExistButNotDisponible_shouldHaveNewDifferentSlugAndDifferentIncrement() {
        com.itexpert.content.lib.models.Node node = new com.itexpert.content.lib.models.Node();
        node.setSlug("mySlug-1");
        node.setId(UUID.randomUUID());
        node.setCode("NODE-CODE");


        com.itexpert.content.lib.entities.Node node0 = new com.itexpert.content.lib.entities.Node();
        node0.setSlug("mySlug-1");
        node0.setId(UUID.randomUUID());
        node0.setCode("XXXXX");

        when(contentNodeRepository.findBySlug("mySlug-1")).thenReturn(Flux.empty());
        when(nodeRepository.findBySlug("mySlug-1")).thenReturn(Flux.just(node0));

        when(contentNodeRepository.findBySlug("mySlug-2")).thenReturn(Flux.empty());
        when(nodeRepository.findBySlug("mySlug-2")).thenReturn(Flux.empty());

        when(nodeRepository.findBySlugAndCode(any(), any())).thenReturn(Flux.empty());

        StepVerifier.create(nodeSlugHelper.update(node))
                .assertNext(n -> {
                    assert n.getSlug().equals("mySlug-2");
                })
                .verifyComplete();
    }
}