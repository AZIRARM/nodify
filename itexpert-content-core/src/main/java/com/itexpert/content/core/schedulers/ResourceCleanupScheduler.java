package com.itexpert.content.core.schedulers;

import com.itexpert.content.core.handlers.ResourceParameterHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class ResourceCleanupScheduler {

    private final ResourceParameterHandler resourceParameterHandler;

    /**
     * Scheduler exécuté toutes les heures pour nettoyer les Nodes et ContentNodes archivés.
     */
    @Scheduled(cron = "0 0 * * * *") // toutes les heures à minute 0
    public void runHourlyCleanup() {
        log.info("Scheduler: démarrage du nettoyage des ressources archivées...");

        resourceParameterHandler.cleanupArchivedChildren()
                .doOnSuccess(result -> log.info("Scheduler: nettoyage terminé, résultats = {}", result))
                .doOnError(error -> log.error("Scheduler: erreur lors du nettoyage des ressources", error))
                .subscribe(); // nécessaire pour déclencher le Flux/Mono en réactif
    }
}
