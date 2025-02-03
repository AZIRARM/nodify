package com.itexpert.content.api.handlers;

import com.itexpert.content.api.helpers.NodeHelper;
import com.itexpert.content.api.mappers.NodeMapper;
import com.itexpert.content.api.repositories.NodeRepository;
import com.itexpert.content.api.utils.RulesUtils;
import com.itexpert.content.lib.enums.StatusEnum;
import com.itexpert.content.lib.models.Node;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@AllArgsConstructor
@Service
public class NodeHandler {
    private final NodeRepository nodeRepository;
    private final NodeMapper nodeMapper;
    private final NodeHelper nodeHelper;

    public Flux<Node> findAll(StatusEnum status) {
        return nodeRepository.findAll()
                .filter(node -> ObjectUtils.isNotEmpty(node.getStatus().equals(status)))
                .map(nodeMapper::fromEntity)
                .flatMap(node -> RulesUtils.evaluateNode(node)
                        .filter(aBoolean -> aBoolean)
                        .map(aBoolean -> node)
                );
    }

    public Mono<Node> findByCodeAndStatus(String code, StatusEnum status) {

        return this.nodeHelper.evaluateNode(code, status).map(Node::getCode)
                .map(nodeCode -> nodeRepository.findByCodeAndStatus(code, status.name())
                        .map(nodeMapper::fromEntity)
                        .flatMap(node -> RulesUtils.evaluateNode(node)
                                .filter(aBoolean -> aBoolean)
                                .map(aBoolean -> node)
                        )
                ).flatMap(Mono::from);
    }


    public Flux<Node> findParentsNodes(StatusEnum status) {
        return nodeRepository.findParentsNodesByStatus(status.name())
                .map(nodeMapper::fromEntity)
                .flatMap(node -> this.nodeHelper.evaluateNode(node.getCode(), status))
                .flatMap(node -> RulesUtils.evaluateNode(node)
                        .filter(aBoolean -> aBoolean)
                        .map(aBoolean -> node)
                );
    }

    public Flux<Node> findChildreensByCodeParent(String code, StatusEnum status) {
        return nodeRepository.findChildreensByCodeParent(code, status.name())
                .map(nodeMapper::fromEntity)
                .flatMap(node -> RulesUtils.evaluateNode(node)
                        .filter(aBoolean -> aBoolean)
                        .map(aBoolean -> node)
                );
    }
}

