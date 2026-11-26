package com.itexpert.content.core.schedulers;

import com.itexpert.content.core.entities.ResourceLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class LockCleanerScheduler {

    private final ReactiveMongoTemplate mongoTemplate;

    @Scheduled(fixedDelayString = "${app.locks.cleanup-delay-ms:60000}")
    public void cleanExpiredLocks() {
        Query expiredQuery = Query.query(Criteria.where("expiresAt").lte(Instant.now()));

        mongoTemplate.remove(expiredQuery, ResourceLock.class)
                .doOnSuccess(result -> {
                    if (result != null && result.getDeletedCount() > 0) {
                        log.debug("Cleaned {} expired resource locks", result.getDeletedCount());
                    }
                })
                .onErrorResume(error -> {
                    log.error("Failed to clean expired resource locks", error);
                    return reactor.core.publisher.Mono.empty();
                })
                .subscribe();
    }
}