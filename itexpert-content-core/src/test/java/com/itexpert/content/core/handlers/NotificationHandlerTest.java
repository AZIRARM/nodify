package com.itexpert.content.core.handlers;

import com.itexpert.content.core.mappers.NotificationMapper;
import com.itexpert.content.core.repositories.NodeRepository;
import com.itexpert.content.core.repositories.UserRepository;
import com.itexpert.content.lib.enums.NotificationEnum;
import com.itexpert.content.lib.models.Notification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveZSetOperations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class NotificationHandlerTest {

    @Mock
    private ReactiveRedisTemplate<String, String> redisTemplate;

    @Mock
    private ReactiveZSetOperations<String, String> zSetOperations;

    @Mock
    private NotificationMapper notificationMapper;

    private NotificationHandler notificationHandler;

    @Mock
    private UserRepository userRepository;

    private Notification sampleNotification;
    private String userId = UUID.randomUUID().toString();
    private String sampleJson;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        userRepository = mock(UserRepository.class);

        notificationHandler = new NotificationHandler(redisTemplate, notificationMapper, userRepository);

        sampleNotification = Notification.builder()
                .id(UUID.randomUUID())
                .user(userId)
                .type("ALERT")
                .typeCode("CODE")
                .typeVersion("1.0")
                .code(NotificationEnum.CREATION.name())
                .date(Instant.now().toEpochMilli())
                .description("Test notification")
                .build();

        sampleJson = "sample-json";
        when(notificationMapper.toJson(any(Notification.class))).thenReturn(sampleJson);
        when(notificationMapper.fromJson(eq(sampleJson))).thenReturn(sampleNotification);
    }

    @Test
    void testSave_NewNotification() {
        when(zSetOperations.range(anyString(), any(Range.class))).thenReturn(Flux.empty());
        when(zSetOperations.add(anyString(), anyString(), anyDouble())).thenReturn(Mono.just(true));

        StepVerifier.create(notificationHandler.save(sampleNotification))
                .expectNextMatches(n -> n.getId().equals(sampleNotification.getId()))
                .verifyComplete();

        verify(zSetOperations).add(anyString(), eq(sampleJson), eq((double) sampleNotification.getDate()));
    }

    @Test
    void testUnreadedByUser_WithNotifications() {
        when(zSetOperations.reverseRange(anyString(), any(Range.class)))
                .thenReturn(Flux.just(sampleJson));

        StepVerifier.create(notificationHandler.unreadedByUser(userId, 0, 10))
                .expectNextMatches(n -> n.getId().equals(sampleNotification.getId()))
                .verifyComplete();
    }

    @Test
    void testMarkRead_Success() {
        // Mock de keys pour delete
        when(redisTemplate.keys(anyString())).thenReturn(Flux.just("NOTIFICATIONS:" + userId));

        // Mock de range pour retourner la notification
        when(zSetOperations.range(anyString(), any(Range.class))).thenReturn(Flux.just(sampleJson));

        // Mock de remove pour simuler la suppression
        when(zSetOperations.remove(anyString(), eq(sampleJson))).thenReturn(Mono.just(1L));

        StepVerifier.create(notificationHandler.markRead(userId, sampleNotification.getId()))
                .expectNextMatches(n -> n.getId().equals(sampleNotification.getId()))
                .verifyComplete();
    }

    @Test
    void testMarkAllAsRead() {
        when(redisTemplate.keys(anyString())).thenReturn(Flux.just("NOTIFICATIONS:" + userId));
        when(zSetOperations.reverseRange(anyString(), any(Range.class))).thenReturn(Flux.just(sampleJson));
        when(zSetOperations.range(anyString(), any(Range.class))).thenReturn(Flux.just(sampleJson));
        when(zSetOperations.remove(anyString(), eq(sampleJson))).thenReturn(Mono.just(1L));

        StepVerifier.create(notificationHandler.markAllAsRead(userId))
                .expectNextMatches(n -> n.getId().equals(sampleNotification.getId()))
                .verifyComplete();
    }

    @Test
    void testCountUnreaded() {
        when(zSetOperations.reverseRange(anyString(), any(Range.class))).thenReturn(Flux.just(sampleJson));

        StepVerifier.create(notificationHandler.countUnreaded(userId))
                .expectNext(1L)
                .verifyComplete();
    }
}
