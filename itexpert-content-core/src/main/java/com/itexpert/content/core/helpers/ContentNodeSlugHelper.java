package com.itexpert.content.core.helpers;

import com.itexpert.content.core.mappers.ContentNodeMapper;
import com.itexpert.content.core.repositories.ContentNodeRepository;
import com.itexpert.content.core.repositories.NodeRepository;
import com.itexpert.content.lib.models.ContentNode;
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
public class ContentNodeSlugHelper {

    private final ContentNodeRepository contentNodeRepository;

    private final NodeRepository nodeRepository;

    private final ContentNodeMapper contentNodeMapper;

    public Flux<ContentNode> update(List<ContentNode> contents, String environment) {
        return Flux.fromIterable(contents)
                .flatMap(content -> this.renameSlug(content, environment, this.generateSlug(content.getSlug(), environment, 0)));
    }

    private Mono<ContentNode> renameSlug(ContentNode content, String environment, String slug) {
        return this.contentNodeRepository.findAllBySlug(slug)
                .hasElements()
                .flatMap(existsInContentNodeRepo -> {
                    if (existsInContentNodeRepo) {
                        String newSlug = this.generateSlug(content.getSlug(), environment, extractRec(slug) + 1);
                        return renameSlug(content, environment, newSlug);
                    } else {
                        return this.nodeRepository.findAllBySlug(slug)
                                .hasElements()
                                .flatMap(existsInNodeRepo -> {
                                    if (existsInNodeRepo) {
                                        String newSlug = this.generateSlug(content.getSlug(), environment, extractRec(slug) + 1);
                                        return renameSlug(content, environment, newSlug);
                                    } else {
                                        content.setSlug(slug);
                                        return Mono.just(content);
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