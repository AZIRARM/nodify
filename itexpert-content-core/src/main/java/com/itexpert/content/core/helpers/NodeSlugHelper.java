package com.itexpert.content.core.helpers;

import com.itexpert.content.core.mappers.NodeMapper;
import com.itexpert.content.core.repositories.ContentNodeRepository;
import com.itexpert.content.core.repositories.NodeRepository;
import com.itexpert.content.lib.models.Node;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Component
@AllArgsConstructor
public class NodeSlugHelper {

    private final NodeRepository nodeRepository;

    private final ContentNodeRepository contentNodeRepository;

    private final NodeMapper nodeMapper;

    public Flux<Node> update(List<Node> nodes, String environment) {
        return Flux.fromIterable(nodes)
                .flatMap(node -> this.renameSlug(node, environment, this.generateSlug(node.getSlug(), environment, 0)));
    }

    private Mono<Node> renameSlug(Node node, String environment, String slug) {
        return this.nodeRepository.findAllBySlug(slug)
                .hasElements()
                .flatMap(existsInNodeRepo -> {
                    if (existsInNodeRepo) {
                        // Le slug existe dans nodeRepository → incrément et rappel récursif
                        String newSlug = this.generateSlug(node.getSlug(), environment, extractRec(slug) + 1);
                        return renameSlug(node, environment, newSlug);
                    } else {
                        // Pas trouvé dans nodeRepository, on cherche dans contentNodeRepository
                        return this.contentNodeRepository.findAllBySlug(slug)
                                .hasElements()
                                .flatMap(existsInContentNodeRepo -> {
                                    if (existsInContentNodeRepo) {
                                        // Le slug existe dans contentNodeRepository → incrément et rappel récursif
                                        String newSlug = this.generateSlug(node.getSlug(), environment, extractRec(slug) + 1);
                                        return renameSlug(node, environment, newSlug);
                                    } else {
                                        // Slug dispo dans les deux → on peut setter et retourner node
                                        node.setSlug(slug);
                                        return Mono.just(node);
                                    }
                                });
                    }
                });
    }

    private String generateSlug(String slug, String environment, int rec) {
        if (ObjectUtils.isNotEmpty(slug)) {
            String baseSlug = slug.trim().replace(environment, "");
            if (rec <= 0) {
                return baseSlug + "-" + environment;
            } else {
                return baseSlug + "-" + environment + rec;
            }
        }
        return environment + (rec > 0 ? rec : "");
    }

    // Permet de récupérer le compteur rec d'un slug existant
    private int extractRec(String slug) {
        String digits = slug.replaceAll(".*?(\\d+)$", "$1");
        return digits.matches("\\d+") ? Integer.parseInt(digits) : 0;
    }


}