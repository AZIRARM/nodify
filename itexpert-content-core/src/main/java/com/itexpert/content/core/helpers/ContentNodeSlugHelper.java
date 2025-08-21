package com.itexpert.content.core.helpers;

import com.itexpert.content.core.handlers.SlugHandler;
import com.itexpert.content.core.mappers.ContentNodeMapper;
import com.itexpert.content.core.repositories.ContentNodeRepository;
import com.itexpert.content.core.repositories.NodeRepository;
import com.itexpert.content.core.utils.SlugsUtils;
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
    private final SlugHandler slugHandler;

    public Mono<ContentNode> update(ContentNode content) {
        return this.renameSlug(content, content.getSlug());
    }

    private Mono<ContentNode> renameSlug(ContentNode content, String slug) {
        if (ObjectUtils.isEmpty(slug)) {
            return Mono.just(content);
        }

        return this.slugHandler.existsBySlug(slug)
                .collectList()
                .flatMap(codes -> {
                    if (codes.isEmpty()) {
                        // Cas 1 : pas de code → on met à jour et on retourne
                        content.setSlug(slug);
                        return Mono.just(content);
                    } else if (codes.size() == 1) {
                        // Cas 2 : un seul code trouvé
                        String code = codes.get(0);
                        if (code.equals(content.getCode())) {
                            // Même code → on accepte ce slug
                            content.setSlug(slug);
                            return Mono.just(content);
                        } else {
                            // Code différent → recalculer un nouveau slug et recommencer
                            String newSlug = SlugsUtils.generateSlug(
                                    content.getSlug(),
                                    SlugsUtils.extractRec(slug) + 1
                            );
                            return renameSlug(content, newSlug);
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