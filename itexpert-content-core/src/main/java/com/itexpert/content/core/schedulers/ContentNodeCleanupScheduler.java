package com.itexpert.content.core.schedulers;

import com.itexpert.content.core.repositories.ContentNodeRepository;
import com.itexpert.content.core.repositories.NodeRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.List;

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
        return contentNodeRepository.findDistinctParentCodes()
                .filter(code -> code != null && !code.isBlank())
                .collectList()
                .flatMap(this::processOrphanCleanup); // Clearer for the compiler
    }

    private Mono<Long> processOrphanCleanup(List<String> parentCodesInContent) {
        // If no parentCodes exist, nothing to check
        if (parentCodesInContent.isEmpty()) {
            return Mono.just(0L);
        }

        return nodeRepository.findExistingCodesIn(parentCodesInContent)
                .collectList()
                .flatMap(existingCodes -> {
                    // Identify codes present in ContentNode but missing in Node
                    List<String> orphanedCodes = parentCodesInContent.stream()
                            .filter(code -> !existingCodes.contains(code))
                            .toList();

                    if (orphanedCodes.isEmpty()) {
                        log.info("Scheduler: No orphaned ContentNodes found.");
                        return Mono.just(0L);
                    }

                    log.info("Scheduler: Deleting ContentNodes for {} orphaned codes", orphanedCodes.size());
                    return contentNodeRepository.deleteByParentCodeIn(orphanedCodes);
                });
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