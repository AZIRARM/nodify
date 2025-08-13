package com.itexpert.content.core.helpers;

import com.itexpert.content.core.repositories.ContentNodeRepository;
import com.itexpert.content.core.repositories.NodeRepository;
import com.itexpert.content.lib.entities.Node;
import com.itexpert.content.lib.models.ContentNode;
import org.apache.commons.lang3.ObjectUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class ContentNodeSlugHelperTest {

    private ContentNodeRepository contentNodeRepository;
    private NodeRepository nodeRepository;
    private ContentNodeSlugHelper contentNodeSlugHelper;

    @BeforeEach
    void setup() {
        contentNodeRepository = mock(ContentNodeRepository.class);
        nodeRepository = mock(NodeRepository.class);
        contentNodeSlugHelper = new ContentNodeSlugHelper(contentNodeRepository, nodeRepository);
    }

    @Test
    void testUpdate_slugNotExistsAnywhere_doNothing() {
        ContentNode content = new ContentNode();

        // Aucun slug dans contentNodeRepository
        when(contentNodeRepository.findAllBySlug(anyString())).thenReturn(Flux.empty());
        // Aucun slug dans nodeRepository
        when(nodeRepository.findAllBySlug(anyString())).thenReturn(Flux.empty());

        when(contentNodeRepository.findBySlugAndCode(any(), any())).thenReturn(Flux.fromIterable(List.of(new com.itexpert.content.lib.entities.ContentNode())));

        StepVerifier.create(contentNodeSlugHelper.update(content, "env"))
                .expectNextMatches(c -> ObjectUtils.isEmpty(c.getSlug()))
                .verifyComplete();

        verify(contentNodeRepository, times(0)).findAllBySlug(any());
        verify(nodeRepository, times(0)).findAllBySlug(any());

    }

    @Test
    void testUpdate_slugExistsInContentNodeRepository_incrementSlug() {
        ContentNode content = new ContentNode();
        content.setSlug("mySlug");

        // Slug existe dans contentNodeRepository à la 1ère vérif
        when(contentNodeRepository.findAllBySlug("mySlug-env")).thenReturn(Flux.just(new com.itexpert.content.lib.entities.ContentNode()));

        // Slug "mySlug-env1" n'existe pas dans contentNodeRepository
        when(contentNodeRepository.findAllBySlug("mySlug-env1")).thenReturn(Flux.empty());
        // Slug "mySlug-env1" n'existe pas dans nodeRepository
        when(nodeRepository.findAllBySlug("mySlug-env1")).thenReturn(Flux.empty());

        when(contentNodeRepository.findBySlugAndCode(any(), any())).thenReturn(Flux.empty());

        StepVerifier.create(contentNodeSlugHelper.update(content, "env"))
                .expectNextMatches(c -> c.getSlug().equals("mySlug-env1"))
                .verifyComplete();

        verify(contentNodeRepository).findAllBySlug("mySlug-env");
        verify(contentNodeRepository).findAllBySlug("mySlug-env1");
        verify(nodeRepository).findAllBySlug("mySlug-env1");
    }

    @Test
    void testUpdate_slugExistsInNodeRepository_incrementSlug() {
        ContentNode content = new ContentNode();
        content.setSlug("mySlug");

        // Pas dans contentNodeRepository
        when(contentNodeRepository.findAllBySlug("mySlug-env")).thenReturn(Flux.empty());
        // Slug existe dans nodeRepository
        when(nodeRepository.findAllBySlug("mySlug-env")).thenReturn(Flux.just(new Node()));
        // Slug "mySlug-env1" n'existe pas dans contentNodeRepository
        when(contentNodeRepository.findAllBySlug("mySlug-env1")).thenReturn(Flux.empty());
        // Slug "mySlug-env1" n'existe pas dans nodeRepository
        when(nodeRepository.findAllBySlug("mySlug-env1")).thenReturn(Flux.empty());

        when(contentNodeRepository.findBySlugAndCode(any(), any())).thenReturn(Flux.empty());

        StepVerifier.create(contentNodeSlugHelper.update(content, "env"))
                .expectNextMatches(c -> c.getSlug().equals("mySlug-env1"))
                .verifyComplete();

        verify(contentNodeRepository).findAllBySlug("mySlug-env");
        verify(nodeRepository).findAllBySlug("mySlug-env");
        verify(contentNodeRepository).findAllBySlug("mySlug-env1");
        verify(nodeRepository).findAllBySlug("mySlug-env1");
    }
}