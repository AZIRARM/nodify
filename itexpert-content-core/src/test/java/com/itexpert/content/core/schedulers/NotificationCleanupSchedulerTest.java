package com.itexpert.content.core.schedulers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveZSetOperations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationCleanupSchedulerTest {

    @Mock
    private ReactiveRedisTemplate<String, String> redisTemplate;

    @Mock
    private ReactiveZSetOperations<String, String> zSetOperations;

    @InjectMocks
    private NotificationCleanupScheduler notificationCleanupScheduler;

    /**
     * Test: Successfully cleans up notifications when user has more than
     * MAX_NOTIFICATIONS.
     */
    @Test
    void cleanupNotifications_ShouldDeleteExcessNotifications_WhenSizeExceedsLimit() {
        String key = "NOTIFICATIONS:user:123";
        Long currentSize = 1500L;
        Long toRemove = 500L; // 1500 - 1000 = 500
        Long removedCount = 500L;

        when(redisTemplate.keys("NOTIFICATIONS:*")).thenReturn(Flux.just(key));
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.size(key)).thenReturn(Mono.just(currentSize));
        when(zSetOperations.removeRange(eq(key), any(Range.class))).thenReturn(Mono.just(removedCount));

        notificationCleanupScheduler.cleanupNotifications();

        verify(redisTemplate).keys("NOTIFICATIONS:*");
        verify(zSetOperations).size(key);
        verify(zSetOperations).removeRange(eq(key), any(Range.class));
    }

    /**
     * Test: Does nothing when user has exactly MAX_NOTIFICATIONS.
     */
    @Test
    void cleanupNotifications_ShouldNotDeleteAnything_WhenSizeEqualsMax() {
        String key = "NOTIFICATIONS:user:123";
        Long currentSize = 1000L;

        when(redisTemplate.keys("NOTIFICATIONS:*")).thenReturn(Flux.just(key));
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.size(key)).thenReturn(Mono.just(currentSize));

        notificationCleanupScheduler.cleanupNotifications();

        verify(redisTemplate).keys("NOTIFICATIONS:*");
        verify(zSetOperations).size(key);
        verify(zSetOperations, never()).removeRange(anyString(), any(Range.class));
    }

    /**
     * Test: Does nothing when user has less than MAX_NOTIFICATIONS.
     */
    @Test
    void cleanupNotifications_ShouldNotDeleteAnything_WhenSizeIsLessThanMax() {
        String key = "NOTIFICATIONS:user:123";
        Long currentSize = 500L;

        when(redisTemplate.keys("NOTIFICATIONS:*")).thenReturn(Flux.just(key));
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.size(key)).thenReturn(Mono.just(currentSize));

        notificationCleanupScheduler.cleanupNotifications();

        verify(redisTemplate).keys("NOTIFICATIONS:*");
        verify(zSetOperations).size(key);
        verify(zSetOperations, never()).removeRange(anyString(), any(Range.class));
    }

    /**
     * Test: Handles multiple users/keys - deletes excess notifications for each.
     */
    @Test
    void cleanupNotifications_ShouldHandleMultipleKeys_Correctly() {
        String key1 = "NOTIFICATIONS:user:111";
        String key2 = "NOTIFICATIONS:user:222";
        String key3 = "NOTIFICATIONS:user:333";
        Long sizeKey1 = 1200L;
        Long sizeKey2 = 800L; // Below limit - should not delete
        Long sizeKey3 = 2000L;
        Long toRemoveKey1 = 200L;
        Long toRemoveKey3 = 1000L;

        when(redisTemplate.keys("NOTIFICATIONS:*")).thenReturn(Flux.just(key1, key2, key3));
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.size(key1)).thenReturn(Mono.just(sizeKey1));
        when(zSetOperations.size(key2)).thenReturn(Mono.just(sizeKey2));
        when(zSetOperations.size(key3)).thenReturn(Mono.just(sizeKey3));
        when(zSetOperations.removeRange(eq(key1), any(Range.class))).thenReturn(Mono.just(toRemoveKey1));
        when(zSetOperations.removeRange(eq(key3), any(Range.class))).thenReturn(Mono.just(toRemoveKey3));

        notificationCleanupScheduler.cleanupNotifications();

        verify(redisTemplate).keys("NOTIFICATIONS:*");
        verify(zSetOperations).size(key1);
        verify(zSetOperations).size(key2);
        verify(zSetOperations).size(key3);
        verify(zSetOperations).removeRange(eq(key1), any(Range.class));
        verify(zSetOperations, never()).removeRange(eq(key2), any(Range.class));
        verify(zSetOperations).removeRange(eq(key3), any(Range.class));
    }

    /**
     * Test: Handles null size returned from Redis (key might not exist or has no
     * members).
     */
    @Test
    void cleanupNotifications_ShouldNotDeleteAnything_WhenSizeIsNull() {
        String key = "NOTIFICATIONS:user:123";

        when(redisTemplate.keys("NOTIFICATIONS:*")).thenReturn(Flux.just(key));
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.size(key)).thenReturn(Mono.empty()); // Simulates null size

        notificationCleanupScheduler.cleanupNotifications();

        verify(redisTemplate).keys("NOTIFICATIONS:*");
        verify(zSetOperations).size(key);
        verify(zSetOperations, never()).removeRange(anyString(), any(Range.class));
    }

    /**
     * Test: Handles empty key set gracefully (no notifications to clean).
     */
    @Test
    void cleanupNotifications_ShouldDoNothing_WhenNoKeysFound() {
        when(redisTemplate.keys("NOTIFICATIONS:*")).thenReturn(Flux.empty());

        notificationCleanupScheduler.cleanupNotifications();

        verify(redisTemplate).keys("NOTIFICATIONS:*");
        verify(redisTemplate, never()).opsForZSet();
        verify(zSetOperations, never()).size(anyString());
        verify(zSetOperations, never()).removeRange(anyString(), any(Range.class));
    }

    /**
     * Test: Handles error during keys retrieval gracefully.
     */
    @Test
    void cleanupNotifications_ShouldHandleError_WhenKeysRetrievalFails() {
        RuntimeException error = new RuntimeException("Redis connection failed");

        when(redisTemplate.keys("NOTIFICATIONS:*")).thenReturn(Flux.error(error));

        // Should not throw exception (errors are handled within the subscription)
        notificationCleanupScheduler.cleanupNotifications();

        verify(redisTemplate).keys("NOTIFICATIONS:*");
        verify(redisTemplate, never()).opsForZSet();
    }

    /**
     * Test: Handles error during size retrieval - flux fails fast.
     * Current behavior: When an error occurs on one key, the entire flux stops.
     * Key2 is never processed because Key1 fails.
     */
    @Test
    void cleanupNotifications_ShouldHandleError_WhenSizeRetrievalFails() {
        String key1 = "NOTIFICATIONS:user:111";
        String key2 = "NOTIFICATIONS:user:222";
        RuntimeException error = new RuntimeException("Size retrieval failed");

        when(redisTemplate.keys("NOTIFICATIONS:*")).thenReturn(Flux.just(key1, key2));
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.size(key1)).thenReturn(Mono.error(error));
        // size(key2) is NEVER called because flux fails at key1

        notificationCleanupScheduler.cleanupNotifications();

        verify(redisTemplate).keys("NOTIFICATIONS:*");
        verify(zSetOperations).size(key1);
        // size(key2) should never be called due to fail-fast behavior
        verify(zSetOperations, never()).size(key2);
        verify(zSetOperations, never()).removeRange(eq(key1), any(Range.class));
        verify(zSetOperations, never()).removeRange(eq(key2), any(Range.class));
    }

    /**
     * Test: Handles error during removeRange operation gracefully.
     */
    @Test
    void cleanupNotifications_ShouldHandleError_WhenRemoveRangeFails() {
        String key = "NOTIFICATIONS:user:123";
        Long currentSize = 1500L;
        RuntimeException error = new RuntimeException("Remove range operation failed");

        when(redisTemplate.keys("NOTIFICATIONS:*")).thenReturn(Flux.just(key));
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.size(key)).thenReturn(Mono.just(currentSize));
        when(zSetOperations.removeRange(eq(key), any(Range.class))).thenReturn(Mono.error(error));

        // Should not throw exception (errors are handled within the subscription)
        notificationCleanupScheduler.cleanupNotifications();

        verify(redisTemplate).keys("NOTIFICATIONS:*");
        verify(zSetOperations).size(key);
        verify(zSetOperations).removeRange(eq(key), any(Range.class));
    }

    /**
     * Test: Verifies the correct range is used for deletion (oldest notifications
     * first).
     */
    @Test
    void cleanupNotifications_ShouldUseCorrectRange_ForDeletion() {
        String key = "NOTIFICATIONS:user:123";
        Long currentSize = 1500L;
        Long toRemove = 500L;

        when(redisTemplate.keys("NOTIFICATIONS:*")).thenReturn(Flux.just(key));
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.size(key)).thenReturn(Mono.just(currentSize));
        when(zSetOperations.removeRange(eq(key), any(Range.class))).thenReturn(Mono.just(toRemove));

        notificationCleanupScheduler.cleanupNotifications();

        verify(redisTemplate).keys("NOTIFICATIONS:*");
        verify(zSetOperations).size(key);
        // Verify that removeRange was called with Range.closed(0, toRemove - 1)
        // Which means: remove indices 0 through 499 (oldest 500 notifications)
        verify(zSetOperations).removeRange(eq(key),
                argThat(range -> range.toString().equals(Range.closed(0L, toRemove - 1).toString())));
    }

    /**
     * Test: Verifies scheduled annotation configuration.
     */
    @Test
    void cleanupNotifications_ShouldHaveCorrectScheduledAnnotation() throws NoSuchMethodException {
        var method = NotificationCleanupScheduler.class.getMethod("cleanupNotifications");
        var scheduledAnnotation = method.getAnnotation(org.springframework.scheduling.annotation.Scheduled.class);

        org.assertj.core.api.Assertions.assertThat(scheduledAnnotation).isNotNull();
        org.assertj.core.api.Assertions.assertThat(scheduledAnnotation.fixedDelay()).isEqualTo(60 * 60 * 1000);
    }
}