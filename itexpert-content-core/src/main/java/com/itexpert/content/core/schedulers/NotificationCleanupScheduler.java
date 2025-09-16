package com.itexpert.content.core.schedulers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationCleanupScheduler {

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private static final int MAX_NOTIFICATIONS = 1000;

    @Scheduled(fixedDelay = 60 * 60 * 1000)
    public void cleanupNotifications() {
        log.info("🚀 Lancement du cleanup des notifications (max {} par utilisateur)", MAX_NOTIFICATIONS);

        redisTemplate.keys("NOTIFICATIONS:*")
                .flatMap(this::trimUserNotifications)
                .subscribe(
                        count -> log.info("✅ Nettoyage effectué, {} anciennes notifications supprimées", count),
                        error -> log.error("❌ Erreur pendant le cleanup des notifications", error)
                );
    }

    private Flux<Long> trimUserNotifications(String key) {
        return redisTemplate.opsForZSet().size(key)
                .flatMapMany(size -> {
                    if (size != null && size > MAX_NOTIFICATIONS) {
                        long toRemove = size - MAX_NOTIFICATIONS;
                        log.info("🧹 Suppression de {} notifications pour {}", toRemove, key);

                        // Supprime par rang (0 = le plus ancien)
                        return redisTemplate.opsForZSet()
                                .removeRange(key, Range.closed(0L, toRemove - 1))
                                .flux(); // Mono<Long> → Flux<Long>
                    } else {
                        return Flux.empty();
                    }
                });
    }

}
