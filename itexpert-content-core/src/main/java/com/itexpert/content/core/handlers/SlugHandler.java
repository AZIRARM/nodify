package com.itexpert.content.core.handlers;

import com.itexpert.content.core.repositories.ContentNodeRepository;
import com.itexpert.content.core.repositories.NodeRepository;
import com.itexpert.content.lib.entities.ContentNode;
import com.itexpert.content.lib.entities.Node;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Slf4j
@AllArgsConstructor
@Service
public class SlugHandler {
    private final NodeRepository nodeRepository;
    private final ContentNodeRepository contentNodeRepository;


    public Flux<String> existsBySlug(String slug) {
        Flux<String> nodeCodes = Flux.from(this.nodeRepository.findBySlug(slug))
                .map(Node::getCode)
                .filter(Objects::nonNull); // enlève les nulls

        Flux<String> contentCodes = Flux.from(this.contentNodeRepository.findBySlug(slug))
                .map(ContentNode::getCode)
                .filter(Objects::nonNull); // enlève les nulls

        return Flux.concat(nodeCodes, contentCodes)
                .distinct(); // supprime les doublons
    }


}

