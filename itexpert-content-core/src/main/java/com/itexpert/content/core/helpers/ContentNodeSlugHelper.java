package com.itexpert.content.core.helpers;

import com.itexpert.content.core.mappers.ContentNodeMapper;
import com.itexpert.content.core.repositories.ContentNodeRepository;
import com.itexpert.content.core.repositories.NodeRepository;
import com.itexpert.content.core.utils.SlugsUtils;
import com.itexpert.content.lib.enums.StatusEnum;
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
    private final ContentNodeMapper contentNodeMapper;
    private final NodeRepository nodeRepository;

    public Mono<ContentNode> update(ContentNode content) {
        return this.contentNodeRepository.findByCodeAndStatus(content.getCode(), StatusEnum.SNAPSHOT.name())
                .map(this.contentNodeMapper::fromEntity)
                .flatMap(existingNode-> {
                    if(ObjectUtils.isNotEmpty(existingNode.getSlug())){
                        return Mono.just(content);
                    } else{
                        return this.renameSlug(content, SlugsUtils.generateSlug(content.getSlug(), 0));
                    }
                })
                .switchIfEmpty(Mono.just(content));
    }

    private Mono<ContentNode> renameSlug(ContentNode content, String slug) {
        if (ObjectUtils.isEmpty(slug)) {
            return Mono.just(content);
        }
        if (ObjectUtils.isNotEmpty(content.getSlug())) {
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
                                        String newSlug = SlugsUtils.generateSlug(content.getSlug(), SlugsUtils.extractRec(slug) + 1);
                                        return renameSlug(content, newSlug);
                                    } else {
                                        return this.nodeRepository.findAllBySlug(slug)
                                                .hasElements()
                                                .flatMap(existsInNodeRepo -> {
                                                    if (existsInNodeRepo) {
                                                        String newSlug = SlugsUtils.generateSlug(content.getSlug(), SlugsUtils.extractRec(slug) + 1);
                                                        return renameSlug(content, newSlug);
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
}