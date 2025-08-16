package com.itexpert.content.core.handlers;

import com.itexpert.content.core.helpers.ContentHelper;
import com.itexpert.content.core.helpers.ContentNodeSlugHelper;
import com.itexpert.content.core.mappers.ContentNodeMapper;
import com.itexpert.content.core.repositories.ContentNodeRepository;
import com.itexpert.content.core.repositories.NodeRepository;
import com.itexpert.content.lib.entities.ContentNode;
import com.itexpert.content.lib.enums.StatusEnum;
import com.itexpert.content.lib.models.Notification;
import org.apache.commons.lang3.ObjectUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class ContentNodeHandlerImportTest {

    private ContentNodeRepository contentNodeRepository;
    private ContentNodeMapper contentNodeMapper;

    private NodeRepository nodeRepository;

    private UserHandler userHandler;
    private NotificationHandler notificationHandler;
    private DataHandler dataHandler;
    private ContentHelper contentHelper;

    private ContentNodeHandler contentNodeHandler;

    private ContentNodeSlugHelper contentNodeSlugHelper;

    private com.itexpert.content.lib.models.ContentNode snapshotNode;
    private ContentNode snapshotEntity;

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
                .userId(UUID.randomUUID())
                .type("CONTENT_NODE")
                .build();

        snapshotNode = new com.itexpert.content.lib.models.ContentNode();
        snapshotNode.setId(UUID.randomUUID());
        snapshotNode.setCode("NODE-DEV");
        snapshotNode.setParentCode("PARENT-DEV");
        snapshotNode.setVersion("1");
        snapshotNode.setStatus(StatusEnum.SNAPSHOT);
        snapshotNode.setSlug("my-beautifull-slug");
        snapshotNode.setCreationDate(Instant.now().minusSeconds(3600).toEpochMilli());

        snapshotEntity = new ContentNode();
        snapshotEntity.setId(snapshotNode.getId());
        snapshotEntity.setCode(snapshotNode.getCode());
        snapshotEntity.setParentCode(snapshotNode.getParentCode());
        snapshotEntity.setVersion(snapshotNode.getVersion());
        snapshotEntity.setStatus(snapshotNode.getStatus());
        snapshotEntity.setSlug("my-beautifull-slug");
        snapshotEntity.setCreationDate(snapshotNode.getCreationDate());

        when(notificationHandler.create(any(), any(), any(), any(), any(), any())).thenReturn(Mono.just(notification));
    }

    @Test
    public void testImportContentNode_existingContentNode() {
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

        when(contentNodeRepository.findByCodeAndStatus(eq("code123"), eq(StatusEnum.SNAPSHOT.name())))
                .thenReturn(Mono.just(existingNode));


        when(contentNodeRepository.save(any(ContentNode.class))).thenReturn(Mono.just(savedEntity));

        contentNodeSlugHelper = mock(ContentNodeSlugHelper.class);

        this.updateConfiguration(contentNodeSlugHelper);

        when(contentNodeSlugHelper.update(any(), any()))
                .thenAnswer(new Answer<Mono<com.itexpert.content.lib.models.ContentNode>>() {
                    @Override
                    public Mono<com.itexpert.content.lib.models.ContentNode> answer(InvocationOnMock invocationOnMock) {
                        com.itexpert.content.lib.models.ContentNode content = invocationOnMock.getArgument(0);
                        content.setSlug("my-beautifull-slug-prod");
                        return Mono.just(content);
                    }
                });

        Mono<com.itexpert.content.lib.models.ContentNode> resultMono = contentNodeHandler.importContentNode(inputNode);

        StepVerifier.create(resultMono)
                .assertNext(result -> {
                    assert result.getCode().equals("code123");
                    assert result.getVersion().equals("2");
                    assert result.getStatus() == StatusEnum.SNAPSHOT;
                    assert result.getId() != null;
                })
                .verifyComplete();

        verify(contentNodeRepository).findByCodeAndStatus(eq("code123"), eq(StatusEnum.SNAPSHOT.name()));
        verify(contentNodeRepository, times(2)).save(any(ContentNode.class));
        verify(notificationHandler, times(1)).create(any(), any(), any(), any(), any(), any());
    }

    @Test
    public void testImportContentNode_noExistingContentNode() {
        com.itexpert.content.lib.models.ContentNode inputNode = new com.itexpert.content.lib.models.ContentNode();
        inputNode.setCode("newCode");

        ContentNode savedEntity = new ContentNode();
        savedEntity.setCode("newCode");
        savedEntity.setId(UUID.randomUUID());
        savedEntity.setVersion("0");
        savedEntity.setStatus(StatusEnum.SNAPSHOT);

        when(contentNodeRepository.findByCodeAndStatus(eq("newCode"), eq(StatusEnum.SNAPSHOT.name())))
                .thenReturn(Mono.empty());

        when(contentNodeRepository.save(any(ContentNode.class))).thenReturn(Mono.just(savedEntity));

        contentNodeSlugHelper = mock(ContentNodeSlugHelper.class);

        this.updateConfiguration(contentNodeSlugHelper);

        when(contentNodeSlugHelper.update(any(), any()))
                .thenAnswer(new Answer<Mono<com.itexpert.content.lib.models.ContentNode>>() {
                    @Override
                    public Mono<com.itexpert.content.lib.models.ContentNode> answer(InvocationOnMock invocationOnMock) {
                        com.itexpert.content.lib.models.ContentNode content = invocationOnMock.getArgument(0);
                        content.setSlug("my-beautifull-slug-prod");
                        return Mono.just(content);
                    }
                });

        Mono<com.itexpert.content.lib.models.ContentNode> resultMono = contentNodeHandler.importContentNode(inputNode);

        StepVerifier.create(resultMono)
                .assertNext(result -> {
                    assert result.getCode().equals("newCode");
                    assert result.getVersion().equals("0");
                    assert result.getStatus() == StatusEnum.SNAPSHOT;
                    assert result.getId() != null;
                })
                .verifyComplete();

        verify(contentNodeRepository).findByCodeAndStatus(eq("newCode"), eq(StatusEnum.SNAPSHOT.name()));
        verify(contentNodeRepository).save(any(ContentNode.class));
        verify(notificationHandler, times(1)).create(any(), any(), any(), any(), any(), any());
    }

    private void updateConfiguration(ContentNodeSlugHelper contentNodeSlugHelperParam) {
        contentNodeHandler = new ContentNodeHandler(
                contentNodeRepository,
                contentNodeMapper,
                nodeRepository,
                userHandler,
                notificationHandler,
                dataHandler,
                contentHelper,
                contentNodeSlugHelperParam);
    }
}
