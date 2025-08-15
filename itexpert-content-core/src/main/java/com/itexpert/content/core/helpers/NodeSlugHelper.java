package com.itexpert.content.core.helpers;

import com.itexpert.content.core.repositories.ContentNodeRepository;
import com.itexpert.content.core.repositories.NodeRepository;
import com.itexpert.content.lib.models.Node;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@AllArgsConstructor
public class NodeSlugHelper {

    private final NodeRepository nodeRepository;

    private final ContentNodeRepository contentNodeRepository;

    public Mono<Node> update(Node node, String environment) {
        return this.renameSlug(node, environment, this.generateSlug(node.getSlug(), environment, 0));
    }

    private Mono<Node> renameSlug(Node node, String environment, String slug) {
        if (ObjectUtils.isEmpty(slug)) {
            return Mono.just(node);
        }

        return this.nodeRepository.findBySlugAndCode(slug, node.getCode())
                .hasElements() // transforme le Flux en Mono<Boolean>
                .flatMap(exists -> {
                    if (exists) {
                        // Le slug existe dans nodeRepository → incrément et rappel récursif
                        return Mono.just(node);
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
            String baseSlug = ObjectUtils.isNotEmpty(environment) ? slug.trim().replace(environment.toLowerCase(), "") :  slug.trim();
            if (rec <= 0) {
                return ObjectUtils.isNotEmpty(environment) ? baseSlug + "-" + environment.toLowerCase() : baseSlug;
            } else {
                return ObjectUtils.isNotEmpty(environment) ? baseSlug + "-" + environment.toLowerCase() + rec :  baseSlug + "-" + rec;
            }
        }
        return null;
    }

    // Permet de récupérer le compteur rec d'un slug existant
    private int extractRec(String slug) {
        String digits = slug.replaceAll(".*?(\\d+)$", "$1");
        return digits.matches("\\d+") ? Integer.parseInt(digits) : 0;
    }


}