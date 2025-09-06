package com.itexpert.content.core.handlers;

import com.itexpert.content.core.models.LockInfo;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
public class RedisHandler {

    private final ReactiveStringRedisTemplate redisTemplate;

    public RedisHandler(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // --- Acquérir un lock ---
    public Mono<Boolean> acquireLock(String resourceCode, String userId, Duration ttl) {
        String key = "lock:node:" + resourceCode;
        return redisTemplate.opsForValue()
                .setIfAbsent(key, userId, ttl)
                .defaultIfEmpty(false);
    }

    // --- Relâcher un lock ---
    public Mono<Boolean> releaseLock(String resourceCode, String userId) {
        String key = "lock:node:" + resourceCode;
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
    public Mono<Boolean> refreshLock(String resourceCode, String userId, Duration ttl) {
        String key = "lock:node:" + resourceCode;
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
    public Mono<LockInfo> getLockInfo(String resourceCode, Authentication authentication) {
        String currentUser = authentication.getPrincipal().toString();
        String key = "lock:node:" + resourceCode;

        return redisTemplate.opsForValue().get(key)
                .map(owner -> {
                    boolean isOwner = owner.equals(currentUser);
                    boolean locked = !isOwner; // verrouillé seulement si ce n’est pas lui
                    return new LockInfo(owner, isOwner, locked, resourceCode);
                })
                .defaultIfEmpty(new LockInfo(null, false, false, resourceCode)); // pas de lock
    }

    public Mono<Boolean> forceReleaseLock(String resourceCode) {
        String key = "lock:node:" + resourceCode;
        return redisTemplate.delete(key)
                .map(deleted -> deleted > 0)
                .defaultIfEmpty(false);
    }

    public Flux<LockInfo> getAllLocks() {
        return redisTemplate.keys("lock:node:*")
                .flatMap(key -> redisTemplate.opsForValue().get(key)
                        .map(owner -> {
                            String resourceCode = key.replace("lock:node:", "");
                            return new LockInfo(owner, false, true, resourceCode);
                        })
                );
    }

    public Mono<Boolean> canModify(String resourceCode, String userId, Duration ttl) {
        String key = "lock:node:" + resourceCode;
        return redisTemplate.opsForValue().get(key)
                .flatMap(owner -> {
                    if (owner.equals(userId)) {
                        // déjà verrouillé par l’utilisateur
                        return Mono.just(true);
                    } else {
                        // verrouillé par quelqu’un d’autre
                        return Mono.just(false);
                    }
                })
                .switchIfEmpty(
                        // pas de lock → essayer d’acquérir
                        redisTemplate.opsForValue().setIfAbsent(key, userId, ttl)
                                .defaultIfEmpty(false)
                );
    }

}
