package com.itexpert.content.core.helpers;

import com.itexpert.content.core.repositories.ContentNodeRepository;
import com.itexpert.content.core.repositories.NodeRepository;
import com.itexpert.content.lib.models.ContentNode;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@AllArgsConstructor
public class ContentNodeSlugHelper {

    private final ContentNodeRepository contentNodeRepository;

    private final NodeRepository nodeRepository;

    public Mono<ContentNode> update(ContentNode content, String environment) {
        return this.renameSlug(content, environment, this.generateSlug(content.getSlug(), environment, 0));
    }

    private Mono<ContentNode> renameSlug(ContentNode content, String environment, String slug) {
        if (ObjectUtils.isEmpty(slug)) {
            return Mono.just(content);
        }

        return this.contentNodeRepository.findBySlugAndCode(slug, content.getCode())
                .hasElements() // transforme le Flux en Mono<Boolean>
                .flatMap(exists -> {
                    if (exists) {
                       return Mono.just(content);
                    } else {
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