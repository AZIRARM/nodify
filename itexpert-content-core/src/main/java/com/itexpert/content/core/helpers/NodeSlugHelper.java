package com.itexpert.content.core.helpers;

import com.itexpert.content.core.mappers.NodeMapper;
import com.itexpert.content.core.repositories.ContentNodeRepository;
import com.itexpert.content.core.repositories.NodeRepository;
import com.itexpert.content.core.utils.SlugsUtils;
import com.itexpert.content.lib.enums.StatusEnum;
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

    private final NodeMapper nodeMapper;

    private final ContentNodeRepository contentNodeRepository;

    public Mono<Node> update(Node node) {
        return this.nodeRepository.findByCodeAndStatus(node.getCode(), StatusEnum.SNAPSHOT.name())
                .map(this.nodeMapper::fromEntity)
                .flatMap(existingNode-> {
                    if(ObjectUtils.isNotEmpty(existingNode.getSlug())){
                        return Mono.just(node);
                    } else{
                        return this.renameSlug(node, SlugsUtils.generateSlug(node.getSlug(), 0));
                    }
                })
                .switchIfEmpty(Mono.just(node));
    }

    private Mono<Node> renameSlug(Node node, String slug) {
        if (ObjectUtils.isEmpty(slug)) {
            return Mono.just(node);
        }

        if (ObjectUtils.isNotEmpty(node.getSlug())) {
                return Mono.just(node);
        }

        return this.nodeRepository.findBySlugAndCode(slug, node.getCode())
                .hasElements() // transforme le Flux en Mono<Boolean>
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.just(node);
                    } else {
                        // Pas trouvé dans nodeRepository, on cherche dans contentNodeRepository
                        return this.contentNodeRepository.findAllBySlug(slug)
                                .hasElements()
                                .flatMap(existsInContentNodeRepo -> {
                                    if (existsInContentNodeRepo) {
                                        // Le slug existe dans contentNodeRepository → incrément et rappel récursif
                                        String newSlug = SlugsUtils.generateSlug(node.getSlug(), SlugsUtils.extractRec(slug) + 1);
                                        return renameSlug(node, newSlug);
                                    } else {
                                        // Slug dispo dans les deux → on peut setter et retourner node
                                        node.setSlug(slug);
                                        return Mono.just(node);
                                    }
                                });
                    }
                });
    }
}