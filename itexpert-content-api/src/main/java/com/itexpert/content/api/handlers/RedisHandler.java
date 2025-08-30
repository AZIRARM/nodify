package com.itexpert.content.api.handlers;

import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
public class RedisHandler {

    private final ReactiveRedisTemplate<String, byte[]> redisTemplate;

    public RedisHandler(ReactiveRedisTemplate<String, byte[]> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Mono<byte[]> get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public Mono<Boolean> set(String key, byte[] value, Duration ttl) {
        return redisTemplate.opsForValue().set(key, value, ttl);
    }
}
