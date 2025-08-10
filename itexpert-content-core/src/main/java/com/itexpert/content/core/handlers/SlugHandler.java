package com.itexpert.content.core.handlers;

import com.itexpert.content.core.repositories.ContentNodeRepository;
import com.itexpert.content.core.repositories.NodeRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@AllArgsConstructor
@Service
public class SlugHandler {
    private final NodeRepository nodeRepository;
    private final ContentNodeRepository contentNodeRepository;


    public Mono<Boolean> existsBySlug(String slug) {
        return this.nodeRepository.existsBySlug(slug)
                .flatMap(exists -> {
                    if (!exists) {
                        return this.contentNodeRepository.existsBySlug(slug);
                    }
                    return Mono.just(true);
                });
    }

}

