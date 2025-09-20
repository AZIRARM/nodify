package com.itexpert.content.core.handlers;

import com.itexpert.content.core.mappers.ResourceParameterMapper;
import com.itexpert.content.core.repositories.ResourceParamaterRepository;
import com.itexpert.content.lib.enums.ResourceActionEnum;
import com.itexpert.content.lib.enums.ResourceTypeEnum;
import com.itexpert.content.lib.enums.StatusEnum;
import com.itexpert.content.lib.models.ContentNode;
import com.itexpert.content.lib.models.Node;
import com.itexpert.content.lib.models.ResourceParameter;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Comparator;
import java.util.UUID;

@Slf4j
@AllArgsConstructor
@Service
public class ResourceParameterHandler {
    private final ResourceParamaterRepository resourceParamaterRepository;
    private final ResourceParameterMapper resourceParamaterMapper;
    private final NodeHandler nodeHandler;
    private final ContentNodeHandler contentNodeHandler;

    public Mono<Boolean> cleanupArchivedChildren() {
        return resourceParamaterRepository
                .findByTypeAndAction(ResourceTypeEnum.NODE, ResourceActionEnum.ARCHIVE)
                .flatMap(parameter ->
                        this.nodeHandler.findAllByParentCodeAndStatus(parameter.getCode(), StatusEnum.ARCHIVE.name())
                                // Trier par date de création croissante (les plus anciens d'abord)
                                .sort(Comparator.comparing(Node::getCreationDate))
                                // Ne conserver que les plus anciens à supprimer
                                .skip(parameter.getValue()) // on skip les N plus récents
                                .flatMap(node ->
                                        nodeHandler.deleteById(node.getId())
                                                .then(
                                                        contentNodeHandler.findAllByNodeCodeAndStatus(node.getCode(), StatusEnum.ARCHIVE.name())
                                                                .sort(Comparator.comparing(ContentNode::getCreationDate))
                                                                .skip(parameter.getValue())
                                                                .flatMap(cn -> contentNodeHandler.deleteById(cn.getId()))
                                                                .then() // <-- transforme Flux<Void> en Mono<Void> pour le then()
                                                )
                                )).hasElements();
    }


    public Flux<ResourceParameter> findByTypeAndAction(ResourceTypeEnum type,
                                                       ResourceActionEnum action) {
        return this.resourceParamaterRepository.findByTypeAndAction(type, action)
                .map(resourceParamaterMapper::fromEntity);
    }

    public Mono<Boolean> deleteById(UUID id) {
        return this.resourceParamaterRepository.deleteById(id).hasElement();
    }

    public Mono<ResourceParameter> save(ResourceParameter resourceParameter) {
        return Mono.just(resourceParameter)
                .map(resource -> {
                    if (ObjectUtils.isEmpty(resource.getId())) {
                        resource.setId(UUID.randomUUID());
                    }
                    return this.resourceParamaterMapper.fromModel(resource);
                })
                .flatMap(resourceParamaterRepository::save)
                .map(resourceParamaterMapper::fromEntity);
    }

    public Flux<ResourceParameter> findAll() {
        return this.resourceParamaterRepository.findAll()
                .map(this.resourceParamaterMapper::fromEntity);
    }
}

