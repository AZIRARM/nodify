package com.itexpert.content.core.schedulers;

import com.itexpert.content.core.repositories.ContentNodeRepository;
import com.itexpert.content.core.repositories.NodeRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class ContentNodeCleanupScheduler {

    private final ContentNodeRepository contentNodeRepository;
    private final NodeRepository nodeRepository;

    /**
     * Reactive cleanup operation that removes ContentNodes whose parentCode
     * does not exist in the Node collection (orphaned content nodes).
     * 
     * @return Mono<Long> The number of deleted ContentNode records
     */
    public Mono<Long> cleanNodesWithInvalidParentCodesReactive() {
        return nodeRepository.findDistinctCodes()
                .collectList()
                .flatMap(existingCodes -> {
                    // Safety check: prevent mass deletion when no valid Node codes exist
                    if (existingCodes == null || existingCodes.isEmpty()) {
                        log.warn("No valid Node codes found - operation cancelled to prevent mass deletion");
                        return Mono.just(0L);
                    }
                    // Delete all ContentNodes whose parentCode is NOT in the list of existing Node
                    // codes
                    return contentNodeRepository.deleteByParentCodeNotIn(existingCodes);
                })
                .doOnSuccess(
                        count -> log.info("Scheduler: ContentNode cleanup completed, {} orphaned nodes deleted", count))
                .doOnError(e -> log.error("Scheduler: ContentNode cleanup failed", e));
    }

    /**
     * Scheduled job that runs every hour to clean up ContentNodes with invalid
     * parentCode references.
     * 
     * CRON expression: "0 0 * * * *" - Runs at the beginning of every hour (00:00,
     * 01:00, 02:00, etc.)
     */
    @Scheduled(cron = "0 0 * * * *")
    public void cleanNodesWithInvalidParentCodes() {
        log.info("Scheduler: starting cleanup of ContentNodes with invalid parentCode references...");
        cleanNodesWithInvalidParentCodesReactive().subscribe();
    }
}