package com.itexpert.content.api.helpers;

import com.itexpert.content.api.mappers.NodeMapper;
import com.itexpert.content.api.repositories.NodeRepository;
import com.itexpert.content.api.utils.RulesUtils;
import com.itexpert.content.lib.enums.StatusEnum;
import com.itexpert.content.lib.models.Node;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@AllArgsConstructor
@Slf4j
public class NodeHelper {

    private final NodeRepository nodeRepository;
    private final NodeMapper nodeMapper;

    public Mono<Node> evaluateNode(String code, StatusEnum status) {
        return this.findByCodeAndStatus(code, status)
                .switchIfEmpty(Mono.empty())
                .map(node -> {
                    if (ObjectUtils.isNotEmpty(node.getParentCode())) {
                        return this.evaluateNode(node.getParentCode(), status);
                    }
                    return Mono.just(node);
                }).flatMap(Mono::from);
    }

    public Mono<Node> findByCodeAndStatus(String code, StatusEnum status) {
        return nodeRepository.findByCodeAndStatus(code, status.name())
                .map(nodeMapper::fromEntity)
                .flatMap(node -> RulesUtils.evaluateNode(node)
                        .filter(aBoolean -> aBoolean)
                        .map(aBoolean -> node)
                );
    }

}
