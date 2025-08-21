package com.itexpert.content.core.helpers;

import com.itexpert.content.core.handlers.SlugHandler;
import com.itexpert.content.core.mappers.ContentNodeMapper;
import com.itexpert.content.core.repositories.ContentNodeRepository;
import com.itexpert.content.core.repositories.NodeRepository;
import com.itexpert.content.lib.entities.Node;
import com.itexpert.content.lib.enums.StatusEnum;
import com.itexpert.content.lib.models.ContentNode;
import org.apache.commons.lang3.ObjectUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class ContentNodeSlugHelperTest {

    private ContentNodeRepository contentNodeRepository;
    private NodeRepository nodeRepository;
    private ContentNodeSlugHelper contentNodeSlugHelper;
    private ContentNodeMapper contentNodeMapper;
    private SlugHandler slugHandler;

    @BeforeEach
    void setup() {
        contentNodeRepository = mock(ContentNodeRepository.class);
        nodeRepository = mock(NodeRepository.class);
        contentNodeMapper = Mappers.getMapper(ContentNodeMapper.class);
        slugHandler = new SlugHandler(nodeRepository, contentNodeRepository);

        contentNodeSlugHelper = new ContentNodeSlugHelper(contentNodeRepository, contentNodeMapper, nodeRepository, slugHandler);
    }

    @Test
    void testUpdate_slugNotExistsAnywhere_doNothing() {
        ContentNode content = new ContentNode();

        com.itexpert.content.lib.entities.ContentNode contentNodeEntity = new com.itexpert.content.lib.entities.ContentNode();
        contentNodeEntity.setId(UUID.randomUUID());
        contentNodeEntity.setCode(content.getCode());


        when(contentNodeRepository.findByCodeAndStatus(content.getCode(), StatusEnum.SNAPSHOT.name())).thenReturn(Mono.just(contentNodeEntity));

        // Aucun slug dans contentNodeRepository
        when(contentNodeRepository.findAllBySlug(anyString())).thenReturn(Flux.empty());
        // Aucun slug dans nodeRepository
        when(nodeRepository.findAllBySlug(anyString())).thenReturn(Flux.empty());

        when(contentNodeRepository.findBySlugAndCode(any(), any())).thenReturn(Flux.fromIterable(List.of(new com.itexpert.content.lib.entities.ContentNode())));

        StepVerifier.create(contentNodeSlugHelper.update(content))
                .expectNextMatches(c -> ObjectUtils.isEmpty(c.getSlug()))
                .verifyComplete();

        verify(contentNodeRepository, times(0)).findAllBySlug(any());
        verify(nodeRepository, times(0)).findAllBySlug(any());

    }

    @Test
    void testUpdate_slugExistsInContentNodeRepository_incrementSlug() {
        ContentNode content = new ContentNode();
        content.setSlug("mySlug");

        com.itexpert.content.lib.entities.ContentNode contentNodeEntity = new com.itexpert.content.lib.entities.ContentNode();
        contentNodeEntity.setId(UUID.randomUUID());
        contentNodeEntity.setCode(content.getCode());


        when(contentNodeRepository.findByCodeAndStatus(content.getCode(), StatusEnum.SNAPSHOT.name())).thenReturn(Mono.just(contentNodeEntity));

        // Slug existe dans contentNodeRepository à la 1ère vérif
        when(contentNodeRepository.findAllBySlug("mySlug")).thenReturn(Flux.just(new com.itexpert.content.lib.entities.ContentNode()));

        // Slug "mySlug-env1" n'existe pas dans contentNodeRepository
        when(contentNodeRepository.findAllBySlug("mySlug-0")).thenReturn(Flux.empty());
        // Slug "mySlug-env1" n'existe pas dans nodeRepository
        when(nodeRepository.findAllBySlug("mySlug-0")).thenReturn(Flux.empty());

        when(contentNodeRepository.findBySlugAndCode(any(), any())).thenReturn(Flux.empty());

        StepVerifier.create(contentNodeSlugHelper.update(content))
                .expectNextMatches(c -> c.getSlug().equals("mySlug-0"))
                .verifyComplete();

        verify(contentNodeRepository).findAllBySlug("mySlug-0");
        verify(nodeRepository).findAllBySlug("mySlug-0");
    }

    @Test
    void testUpdate_slugExistsInNodeRepository_incrementSlug() {
        ContentNode content = new ContentNode();
        content.setSlug("mySlug");

        com.itexpert.content.lib.entities.ContentNode contentNodeEntity = new com.itexpert.content.lib.entities.ContentNode();
        contentNodeEntity.setId(UUID.randomUUID());
        contentNodeEntity.setCode(content.getCode());


        when(contentNodeRepository.findByCodeAndStatus(content.getCode(), StatusEnum.SNAPSHOT.name())).thenReturn(Mono.just(contentNodeEntity));

        // Pas dans contentNodeRepository
        when(contentNodeRepository.findAllBySlug("mySlug-0")).thenReturn(Flux.empty());
        // Slug existe dans nodeRepository
        when(nodeRepository.findAllBySlug("mySlug-0")).thenReturn(Flux.just(new Node()));
        // Slug "mySlug-env1" n'existe pas dans contentNodeRepository
        when(contentNodeRepository.findAllBySlug("mySlug-1")).thenReturn(Flux.empty());
        // Slug "mySlug-env1" n'existe pas dans nodeRepository
        when(nodeRepository.findAllBySlug("mySlug-1")).thenReturn(Flux.empty());

        when(contentNodeRepository.findBySlugAndCode(any(), any())).thenReturn(Flux.empty());

        StepVerifier.create(contentNodeSlugHelper.update(content))
                .expectNextMatches(c -> c.getSlug().equals("mySlug-1"))
                .verifyComplete();

        verify(contentNodeRepository).findAllBySlug("mySlug-0");
        verify(nodeRepository).findAllBySlug("mySlug-0");
        verify(contentNodeRepository).findAllBySlug("mySlug-1");
        verify(nodeRepository).findAllBySlug("mySlug-1");
    }



    @Test
    void testUpdate_slugExistForOtherNodeOrContentAndIncrementExist_shouldHaveNewDifferentSlugAndDifferentIncrement() {
        com.itexpert.content.lib.models.ContentNode contentNode = new com.itexpert.content.lib.models.ContentNode();
        contentNode.setSlug("mySlug-3");
        contentNode.setId(UUID.randomUUID());
        contentNode.setCode("NODE-CODE");


        com.itexpert.content.lib.entities.ContentNode contentNodeEntity = new com.itexpert.content.lib.entities.ContentNode();
        contentNodeEntity.setId(UUID.randomUUID());
        contentNodeEntity.setCode(contentNode.getCode());


        when(contentNodeRepository.findByCodeAndStatus(contentNode.getCode(), StatusEnum.SNAPSHOT.name())).thenReturn(Mono.just(contentNodeEntity));


        when(contentNodeRepository.findAllBySlug("mySlug-0")).thenReturn(Flux.fromIterable(List.of(new com.itexpert.content.lib.entities.ContentNode())));
        when(nodeRepository.findAllBySlug("mySlug-0")).thenReturn(Flux.empty());

        when(contentNodeRepository.findAllBySlug("mySlug-1")).thenReturn(Flux.empty());
        when(nodeRepository.findAllBySlug("mySlug-1")).thenReturn(Flux.empty());

        when(contentNodeRepository.findBySlugAndCode(any(), any())).thenReturn(Flux.empty());

        StepVerifier.create(contentNodeSlugHelper.update(contentNode))
                .assertNext(n -> {
                    assert n.getSlug().equals("mySlug-1");
                })
                .verifyComplete();
    }

    @Test
    void testUpdate_slugExistForOtherNodeOrContentAndIncrementExistButNotDisponible_shouldHaveNewDifferentSlugAndDifferentIncrement() {
        ContentNode contentNode = new ContentNode();
        contentNode.setSlug("mySlug-1");
        contentNode.setId(UUID.randomUUID());
        contentNode.setCode("NODE-CODE");

        com.itexpert.content.lib.entities.ContentNode contentNodeEntity = new com.itexpert.content.lib.entities.ContentNode();
        contentNodeEntity.setId(UUID.randomUUID());
        contentNodeEntity.setCode(contentNode.getCode());


        when(contentNodeRepository.findByCodeAndStatus(contentNode.getCode(), StatusEnum.SNAPSHOT.name())).thenReturn(Mono.just(contentNodeEntity));

        when(contentNodeRepository.findAllBySlug("mySlug-0")).thenReturn(Flux.fromIterable(List.of(new com.itexpert.content.lib.entities.ContentNode())));
        when(nodeRepository.findAllBySlug("mySlug-0")).thenReturn(Flux.empty());

        when(contentNodeRepository.findAllBySlug("mySlug-1")).thenReturn(Flux.fromIterable(List.of(new com.itexpert.content.lib.entities.ContentNode())));
        when(nodeRepository.findAllBySlug("mySlug-1")).thenReturn(Flux.empty());

        when(contentNodeRepository.findAllBySlug("mySlug-2")).thenReturn(Flux.empty());
        when(nodeRepository.findAllBySlug("mySlug-2")).thenReturn(Flux.empty());

        when(contentNodeRepository.findBySlugAndCode(any(), any())).thenReturn(Flux.empty());

        StepVerifier.create(contentNodeSlugHelper.update(contentNode))
                .assertNext(n -> {
                    assert n.getSlug().equals("mySlug-2");
                })
                .verifyComplete();
    }
}