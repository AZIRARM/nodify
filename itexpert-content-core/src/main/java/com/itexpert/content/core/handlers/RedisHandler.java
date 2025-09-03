package com.itexpert.content.core.handlers;

import com.itexpert.content.core.models.LockInfo;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
public class RedisHandler {

    private final ReactiveStringRedisTemplate redisTemplate;

    public RedisHandler(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // --- Acquérir un lock ---
    public Mono<Boolean> acquireLock(String nodeId, String userId, Duration ttl) {
        String key = "lock:node:" + nodeId;
        return redisTemplate.opsForValue()
                .setIfAbsent(key, userId, ttl)
                .defaultIfEmpty(false);
    }

    // --- Relâcher un lock ---
    public Mono<Boolean> releaseLock(String nodeId, String userId) {
        String key = "lock:node:" + nodeId;
        return redisTemplate.opsForValue().get(key)
                .flatMap(value -> {
                    if (value.equals(userId)) {
                        return redisTemplate.delete(key).map(deleted -> true);
                    }
                    return Mono.just(false);
                })
                .defaultIfEmpty(false);
    }

    // --- Rafraîchir le TTL d’un lock ---
    public Mono<Boolean> refreshLock(String nodeId, String userId, Duration ttl) {
        String key = "lock:node:" + nodeId;
        return redisTemplate.opsForValue().get(key)
                .flatMap(value -> {
                    if (value.equals(userId)) {
                        return redisTemplate.expire(key, ttl);
                    }
                    return Mono.just(false);
                })
                .defaultIfEmpty(false);
    }

    // --- Récupérer le lock sous forme de DTO LockInfo ---
    public Mono<LockInfo> getLockInfo(String nodeId, Authentication authentication) {
        String currentUser = authentication.getPrincipal().toString();
        String key = "lock:node:" + nodeId;

        return redisTemplate.opsForValue().get(key)
                .map(owner -> {
                    boolean isOwner = owner.equals(currentUser);
                    boolean locked = !isOwner; // verrouillé seulement si ce n’est pas lui
                    return new LockInfo(owner, isOwner, locked);
                })
                .defaultIfEmpty(new LockInfo(null, false, false)); // pas de lock
    }

    public Mono<Boolean> forceReleaseLock(String nodeId) {
        String key = "lock:node:" + nodeId;
        return redisTemplate.delete(key)
                .map(deleted -> deleted > 0)
                .defaultIfEmpty(false);
    }

}
