package com.itexpert.content.core.schedulers;

import com.itexpert.content.core.repositories.ContentNodeRepository;
import com.itexpert.content.core.repositories.NodeRepository;
import com.itexpert.content.lib.entities.ContentNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
@Slf4j
@RequiredArgsConstructor
public class ResourceCleanupScheduler {


    private final NodeRepository nodeRepository;
    private final ContentNodeRepository contentNodeRepository;

    /**
     * Scheduler exécuté toutes les heures pour nettoyer les Nodes archivés.
     */
    @Scheduled(cron = "0 0 * * * *")
    public void cleanNodes() {
        log.info("Scheduler: démarrage du nettoyage des noeuds archivés...");

        nodeRepository.findActiveNodesToClean()
                .filter(node -> node.getMaxVersionsToKeep() != null && node.getMaxVersionsToKeep() > 0)
                .flatMap(activeNode -> {
                    int maxToKeep = activeNode.getMaxVersionsToKeep();

                    return nodeRepository.findArchivedByCode(activeNode.getCode())
                            .sort((a, b) ->
                                    b.getModificationDate().compareTo(a.getModificationDate()))
                            .skip(maxToKeep)
                            .flatMap(nodeRepository::delete);
                })
                .doOnComplete(() -> log.info("Scheduler: nettoyage terminé"))
                .doOnError(e -> log.error("Erreur nettoyage", e))
                .subscribe();
    }


    /**
     * Scheduler exécuté toutes les heures pour nettoyer les ContentNodes archivés.
     */
    @Scheduled(cron = "0 0 * * * *")
    public void cleanContentNodes() {
        log.info("Scheduler: démarrage du nettoyage des contenus archivés...");

        contentNodeRepository.findContentToClean()
                .filter(node ->
                        node.getMaxVersionsToKeep() != null &&
                                node.getMaxVersionsToKeep() > 0 &&
                                node.getCode() != null
                )
                .flatMap(activeContent -> {
                    int maxToKeep = activeContent.getMaxVersionsToKeep();
                    String code = activeContent.getCode();

                    return contentNodeRepository.findArchivedByNodeCode(code)
                            .sort((a, b) ->
                                    b.getModificationDate().compareTo(a.getModificationDate()))
                            .skip(maxToKeep)
                            .flatMap(contentNodeRepository::delete);
                })
                .doOnComplete(() -> log.info("Scheduler: nettoyage des contenus terminé"))
                .doOnError(e -> log.error("Scheduler: erreur lors du nettoyage des contenus", e))
                .subscribe();
    }


}
