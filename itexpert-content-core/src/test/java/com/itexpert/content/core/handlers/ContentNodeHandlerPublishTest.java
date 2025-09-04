package com.itexpert.content.core.handlers;

import com.itexpert.content.core.helpers.ContentHelper;
import com.itexpert.content.core.helpers.ContentNodeSlugHelper;
import com.itexpert.content.core.mappers.ContentNodeMapper;
import com.itexpert.content.core.repositories.ContentNodeRepository;
import com.itexpert.content.core.repositories.NodeRepository;
import com.itexpert.content.lib.entities.ContentNode;
import com.itexpert.content.lib.enums.StatusEnum;
import com.itexpert.content.lib.models.Notification;
import com.itexpert.content.lib.models.UserPost;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ContentNodeHandlerPublishTest {

    private ContentNodeRepository contentNodeRepository;
    private ContentNodeMapper contentNodeMapper;

    private NodeRepository nodeRepository;

    private UserHandler userHandler;
    private NotificationHandler notificationHandler;
    private DataHandler dataHandler;
    private ContentHelper contentHelper;

    private ContentNodeHandler contentNodeHandler;

    private ContentNodeSlugHelper contentNodeSlugHelper;


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
                contentHelper,
                contentNodeSlugHelper);

        Notification notification = Notification.builder()
                .id(UUID.randomUUID())
                .user("Admin")
                .type("CONTENT_NODE")
                .build();

        when(userHandler.findById(any())).thenReturn(Mono.just(mock(UserPost.class)));

        when(contentNodeRepository.save(any(ContentNode.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        when(notificationHandler.create(any(), any(), any(), any(), any(), any())).thenReturn(Mono.just(notification));
    }

    @Test
    void publish_shouldCallCreateFirstPublication_whenNoPublishedNodeExists() {
        // GIVEN
        UUID contentNodeId = UUID.randomUUID();
        ContentNode snapshotNode = new ContentNode();
        snapshotNode.setId(contentNodeId);
        snapshotNode.setCode("NEW-NODE-CODE");
        snapshotNode.setVersion("0");
        snapshotNode.setStatus(StatusEnum.SNAPSHOT);

        when(contentNodeRepository.findById(contentNodeId)).thenReturn(Mono.just(snapshotNode));
        when(contentNodeRepository.findByIdAndStatus(contentNodeId, StatusEnum.SNAPSHOT)).thenReturn(Mono.just(snapshotNode));
        when(contentNodeRepository.findByCodeAndStatus(snapshotNode.getCode(), StatusEnum.PUBLISHED.name())).thenReturn(Mono.empty());
        when(contentNodeRepository.findByCodeAndStatus(snapshotNode.getCode(), StatusEnum.SNAPSHOT.name())).thenReturn(Mono.just(snapshotNode));


        StepVerifier.create(contentNodeHandler.publish(snapshotNode.getCode(), true, "Admin"))
                .assertNext(contentNode -> {
                    assert contentNode.getStatus().equals(StatusEnum.PUBLISHED);
                    assert contentNode.getId().equals(snapshotNode.getId());
                })
                .verifyComplete();

        verify(contentNodeRepository, times(2)).save(any(ContentNode.class));
        verify(notificationHandler, times(1)).create(any(), any(), any(), any(), any(), any());
    }

    @Test
    void publish_shouldArchiveExistingPublished_whenExistingPublishedContent() {
        // GIVEN
        UUID snapshotContentId = UUID.randomUUID();
        ContentNode snapshotContent = new ContentNode();
        snapshotContent.setId(snapshotContentId);
        snapshotContent.setCode("CONTENT-CODE");
        snapshotContent.setVersion("1");
        snapshotContent.setStatus(StatusEnum.SNAPSHOT);

        UUID publishedContentId = UUID.randomUUID();
        ContentNode publishedContent = new ContentNode();
        publishedContent.setId(publishedContentId);
        publishedContent.setCode("CONTENT-CODE");
        publishedContent.setVersion("0");
        publishedContent.setStatus(StatusEnum.PUBLISHED);

        // Mocks
        when(contentNodeRepository.findById(snapshotContentId)).thenReturn(Mono.just(snapshotContent));

        when(contentNodeRepository.findByCodeAndStatus(snapshotContent.getCode(), StatusEnum.PUBLISHED.name()))
                .thenReturn(Mono.just(publishedContent));

        when(contentNodeRepository.findByCodeAndStatus(snapshotContent.getCode(), StatusEnum.SNAPSHOT.name()))
                .thenReturn(Mono.just(publishedContent));


        when(contentNodeRepository.save(any(ContentNode.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        // WHEN
        StepVerifier.create(contentNodeHandler.publish(snapshotContent.getCode(), true, "Admin"))
                .assertNext(contentNode -> {
                    assert contentNode.getStatus().equals(StatusEnum.PUBLISHED);
                    assert !contentNode.getId().equals(snapshotContent.getId());
                })
                .verifyComplete();

        // THEN
        // 2 saves principaux : archiver l'ancien + publier le snapshot
        verify(contentNodeRepository, times(3)).save(any(ContentNode.class));
        verify(notificationHandler, times(1)).create(any(), any(), any(), any(), any(), any());
    }

}
