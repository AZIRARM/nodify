package com.itexpert.content.core.schedulers;

import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class LockCleanerScheduler {

  private final ReactiveStringRedisTemplate redisTemplate;

  public LockCleanerScheduler(ReactiveStringRedisTemplate redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  /**
   * Vérifie toutes les heures si certains locks doivent être libérés.
   */
  @Scheduled(fixedRate = 60 * 60 * 1000) // toutes les heures
  public void cleanExpiredLocks() {
    redisTemplate.keys("lock:node:*")
      .flatMap(key ->
        redisTemplate.getExpire(key)
          .flatMap(ttl -> {
            if (ttl == null || ttl <= 0) {
              // Si le TTL est expiré ou absent -> supprimer la clé
              return redisTemplate.delete(key)
                .doOnSuccess(deleted -> {
                  if (deleted > 0) {
                    System.out.println("🔓 Lock libéré : " + key);
                  }
                })
                .then();
            }
            return Mono.empty();
          })
      )
      .subscribe();
  }
}