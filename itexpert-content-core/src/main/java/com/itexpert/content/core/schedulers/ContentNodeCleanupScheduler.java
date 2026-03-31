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

    public Mono<Long> cleanNodesWithInvalidParentCodesReactive() {
        return nodeRepository.findDistinctCodes()
                .collectList()
                .flatMap(contentNodeRepository::deleteByParentCodeNotIn)
                .doOnSuccess(count -> log.info("Scheduler: nettoyage des ContentNode Invalides terminé, {} supprimés",
                        count))
                .doOnError(e -> log.error("Erreur nettoyage", e));
    }

    /**
     * Scheduler exécuté toutes les heures pour nettoyer les contents nodes avec des
     * parentCode invalides.
     */
    @Scheduled(cron = "0 0 * * * *")
    public void cleanNodesWithInvalidParentCodes() {
        log.info("Scheduler: démarrage du nettoyage des nodes avec des parentCode invalides...");
        cleanNodesWithInvalidParentCodesReactive().subscribe();
    }

}