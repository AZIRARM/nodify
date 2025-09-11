package com.itexpert.content.core.handlers;

import com.itexpert.content.core.mappers.NotificationMapper;
import com.itexpert.content.lib.enums.NotificationEnum;
import com.itexpert.content.lib.models.Notification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveZSetOperations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationHandlerTest {

    @Mock
    private ReactiveRedisTemplate<String, String> redisTemplate;

    @Mock
    private ReactiveZSetOperations<String, String> zSetOperations;

    @Mock
    private NotificationMapper notificationMapper;

    private NotificationHandler notificationHandler;

    private Notification sampleNotification;
    private String userId = UUID.randomUUID().toString();

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);

        notificationHandler = new NotificationHandler(redisTemplate, notificationMapper);

        sampleNotification = Notification.builder()
                .id(UUID.randomUUID())
                .user(userId)
                .type("ALERT")
                .typeCode("CODE")
                .typeVersion("1.0")
                .code(NotificationEnum.CREATION.name())
                .date(Instant.now().toEpochMilli())
                .description("Test notification")
                .readers(new ArrayList<>())
                .build();
    }

    @Test
    void create_ShouldStoreNotificationInRedis() {
        String json = "json-string";

        when(notificationMapper.toJson(any(Notification.class))).thenReturn(json);
        when(zSetOperations.add(anyString(), eq(json), anyDouble())).thenReturn(Mono.just(true));

        Mono<Notification> result = notificationHandler.create(
                NotificationEnum.CREATION,
                "Test notification",
                userId,
                "ALERT",
                "CODE",
                "1.0"
        );

        StepVerifier.create(result)
                .assertNext(notification -> {
                    assert notification.getUser().equals(userId);
                    assert notification.getDescription().equals("Test notification");
                })
                .verifyComplete();

        verify(zSetOperations).add(anyString(), eq(json), anyDouble());
    }

    @Test
    void findAll_ShouldReturnNotifications() {
        String json = "json-string";

        when(zSetOperations.reverseRange(anyString(), any()))
                .thenReturn(Flux.just(json));
        when(notificationMapper.fromJson(json)).thenReturn(sampleNotification);

        Flux<Notification> result = notificationHandler.findAll(userId);

        StepVerifier.create(result)
                .assertNext(notification -> {
                    assert notification.getId().equals(sampleNotification.getId());
                    assert notification.getUser().equals(userId);
                })
                .verifyComplete();

        verify(zSetOperations).reverseRange(anyString(), any());
    }

    @Test
    void countUnreaded_ShouldReturnCorrectCount() {
        String json = "json-string";

        Notification unread = Notification.builder()
                .id(UUID.randomUUID())
                .user(userId)
                .readers(new ArrayList<>()) // non lu
                .build();

        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);

        when(zSetOperations.reverseRange(anyString(), any()))
                .thenReturn(Flux.just(json));

        when(notificationMapper.fromJson(json)).thenReturn(unread);

        Mono<Long> result = notificationHandler.countUnreaded(userId);

        StepVerifier.create(result)
                .expectNext(1L)
                .verifyComplete();

        verify(zSetOperations).reverseRange(anyString(), any());
        verify(notificationMapper).fromJson(json);
    }



    @Test
    void markRead_ShouldAddUserToReaders() {
        String json = "json-string";

        when(zSetOperations.range(anyString(), any()))
                .thenReturn(Flux.just(json));
        when(notificationMapper.fromJson(json)).thenReturn(sampleNotification);
        when(notificationMapper.toJson(any(Notification.class))).thenReturn(json);
        when(zSetOperations.add(anyString(), eq(json), anyDouble())).thenReturn(Mono.just(true));

        Mono<Notification> result = notificationHandler.markRead(userId, sampleNotification.getId());

        StepVerifier.create(result)
                .assertNext(notification -> {
                    assert notification.getReaders().contains(UUID.fromString(userId));
                })
                .verifyComplete();
    }
}
