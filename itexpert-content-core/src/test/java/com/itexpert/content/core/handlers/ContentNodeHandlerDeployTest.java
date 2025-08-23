package com.itexpert.content.core.handlers;

import com.itexpert.content.core.helpers.ContentHelper;
import com.itexpert.content.core.helpers.ContentNodeSlugHelper;
import com.itexpert.content.core.mappers.ContentNodeMapper;
import com.itexpert.content.core.mappers.NodeMapper;
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
import static org.mockito.Mockito.*;

public class ContentNodeHandlerDeployTest {

    private ContentNodeRepository contentNodeRepository;
    private ContentNodeMapper contentNodeMapper;
    private NodeMapper nodeMapper;

    private NodeRepository nodeRepository;

    private UserHandler userHandler;
    private NotificationHandler notificationHandler;
    private DataHandler dataHandler;
    private ContentHelper contentHelper;

    private ContentNodeHandler contentNodeHandler;

    private ContentNodeSlugHelper contentNodeSlugHelper;

    private SlugHandler slugHandler;

    private com.itexpert.content.lib.models.ContentNode snapshotNode;
    private ContentNode snapshotEntity;

    @BeforeEach
    void setup() {
        contentNodeRepository = mock(ContentNodeRepository.class);
        contentNodeMapper = Mappers.getMapper(ContentNodeMapper.class);
        nodeMapper = Mappers.getMapper(NodeMapper.class);
        nodeRepository = mock(NodeRepository.class);
        userHandler = mock(UserHandler.class);
        notificationHandler = mock(NotificationHandler.class);
        dataHandler = mock(DataHandler.class);
        contentHelper = mock(ContentHelper.class);
        slugHandler = new SlugHandler(nodeRepository, contentNodeRepository);
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
    void deployContent_withExistingContent_shouldDeployAndArchiveOld() {
        when(contentNodeRepository.findByCodeAndStatus("NODE-DEV", StatusEnum.SNAPSHOT.name()))
                .thenReturn(Mono.just(snapshotEntity));

        when(contentNodeRepository.findByCodeAndStatus("NODE-PROD", StatusEnum.SNAPSHOT.name()))
                .thenReturn(Mono.just(snapshotEntity));

        when(contentNodeRepository.save(any(ContentNode.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        contentNodeSlugHelper = mock(ContentNodeSlugHelper.class);

        this.updateConfiguration(contentNodeSlugHelper);

        when(contentNodeSlugHelper.update(any()))
                .thenAnswer(new Answer<Mono<com.itexpert.content.lib.models.ContentNode>>() {
                    @Override
                    public Mono<com.itexpert.content.lib.models.ContentNode> answer(InvocationOnMock invocationOnMock) {
                        com.itexpert.content.lib.models.ContentNode content = invocationOnMock.getArgument(0);
                        content.setSlug("my-beautifull-slug-prod");
                        return Mono.just(content);
                    }
                });

        StepVerifier.create(contentNodeHandler.deployContent("NODE-DEV", "PROD"))
                .assertNext(result -> {
                    assert result.getCode().equals("NODE-PROD");
                    assert result.getVersion().equals("2");
                    assert result.getStatus() == StatusEnum.SNAPSHOT;
                })
                .verifyComplete();

        verify(contentNodeRepository, times(2)).save(any(ContentNode.class));
    }

    @Test
    void deployContent_withoutExistingSecondSearch_shouldCreateSnapshotV0() {
        when(contentNodeRepository.findByCodeAndStatus("NODE-DEV", StatusEnum.SNAPSHOT.name()))
                .thenReturn(Mono.just(snapshotEntity));

        when(contentNodeRepository.findByCodeAndStatus("NODE-PROD", StatusEnum.SNAPSHOT.name()))
                .thenReturn(Mono.empty());

        when(contentNodeRepository.save(any(ContentNode.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        contentNodeSlugHelper = mock(ContentNodeSlugHelper.class);

        this.updateConfiguration(contentNodeSlugHelper);

        when(contentNodeSlugHelper.update(any()))
                .thenAnswer(new Answer<Mono<com.itexpert.content.lib.models.ContentNode>>() {
                    @Override
                    public Mono<com.itexpert.content.lib.models.ContentNode> answer(InvocationOnMock invocationOnMock) {
                        com.itexpert.content.lib.models.ContentNode content = invocationOnMock.getArgument(0);
                        content.setSlug("my-beautifull-slug-prod");
                        return Mono.just(content);
                    }
                });

        StepVerifier.create(contentNodeHandler.deployContent("NODE-DEV", "PROD"))
                .assertNext(result -> {
                    assert result.getCode().equals("NODE-PROD");
                    assert result.getVersion().equals("0");
                    assert result.getStatus() == StatusEnum.SNAPSHOT;
                })
                .verifyComplete();

        verify(contentNodeRepository, times(1)).save(any(ContentNode.class));
    }

    @Test
    void deployContentWithSlug_withExistingContentWithoutSlug_shouldDeployAndArchiveOldAndSetNewSlug() {
        when(contentNodeRepository.findByCodeAndStatus("NODE-DEV", StatusEnum.SNAPSHOT.name()))
                .thenReturn(Mono.just(snapshotEntity));


        ContentNode snapshotEntityProd = this.cloneNode(snapshotEntity);
        snapshotEntityProd.setSlug(null);

        when(contentNodeRepository.findByCodeAndStatus("NODE-PROD", StatusEnum.SNAPSHOT.name()))
                .thenReturn(Mono.just(snapshotEntityProd));

        when(contentNodeRepository.save(any(ContentNode.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        contentNodeSlugHelper = mock(ContentNodeSlugHelper.class);

        this.updateConfiguration(contentNodeSlugHelper);

        when(contentNodeSlugHelper.update(any()))
                .thenAnswer(new Answer<Mono<com.itexpert.content.lib.models.ContentNode>>() {
                    @Override
                    public Mono<com.itexpert.content.lib.models.ContentNode> answer(InvocationOnMock invocationOnMock) {
                        com.itexpert.content.lib.models.ContentNode content = invocationOnMock.getArgument(0);
                        content.setSlug("my-beautifull-slug-prod");
                        return Mono.just(content);
                    }
                });

        StepVerifier.create(contentNodeHandler.deployContent("NODE-DEV", "PROD"))
                .assertNext(result -> {
                    assert result.getCode().equals("NODE-PROD");
                    assert result.getVersion().equals("2");
                    assert result.getStatus() == StatusEnum.SNAPSHOT;
                    assert !result.getSlug().equals(snapshotNode.getSlug());
                    assert result.getSlug().equals("my-beautifull-slug-prod");
                })
                .verifyComplete();


        verify(contentNodeRepository, times(2)).save(any(ContentNode.class));
    }

    @Test
    void deployContentWithSlug_withExistingContentWithSlug_shouldDeployAndArchiveOldAndKeepSlug() {
        when(contentNodeRepository.findByCodeAndStatus("NODE-DEV", StatusEnum.SNAPSHOT.name()))
                .thenReturn(Mono.just(snapshotEntity));


        ContentNode snapshotEntityProd = this.cloneNode(snapshotEntity);
        snapshotEntityProd.setSlug("my-slug");
        snapshotEntityProd.setCode("NODE-PROD");

        when(contentNodeRepository.findByCodeAndStatus("NODE-PROD", StatusEnum.SNAPSHOT.name()))
                .thenReturn(Mono.just(snapshotEntityProd));

        when(contentNodeRepository.findBySlug("my-slug")).thenReturn(Flux.fromIterable(List.of(snapshotEntityProd)));
        when(nodeRepository.findBySlug("my-slug")).thenReturn(Flux.empty());


        when(contentNodeRepository.save(any(ContentNode.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));


        contentNodeSlugHelper = new ContentNodeSlugHelper(contentNodeRepository, contentNodeMapper, nodeRepository, slugHandler);

        this.updateConfiguration(contentNodeSlugHelper);

        StepVerifier.create(contentNodeHandler.deployContent("NODE-DEV", "PROD"))
                .assertNext(result -> {
                    assert result.getCode().equals("NODE-PROD");
                    assert result.getVersion().equals("2");
                    assert result.getStatus() == StatusEnum.SNAPSHOT;
                    assert result.getSlug().equals("my-slug"); //no change slug already exist
                })
                .verifyComplete();


        verify(contentNodeRepository, times(2)).save(any(ContentNode.class));
    }

    @Test
    void deployContentWithSlug_withNoExistingContent_shouldDeployAndAddNewSlug() {
        when(contentNodeRepository.findByCodeAndStatus("NODE-DEV", StatusEnum.SNAPSHOT.name()))
                .thenReturn(Mono.just(snapshotEntity));


        when(contentNodeRepository.findByCodeAndStatus("NODE-PROD", StatusEnum.SNAPSHOT.name()))
                .thenReturn(Mono.empty());

        when(contentNodeRepository.save(any(ContentNode.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        contentNodeSlugHelper = mock(ContentNodeSlugHelper.class);

        this.updateConfiguration(contentNodeSlugHelper);

        when(contentNodeSlugHelper.update(any()))
                .thenAnswer(new Answer<Mono<com.itexpert.content.lib.models.ContentNode>>() {
                    @Override
                    public Mono<com.itexpert.content.lib.models.ContentNode> answer(InvocationOnMock invocationOnMock) {
                        com.itexpert.content.lib.models.ContentNode content = invocationOnMock.getArgument(0);
                        content.setSlug("my-beautifull-slug-prod");
                        return Mono.just(content);
                    }
                });

        StepVerifier.create(contentNodeHandler.deployContent("NODE-DEV", "PROD"))
                .assertNext(result -> {
                    assert result.getCode().equals("NODE-PROD");
                    assert result.getVersion().equals("0");
                    assert result.getStatus() == StatusEnum.SNAPSHOT;
                    assert !result.getSlug().equals(snapshotNode.getSlug());
                    assert result.getSlug().equals("my-beautifull-slug-prod");
                })
                .verifyComplete();


        verify(contentNodeRepository, times(1)).save(any(ContentNode.class));
    }


    @Test
    void deployContent_withNoExistingContent_shouldDeployAndAddNewCode() {
        when(contentNodeRepository.findByCodeAndStatus("NODE-DEV", StatusEnum.SNAPSHOT.name()))
                .thenReturn(Mono.just(snapshotEntity));

        when(contentNodeRepository.findByCodeAndStatus("NODE-PROD", StatusEnum.SNAPSHOT.name()))
                .thenReturn(Mono.empty());

        when(contentNodeRepository.save(any(ContentNode.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        contentNodeSlugHelper = mock(ContentNodeSlugHelper.class);

        this.updateConfiguration(contentNodeSlugHelper);

        when(contentNodeSlugHelper.update(any()))
                .thenAnswer(new Answer<Mono<com.itexpert.content.lib.models.ContentNode>>() {
                    @Override
                    public Mono<com.itexpert.content.lib.models.ContentNode> answer(InvocationOnMock invocationOnMock) {
                        com.itexpert.content.lib.models.ContentNode content = invocationOnMock.getArgument(0);
                        content.setSlug("my-beautifull-slug-prod");
                        return Mono.just(content);
                    }
                });

        StepVerifier.create(contentNodeHandler.deployContent("NODE-DEV", "PROD"))
                .assertNext(result -> {
                    assert result.getCode().equals("NODE-PROD");
                    assert result.getVersion().equals("0");
                    assert result.getStatus() == StatusEnum.SNAPSHOT;
                })
                .verifyComplete();

        verify(contentNodeRepository, times(1)).save(any(ContentNode.class));
    }

    @Test
    void deployContent_withExistingContent_shouldDeployAndKeepCode() {
        when(contentNodeRepository.findByCodeAndStatus("NODE-DEV", StatusEnum.SNAPSHOT.name()))
                .thenReturn(Mono.just(snapshotEntity));

        ContentNode snapshotEntityProd = this.cloneNode(snapshotEntity);

        when(contentNodeRepository.findByCodeAndStatus("NODE-PROD", StatusEnum.SNAPSHOT.name()))
                .thenReturn(Mono.just(snapshotEntityProd));

        when(contentNodeRepository.save(any(ContentNode.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        contentNodeSlugHelper = mock(ContentNodeSlugHelper.class);

        this.updateConfiguration(contentNodeSlugHelper);

        when(contentNodeSlugHelper.update(any()))
                .thenAnswer(new Answer<Mono<com.itexpert.content.lib.models.ContentNode>>() {
                    @Override
                    public Mono<com.itexpert.content.lib.models.ContentNode> answer(InvocationOnMock invocationOnMock) {
                        com.itexpert.content.lib.models.ContentNode content = invocationOnMock.getArgument(0);
                        content.setSlug("my-beautifull-slug-prod");
                        return Mono.just(content);
                    }
                });

        StepVerifier.create(contentNodeHandler.deployContent("NODE-DEV", "PROD-OTHER"))
                .assertNext(result -> {
                    assert result.getCode().equals("NODE-PROD");
                    assert result.getVersion().equals("2");
                    assert result.getStatus() == StatusEnum.SNAPSHOT;
                })
                .verifyComplete();

        verify(contentNodeRepository, times(2)).save(any(ContentNode.class));
    }

    @Test
    void deployContentWithoutSlug_withNoExistingContentWithSlug_shouldDeployAndArchiveOldAndNoSlug() {

        ContentNode snapshotEntityDev = this.cloneNode(snapshotEntity);
        snapshotEntityDev.setSlug(null);

        when(contentNodeRepository.findByCodeAndStatus("NODE-DEV", StatusEnum.SNAPSHOT.name()))
                .thenReturn(Mono.just(snapshotEntityDev));


        ContentNode snapshotEntityProd = this.cloneNode(snapshotEntity);
        snapshotEntityProd.setSlug(null);

        when(contentNodeRepository.findByCodeAndStatus("NODE-PROD", StatusEnum.SNAPSHOT.name()))
                .thenReturn(Mono.just(snapshotEntityProd));

        when(contentNodeRepository.save(any(ContentNode.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));


        contentNodeSlugHelper = new ContentNodeSlugHelper(contentNodeRepository, contentNodeMapper, nodeRepository,slugHandler);

        this.updateConfiguration(contentNodeSlugHelper);

        when(contentNodeRepository.findAllBySlug("my-beautifull-slug-prod")).thenReturn(Flux.fromIterable(List.of(new ContentNode())));
        when(nodeRepository.findAllBySlug("my-beautifull-slug-prod")).thenReturn(Flux.empty());

        when(contentNodeRepository.findAllBySlug("my-beautifull-slug-prod1")).thenReturn(Flux.empty());
        when(nodeRepository.findAllBySlug("my-beautifull-slug-prod1")).thenReturn(Flux.empty());

        StepVerifier.create(contentNodeHandler.deployContent("NODE-DEV", "PROD"))
                .assertNext(result -> {
                    assert result.getCode().equals("NODE-PROD");
                    assert result.getVersion().equals("2");
                    assert result.getStatus() == StatusEnum.SNAPSHOT;
                    assert ObjectUtils.isEmpty(result.getSlug());
                })
                .verifyComplete();


        verify(contentNodeRepository, times(2)).save(any(ContentNode.class));
    }


    private ContentNode cloneNode(ContentNode original) {
        ContentNode clone = new ContentNode();
        clone.setId(original.getId());
        clone.setCode(original.getCode());
        clone.setParentCode(original.getParentCode());
        clone.setVersion(original.getVersion());
        clone.setStatus(original.getStatus());
        clone.setCreationDate(original.getCreationDate());
        clone.setModificationDate(original.getModificationDate());
        return clone;
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
