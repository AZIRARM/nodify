package com.itexpert.content.core.helpers;

import com.itexpert.content.core.mappers.NodeMapper;
import com.itexpert.content.core.repositories.ContentNodeRepository;
import com.itexpert.content.core.repositories.NodeRepository;
import com.itexpert.content.lib.entities.ContentNode;
import com.itexpert.content.lib.entities.Node;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NodeSlugHelperTest {
    private NodeRepository nodeRepository;
    private ContentNodeRepository contentNodeRepository;
    private NodeMapper nodeMapper;
    private NodeSlugHelper nodeSlugHelper;

    @BeforeEach
    void setup() {
        contentNodeRepository = mock(ContentNodeRepository.class);
        nodeRepository = mock(NodeRepository.class);
        nodeMapper = mock(NodeMapper.class); // mock mapper si besoin
        nodeSlugHelper = new NodeSlugHelper(nodeRepository, contentNodeRepository, nodeMapper);
    }


    @Test
    void testUpdate_slugDoesNotExist_setSlugDirectly() {
        com.itexpert.content.lib.models.Node node = new com.itexpert.content.lib.models.Node();
        node.setSlug("mySlug");
        node.setId(UUID.randomUUID());
        node.setCode("NODE-CODE");

        when(nodeRepository.findAllBySlug("mySlug-env")).thenReturn(Flux.empty());
        when(contentNodeRepository.findAllBySlug("mySlug-env")).thenReturn(Flux.empty());

        List<com.itexpert.content.lib.models.Node> nodes = new ArrayList<>();
        nodes.add(node);  // Directement l'objet Node

        StepVerifier.create(nodeSlugHelper.update(nodes, "env"))
                .assertNext(n -> {
                   assert n.getSlug().equals("mySlug-env");
                })
                .verifyComplete();
    }

    @Test
    void testUpdate_slugExistsInNodeRepository_incrementsSlug() {
        com.itexpert.content.lib.models.Node node = new com.itexpert.content.lib.models.Node();
        node.setSlug("mySlug");
        node.setId(UUID.randomUUID());
        node.setCode("NODE-CODE");

        when(contentNodeRepository.findAllBySlug("mySlug-env")).thenReturn(Flux.empty());
        when(nodeRepository.findAllBySlug("mySlug-env")).thenReturn(Flux.fromIterable(List.of(new Node())));

        when(contentNodeRepository.findAllBySlug("mySlug-env1")).thenReturn(Flux.empty());
        when(nodeRepository.findAllBySlug("mySlug-env1")).thenReturn(Flux.empty());

        List<com.itexpert.content.lib.models.Node> nodes = new ArrayList<>();
        nodes.add(node);  // Directement l'objet Node

        StepVerifier.create(nodeSlugHelper.update(nodes, "env"))
                .assertNext(n -> {
                    assert n.getSlug().equals("mySlug-env1");
                })
                .verifyComplete();
    }

    @Test
    void testUpdate_slugExistsInContentNodeRepository_incrementsSlug() {
        com.itexpert.content.lib.models.Node node = new com.itexpert.content.lib.models.Node();
        node.setSlug("mySlug");
        node.setId(UUID.randomUUID());
        node.setCode("NODE-CODE");

        when(contentNodeRepository.findAllBySlug("mySlug-env")).thenReturn(Flux.fromIterable(List.of(new ContentNode())));
        when(nodeRepository.findAllBySlug("mySlug-env")).thenReturn(Flux.empty());

        when(contentNodeRepository.findAllBySlug("mySlug-env1")).thenReturn(Flux.empty());
        when(nodeRepository.findAllBySlug("mySlug-env1")).thenReturn(Flux.empty());

        List<com.itexpert.content.lib.models.Node> nodes = new ArrayList<>();
        nodes.add(node);  // Directement l'objet Node

        StepVerifier.create(nodeSlugHelper.update(nodes, "env"))
                .assertNext(n -> {
                    assert n.getSlug().equals("mySlug-env1");
                })
                .verifyComplete();
    }

    @Test
    void testUpdate_slugExistsInNodeAndContentNodeRepository_incrementsSlug() {
        com.itexpert.content.lib.models.Node node = new com.itexpert.content.lib.models.Node();
        node.setSlug("mySlug");
        node.setId(UUID.randomUUID());
        node.setCode("NODE-CODE");

        when(contentNodeRepository.findAllBySlug("mySlug-env")).thenReturn(Flux.fromIterable(List.of(new ContentNode())));
        when(nodeRepository.findAllBySlug("mySlug-env")).thenReturn(Flux.fromIterable(List.of(new Node())));

        when(contentNodeRepository.findAllBySlug("mySlug-env1")).thenReturn(Flux.empty());
        when(nodeRepository.findAllBySlug("mySlug-env1")).thenReturn(Flux.empty());

        List<com.itexpert.content.lib.models.Node> nodes = new ArrayList<>();
        nodes.add(node);  // Directement l'objet Node

        StepVerifier.create(nodeSlugHelper.update(nodes, "env"))
                .assertNext(n -> {
                    assert n.getSlug().equals("mySlug-env1");
                })
                .verifyComplete();
    }
}