package com.itexpert.content.core.handlers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.mockito.Mockito.*;

public class RedisHandlerTest {

    private ReactiveStringRedisTemplate redisTemplate;
    private ReactiveValueOperations<String, String> valueOperations;
    private RedisHandler redisHandler;

    @BeforeEach
    void setup() {
        redisTemplate = mock(ReactiveStringRedisTemplate.class);
        valueOperations = mock(ReactiveValueOperations.class);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        redisHandler = new RedisHandler(redisTemplate);
    }

    @Test
    void acquireLock_ShouldReturnTrue_WhenLockIsFree() {
        when(valueOperations.setIfAbsent("lock:node:resource1", "user1", Duration.ofSeconds(30)))
                .thenReturn(Mono.just(true));

        StepVerifier.create(redisHandler.acquireLock("resource1", "user1", Duration.ofSeconds(30)))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void acquireLock_ShouldReturnFalse_WhenAlreadyLocked() {
        when(valueOperations.setIfAbsent("lock:node:resource1", "user1", Duration.ofSeconds(30)))
                .thenReturn(Mono.just(false));

        StepVerifier.create(redisHandler.acquireLock("resource1", "user1", Duration.ofSeconds(30)))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void releaseLock_ShouldReturnTrue_WhenUserIsOwner() {
        when(valueOperations.get("lock:node:resource1")).thenReturn(Mono.just("user1"));
        when(redisTemplate.delete("lock:node:resource1")).thenReturn(Mono.just(1L));

        StepVerifier.create(redisHandler.releaseLock("resource1", "user1"))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void releaseLock_ShouldReturnFalse_WhenUserIsNotOwner() {
        when(valueOperations.get("lock:node:resource1")).thenReturn(Mono.just("user2"));

        StepVerifier.create(redisHandler.releaseLock("resource1", "user1"))
                .expectNext(false)
                .verifyComplete();

        verify(redisTemplate, times(0)).delete("lock:node:resource1");
    }

    @Test
    void refreshLock_ShouldReturnTrue_WhenUserIsOwner() {
        when(valueOperations.get("lock:node:resource1")).thenReturn(Mono.just("user1"));
        when(redisTemplate.expire("lock:node:resource1", Duration.ofSeconds(60)))
                .thenReturn(Mono.just(true));

        StepVerifier.create(redisHandler.refreshLock("resource1", "user1", Duration.ofSeconds(60)))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void refreshLock_ShouldReturnFalse_WhenUserIsNotOwner() {
        when(valueOperations.get("lock:node:resource1")).thenReturn(Mono.just("user2"));

        StepVerifier.create(redisHandler.refreshLock("resource1", "user1", Duration.ofSeconds(60)))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void getLockInfo_ShouldReturnOwnedLock_WhenUserIsOwner() {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn("user1");
        when(valueOperations.get("lock:node:resource1")).thenReturn(Mono.just("user1"));

        StepVerifier.create(redisHandler.getLockInfo("resource1", auth))
                .assertNext(lockInfo -> {
                    assert lockInfo.getOwner().equals("user1");
                    assert lockInfo.getMine();     // le lock est à moi
                    assert !lockInfo.getLocked();  // donc pas bloqué
                    assert lockInfo.getResourceCode().equals("resource1");
                })
                .verifyComplete();
    }

    @Test
    void getLockInfo_ShouldReturnLocked_WhenOtherUserOwnsLock() {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn("user1");
        when(valueOperations.get("lock:node:resource1")).thenReturn(Mono.just("user2"));

        StepVerifier.create(redisHandler.getLockInfo("resource1", auth))
                .assertNext(lockInfo -> {
                    assert lockInfo.getOwner().equals("user2");
                    assert !lockInfo.getMine();    // ce n’est pas moi
                    assert lockInfo.getLocked();   // donc bloqué
                    assert lockInfo.getResourceCode().equals("resource1");
                })
                .verifyComplete();
    }

    @Test
    void forceReleaseLock_ShouldReturnTrue_WhenKeyDeleted() {
        when(redisTemplate.delete("lock:node:resource1")).thenReturn(Mono.just(1L));

        StepVerifier.create(redisHandler.forceReleaseLock("resource1"))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void getAllLocks_ShouldReturnFluxOfLockInfos() {
        when(redisTemplate.keys("lock:node:*"))
                .thenReturn(Flux.just("lock:node:res1", "lock:node:res2"));
        when(valueOperations.get("lock:node:res1")).thenReturn(Mono.just("user1"));
        when(valueOperations.get("lock:node:res2")).thenReturn(Mono.just("user2"));

        StepVerifier.create(redisHandler.getAllLocks())
                .expectNextMatches(lockInfo -> lockInfo.getOwner().equals("user1"))
                .expectNextMatches(lockInfo -> lockInfo.getOwner().equals("user2"))
                .verifyComplete();
    }

    @Test
    void canModify_ShouldReturnTrue_WhenAlreadyLockedBySameUser() {
        when(valueOperations.get("lock:node:resource1")).thenReturn(Mono.just("user1"));
        // setIfAbsent ne sera jamais appelé, mais on peut mocker au cas où
        when(valueOperations.setIfAbsent(any(), any(), any())).thenReturn(Mono.just(true));

        StepVerifier.create(redisHandler.canModify("resource1", "user1", Duration.ofMinutes(5)))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void canModify_ShouldReturnFalse_WhenLockedByAnotherUser() {
        when(valueOperations.get("lock:node:resource1")).thenReturn(Mono.just("otherUser"));
        // ici pareil, setIfAbsent ne sera pas appelé, mais on le mock pour éviter NullPointer
        when(valueOperations.setIfAbsent(any(), any(), any())).thenReturn(Mono.just(false));

        StepVerifier.create(redisHandler.canModify("resource1", "user1", Duration.ofMinutes(5)))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void canModify_ShouldTryAcquire_WhenNoLockExists() {
        when(valueOperations.get("lock:node:resource1")).thenReturn(Mono.empty());
        when(valueOperations.setIfAbsent("lock:node:resource1", "user1", Duration.ofSeconds(30)))
                .thenReturn(Mono.just(true));

        StepVerifier.create(redisHandler.canModify("resource1", "user1", Duration.ofSeconds(30)))
                .expectNext(true)
                .verifyComplete();
    }
}

