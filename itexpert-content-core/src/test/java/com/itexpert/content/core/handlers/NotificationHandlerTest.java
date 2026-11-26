package com.itexpert.content.core.handlers;

import com.itexpert.content.core.mappers.NotificationMapper;
import com.itexpert.content.core.repositories.NotificationRepository;
import com.itexpert.content.lib.enums.NotificationEnum;
import com.itexpert.content.lib.models.Notification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationHandlerTest {

    @Mock
    private NotificationRepository notificationRepository;

    private NotificationMapper mapper;

    private NotificationHandler notificationHandler;

    private Notification sampleModel;
    private com.itexpert.content.lib.entities.Notification sampleEntity;
    private String userId = UUID.randomUUID().toString();

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        mapper = new NotificationMapper();
        notificationHandler = new NotificationHandler(notificationRepository, mapper);

        UUID id = UUID.randomUUID();
        long now = Instant.now().toEpochMilli();

        sampleModel = Notification.builder()
                .id(id)
                .user(userId)
                .modifiedBy(userId)
                .type("ALERT")
                .typeCode("CODE")
                .typeVersion("1.0")
                .code(NotificationEnum.CREATION.name())
                .date(now)
                .description("Test notification")
                .read(false)
                .build();

        sampleEntity = new com.itexpert.content.lib.entities.Notification();
        sampleEntity.setId(id);
        sampleEntity.setUserId(userId);
        sampleEntity.setType("ALERT");
        sampleEntity.setTypeCode("CODE");
        sampleEntity.setTypeVersion("1.0");
        sampleEntity.setCode(NotificationEnum.CREATION.name());
        sampleEntity.setDate(now);
        sampleEntity.setDescription("Test notification");
        sampleEntity.setReaders(new ArrayList<>());
    }

    @Test
    void testSave_NewNotification() {
        when(notificationRepository.findById(sampleModel.getId())).thenReturn(Mono.empty());
        when(notificationRepository.save(any(com.itexpert.content.lib.entities.Notification.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(notificationHandler.save(sampleModel))
                .expectNextMatches(n -> n.getId().equals(sampleModel.getId()) && !n.isRead())
                .verifyComplete();

        verify(notificationRepository).save(any(com.itexpert.content.lib.entities.Notification.class));
    }

    @Test
    void testUnreadedByUser_WithNotifications() {
        when(notificationRepository.findUnreadByUser(eq(userId), any(Pageable.class)))
                .thenReturn(Flux.just(sampleEntity));

        StepVerifier.create(notificationHandler.unreadedByUser(userId, 0, 10))
                .expectNextMatches(n -> n.getId().equals(sampleModel.getId()) && !n.isRead())
                .verifyComplete();
    }

    @Test
    void testMarkRead_Success() {
        when(notificationRepository.findById(sampleModel.getId())).thenReturn(Mono.just(sampleEntity));
        when(notificationRepository.save(any(com.itexpert.content.lib.entities.Notification.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(notificationHandler.markRead(userId, sampleModel.getId()))
                .expectNextMatches(n -> n.getId().equals(sampleModel.getId()) && n.isRead())
                .verifyComplete();

        verify(notificationRepository).save(any(com.itexpert.content.lib.entities.Notification.class));
    }

    @Test
    void testMarkAllAsRead() {
        when(notificationRepository.findAllForUser(userId)).thenReturn(Flux.just(sampleEntity));
        when(notificationRepository.save(any(com.itexpert.content.lib.entities.Notification.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(notificationHandler.markAllAsRead(userId))
                .expectNextMatches(n -> n.getId().equals(sampleModel.getId()) && n.isRead())
                .verifyComplete();
    }

    @Test
    void testCountUnreaded() {
        when(notificationRepository.countUnreadByUser(userId)).thenReturn(Mono.just(1L));

        StepVerifier.create(notificationHandler.countUnreaded(userId))
                .expectNext(1L)
                .verifyComplete();
    }
}