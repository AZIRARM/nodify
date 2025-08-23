package com.itexpert.content.core.helpers;

import com.itexpert.content.core.handlers.SlugHandler;
import com.itexpert.content.core.mappers.NodeMapper;
import com.itexpert.content.core.repositories.ContentNodeRepository;
import com.itexpert.content.core.repositories.NodeRepository;
import com.itexpert.content.core.utils.SlugsUtils;
import com.itexpert.content.lib.enums.StatusEnum;
import com.itexpert.content.lib.models.ContentNode;
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

    private final SlugHandler slugHandler;

    public Mono<Node> update(Node node) {
        return this.renameSlug(node, node.getSlug());
    }

    private Mono<Node> renameSlug(Node node, String slug) {
        if (ObjectUtils.isEmpty(slug)) {
            return Mono.just(node);
        }

        return this.slugHandler.existsBySlug(slug)
                .collectList()
                .flatMap(codes -> {
                    if (codes.isEmpty()) {
                        // Cas 1 : pas de code → on met à jour et on retourne
                        node.setSlug(slug);
                        return Mono.just(node);
                    } else if (codes.size() == 1) {
                        // Cas 2 : un seul code trouvé
                        String code = codes.get(0);
                        if (code.equals(node.getCode())) {
                            // Même code → on accepte ce slug
                            node.setSlug(slug);
                            return Mono.just(node);
                        } else {
                            // Code différent → recalculer un nouveau slug et recommencer
                            String newSlug = SlugsUtils.generateSlug(
                                    node.getSlug(),
                                    SlugsUtils.extractRec(slug) + 1
                            );
                            return renameSlug(node, newSlug);
                        }
                    } else {
                        // Cas 3 : plusieurs codes → erreur
                        return Mono.error(new IllegalStateException(
                                "Conflit de slug détecté pour '" + slug + "' : plusieurs codes associés."
                        ));
                    }
                });
    }
}