package com.itexpert.content.core.handlers;

import com.itexpert.content.core.helpers.ContentHelper;
import com.itexpert.content.core.mappers.ContentNodeMapper;
import com.itexpert.content.core.repositories.ContentNodeRepository;
import com.itexpert.content.core.repositories.NodeRepository;
import com.itexpert.content.lib.entities.ContentNode;
import com.itexpert.content.lib.enums.NotificationEnum;
import com.itexpert.content.lib.enums.StatusEnum;
import com.itexpert.content.lib.models.Notification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class ContentNodeHandlerTest {

    private ContentNodeRepository contentNodeRepository;
    private ContentNodeMapper contentNodeMapper;

    private NodeRepository nodeRepository;

    private UserHandler userHandler;
    private NotificationHandler notificationHandler;
    private DataHandler dataHandler;
    private ContentHelper contentHelper;

    private ContentNodeHandler contentNodeHandler;

    @BeforeEach
    void setup() {
        contentNodeRepository = mock(ContentNodeRepository.class);
        contentNodeMapper = Mappers.getMapper(ContentNodeMapper.class);
        nodeRepository = mock(NodeRepository.class);
        userHandler = mock(UserHandler.class);
        notificationHandler = mock(NotificationHandler.class);
        dataHandler = mock(DataHandler.class);
        contentHelper = mock(ContentHelper.class);
        contentNodeHandler = new ContentNodeHandler(
                contentNodeRepository,
                contentNodeMapper,
                nodeRepository,
                userHandler,
                notificationHandler,
                dataHandler,
                contentHelper);

        Notification notification = Notification.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .type("CONTENT_NODE")
                .build();

        when(notificationHandler.create(any(), any(), any(), any(), any(), any())).thenReturn(Mono.just(notification));

    }

    @Test
    public void testImportContentNode_existingContentNode() {
        // Préparer les objets
        com.itexpert.content.lib.models.ContentNode inputNode = new com.itexpert.content.lib.models.ContentNode();
        inputNode.setCode("code123");
        inputNode.setVersion("1");

        ContentNode existingNode = new ContentNode();
        existingNode.setCode("code123");
        existingNode.setVersion("1");
        existingNode.setCreationDate(Instant.now().minusSeconds(3600).toEpochMilli());
        existingNode.setStatus(StatusEnum.SNAPSHOT);

        ContentNode archivedNode = new ContentNode();
        archivedNode.setCode(existingNode.getCode());
        archivedNode.setStatus(StatusEnum.ARCHIVE);

        ContentNode savedEntity = new ContentNode();
        savedEntity.setId(UUID.randomUUID());
        savedEntity.setCode("code123");
        savedEntity.setVersion("2");
        savedEntity.setStatus(StatusEnum.SNAPSHOT);

        // Simuler findByCodeAndStatus pour retourner un ContentNode existant
        when(contentNodeRepository.findByCodeAndStatus(eq("code123"), eq(StatusEnum.SNAPSHOT.name())))
                .thenReturn(Mono.just(existingNode));


        // Simuler save dans le repo, retourner le contenu sauvegardé (ici même objet pour simplicité)
        when(contentNodeRepository.save(any(ContentNode.class))).thenReturn(Mono.just(savedEntity));

        // Appel de la méthode à tester
        Mono<com.itexpert.content.lib.models.ContentNode> resultMono = contentNodeHandler.importContentNode(inputNode);

        // Vérifier le résultat
        StepVerifier.create(resultMono)
                .assertNext(result -> {
                    // Assertions sur le ContentNode retourné
                    assert result.getCode().equals("code123");
                    assert result.getVersion().equals("2");
                    assert result.getStatus() == StatusEnum.SNAPSHOT;
                    assert result.getId() != null;
                })
                .verifyComplete();

        // Vérifier interactions avec mocks
        verify(contentNodeRepository).findByCodeAndStatus(eq("code123"), eq(StatusEnum.SNAPSHOT.name()));
        verify(contentNodeRepository, times(2)).save(any(ContentNode.class)); // Une fois pour archive, une fois pour nouveau
        verify(notificationHandler, times(1)).create(any(), any(), any(), any(), any(), any());
    }

    @Test
    public void testImportContentNode_noExistingContentNode() {
        // Préparer l'entrée
        com.itexpert.content.lib.models.ContentNode inputNode = new com.itexpert.content.lib.models.ContentNode();
        inputNode.setCode("newCode");

        ContentNode savedEntity = new ContentNode();
        savedEntity.setCode("newCode");
        savedEntity.setId(UUID.randomUUID());
        savedEntity.setVersion("0");
        savedEntity.setStatus(StatusEnum.SNAPSHOT);

        // Simuler findByCodeAndStatus retourne vide (pas de noeud existant)
        when(contentNodeRepository.findByCodeAndStatus(eq("newCode"), eq(StatusEnum.SNAPSHOT.name())))
                .thenReturn(Mono.empty());

        // Simuler save dans repo
        when(contentNodeRepository.save(any(ContentNode.class))).thenReturn(Mono.just(savedEntity));

        // Appel de la méthode
        Mono<com.itexpert.content.lib.models.ContentNode> resultMono = contentNodeHandler.importContentNode(inputNode);

        // Vérifier
        StepVerifier.create(resultMono)
                .assertNext(result -> {
                    assert result.getCode().equals("newCode");
                    assert result.getVersion().equals("0");
                    assert result.getStatus() == StatusEnum.SNAPSHOT;
                    assert result.getId() != null;
                })
                .verifyComplete();

        // Vérifier interactions
        verify(contentNodeRepository).findByCodeAndStatus(eq("newCode"), eq(StatusEnum.SNAPSHOT.name()));
        verify(contentNodeRepository).save(any(ContentNode.class));
        verify(notificationHandler, times(1)).create(any(), any(), any(), any(), any(), any());
    }
}
