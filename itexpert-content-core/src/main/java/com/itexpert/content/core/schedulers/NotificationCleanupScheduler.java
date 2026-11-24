package com.itexpert.content.core.schedulers;

import com.itexpert.content.core.repositories.NotificationRepository;
import com.itexpert.content.lib.entities.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationCleanupScheduler {

    private final NotificationRepository notificationRepository;
    private static final int MAX_NOTIFICATIONS = 1000;

    @Scheduled(fixedDelay = 60 * 60 * 1000)
    public void cleanupNotifications() {
        log.info("Starting notification cleanup (max {} total)", MAX_NOTIFICATIONS);

        notificationRepository.count()
                .defaultIfEmpty(0L)
                .flatMap(total -> {
                    log.info("Total notifications in database: {}", total);
                    if (total > MAX_NOTIFICATIONS) {
                        int toDeleteCount = (int) (total - MAX_NOTIFICATIONS);
                        log.info("Deleting {} oldest notifications", toDeleteCount);

                        return notificationRepository.findOldestNotifications(PageRequest.of(0, toDeleteCount, Sort.by(Sort.Direction.ASC, "date")))
                                .map(Notification::getId)
                                .collectList()
                                .flatMap(idsToDelete -> {
                                    log.info("Found {} IDs to delete", idsToDelete.size());
                                    if (idsToDelete == null || idsToDelete.isEmpty()) {
                                        return Mono.just(0L);
                                    }
                                    return notificationRepository.deleteAllById(idsToDelete)
                                            .then(Mono.just((long) idsToDelete.size()));
                                });
                    }
                    return Mono.just(0L);
                })
                .subscribe(
                        totalDeleted -> log.info("Cleanup finished. {} old notifications deleted", totalDeleted),
                        error -> log.error("Error during notification cleanup", error)
                );
    }
}