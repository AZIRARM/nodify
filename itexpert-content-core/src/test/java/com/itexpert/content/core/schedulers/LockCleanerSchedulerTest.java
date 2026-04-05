package com.itexpert.content.core.schedulers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LockCleanerSchedulerTest {

    @Mock
    private ReactiveStringRedisTemplate redisTemplate;

    @InjectMocks
    private LockCleanerScheduler lockCleanerScheduler;

    /**
     * Test: Successfully deletes expired locks with negative TTL.
     * Duration.isNegative() or Duration.isZero() indicates expired key
     */
    @Test
    void cleanExpiredLocks_ShouldDeleteLocks_WhenTTLIsNegative() {
        String lockKey = "lock:node:123";
        Duration negativeTTL = Duration.ofSeconds(-2); // Negative duration indicates expired key

        when(redisTemplate.keys("lock:node:*")).thenReturn(Flux.just(lockKey));
        when(redisTemplate.getExpire(lockKey)).thenReturn(Mono.just(negativeTTL));
        when(redisTemplate.delete(lockKey)).thenReturn(Mono.just(1L));

        lockCleanerScheduler.cleanExpiredLocks();

        verify(redisTemplate).keys("lock:node:*");
        verify(redisTemplate).getExpire(lockKey);
        verify(redisTemplate).delete(lockKey);
    }

    /**
     * Test: Successfully deletes expired locks with zero TTL.
     */
    @Test
    void cleanExpiredLocks_ShouldDeleteLocks_WhenTTLIsZero() {
        String lockKey = "lock:node:456";
        Duration zeroTTL = Duration.ZERO;

        when(redisTemplate.keys("lock:node:*")).thenReturn(Flux.just(lockKey));
        when(redisTemplate.getExpire(lockKey)).thenReturn(Mono.just(zeroTTL));
        when(redisTemplate.delete(lockKey)).thenReturn(Mono.just(1L));

        lockCleanerScheduler.cleanExpiredLocks();

        verify(redisTemplate).keys("lock:node:*");
        verify(redisTemplate).getExpire(lockKey);
        verify(redisTemplate).delete(lockKey);
    }

    /**
     * Test: Does NOT delete locks when TTL is null (key has no expiration).
     * Current scheduler behavior: Mono.empty() means flatMap is never executed,
     * so no deletion occurs.
     */
    @Test
    void cleanExpiredLocks_ShouldNotDeleteLocks_WhenTTLIsNull() {
        String lockKey = "lock:node:789";

        when(redisTemplate.keys("lock:node:*")).thenReturn(Flux.just(lockKey));
        when(redisTemplate.getExpire(lockKey)).thenReturn(Mono.empty()); // Simulates null TTL
        // Note: delete() is NOT mocked because it should never be called

        lockCleanerScheduler.cleanExpiredLocks();

        verify(redisTemplate).keys("lock:node:*");
        verify(redisTemplate).getExpire(lockKey);
        verify(redisTemplate, never()).delete(anyString()); // Verify delete is NEVER called
    }

    /**
     * Test: Does NOT delete locks that still have valid (positive) TTL.
     */
    @Test
    void cleanExpiredLocks_ShouldNotDeleteLocks_WhenTTLIsPositive() {
        String lockKey = "lock:node:999";
        Duration positiveTTL = Duration.ofHours(1); // 1 hour remaining

        when(redisTemplate.keys("lock:node:*")).thenReturn(Flux.just(lockKey));
        when(redisTemplate.getExpire(lockKey)).thenReturn(Mono.just(positiveTTL));

        lockCleanerScheduler.cleanExpiredLocks();

        verify(redisTemplate).keys("lock:node:*");
        verify(redisTemplate).getExpire(lockKey);
        verify(redisTemplate, never()).delete(anyString());
    }

    /**
     * Test: Handles multiple locks - deletes expired ones, keeps valid ones.
     */
    @Test
    void cleanExpiredLocks_ShouldHandleMultipleLocks_Correctly() {
        String expiredLock1 = "lock:node:111";
        String expiredLock2 = "lock:node:222";
        String validLock = "lock:node:333";
        Duration expiredTTL = Duration.ofSeconds(-1);
        Duration validTTL = Duration.ofMinutes(30); // 30 minutes remaining

        when(redisTemplate.keys("lock:node:*")).thenReturn(Flux.just(expiredLock1, expiredLock2, validLock));
        when(redisTemplate.getExpire(expiredLock1)).thenReturn(Mono.just(expiredTTL));
        when(redisTemplate.getExpire(expiredLock2)).thenReturn(Mono.just(expiredTTL));
        when(redisTemplate.getExpire(validLock)).thenReturn(Mono.just(validTTL));
        when(redisTemplate.delete(expiredLock1)).thenReturn(Mono.just(1L));
        when(redisTemplate.delete(expiredLock2)).thenReturn(Mono.just(1L));

        lockCleanerScheduler.cleanExpiredLocks();

        verify(redisTemplate).keys("lock:node:*");
        verify(redisTemplate).getExpire(expiredLock1);
        verify(redisTemplate).getExpire(expiredLock2);
        verify(redisTemplate).getExpire(validLock);
        verify(redisTemplate).delete(expiredLock1);
        verify(redisTemplate).delete(expiredLock2);
        verify(redisTemplate, never()).delete(validLock);
    }

    /**
     * Test: Handles empty key set gracefully (no locks to clean).
     */
    @Test
    void cleanExpiredLocks_ShouldDoNothing_WhenNoKeysFound() {
        when(redisTemplate.keys("lock:node:*")).thenReturn(Flux.empty());

        lockCleanerScheduler.cleanExpiredLocks();

        verify(redisTemplate).keys("lock:node:*");
        verify(redisTemplate, never()).getExpire(anyString());
        verify(redisTemplate, never()).delete(anyString());
    }

    /**
     * Test: Handles error during keys retrieval gracefully.
     */
    @Test
    void cleanExpiredLocks_ShouldHandleError_WhenKeysRetrievalFails() {
        RuntimeException error = new RuntimeException("Redis connection failed");

        when(redisTemplate.keys("lock:node:*")).thenReturn(Flux.error(error));

        lockCleanerScheduler.cleanExpiredLocks();

        verify(redisTemplate).keys("lock:node:*");
        verify(redisTemplate, never()).getExpire(anyString());
        verify(redisTemplate, never()).delete(anyString());
    }

    /**
     * Test: Handles error during TTL retrieval - flux fails fast.
     * Current behavior: When an error occurs on one key, the entire flux stops.
     * Key2 is never processed because Key1 fails.
     */
    @Test
    void cleanExpiredLocks_ShouldHandleError_WhenGetExpireFails() {
        String lockKey1 = "lock:node:111";
        String lockKey2 = "lock:node:222";
        RuntimeException error = new RuntimeException("TTL retrieval failed");

        when(redisTemplate.keys("lock:node:*")).thenReturn(Flux.just(lockKey1, lockKey2));
        when(redisTemplate.getExpire(lockKey1)).thenReturn(Mono.error(error));
        // getExpire(lockKey2) is NEVER called because flux fails at lockKey1

        lockCleanerScheduler.cleanExpiredLocks();

        verify(redisTemplate).keys("lock:node:*");
        verify(redisTemplate).getExpire(lockKey1);
        // getExpire(lockKey2) should never be called due to fail-fast behavior
        verify(redisTemplate, never()).getExpire(lockKey2);
        verify(redisTemplate, never()).delete(anyString());
    }

    /**
     * Test: Handles error during delete operation gracefully.
     */
    @Test
    void cleanExpiredLocks_ShouldHandleError_WhenDeleteFails() {
        String lockKey = "lock:node:111";
        Duration expiredTTL = Duration.ofSeconds(-1);
        RuntimeException error = new RuntimeException("Delete operation failed");

        when(redisTemplate.keys("lock:node:*")).thenReturn(Flux.just(lockKey));
        when(redisTemplate.getExpire(lockKey)).thenReturn(Mono.just(expiredTTL));
        when(redisTemplate.delete(lockKey)).thenReturn(Mono.error(error));

        lockCleanerScheduler.cleanExpiredLocks();

        verify(redisTemplate).keys("lock:node:*");
        verify(redisTemplate).getExpire(lockKey);
        verify(redisTemplate).delete(lockKey);
    }

    /**
     * Test: Only deletes locks matching the pattern "lock:node:*".
     */
    @Test
    void cleanExpiredLocks_ShouldOnlyProcessMatchingKeys() {
        String matchingKey = "lock:node:123";
        Duration expiredTTL = Duration.ofSeconds(-1);

        when(redisTemplate.keys("lock:node:*")).thenReturn(Flux.just(matchingKey));
        when(redisTemplate.getExpire(matchingKey)).thenReturn(Mono.just(expiredTTL));
        when(redisTemplate.delete(matchingKey)).thenReturn(Mono.just(1L));

        lockCleanerScheduler.cleanExpiredLocks();

        verify(redisTemplate).keys("lock:node:*");
        verify(redisTemplate).getExpire(matchingKey);
        verify(redisTemplate).delete(matchingKey);
    }
}