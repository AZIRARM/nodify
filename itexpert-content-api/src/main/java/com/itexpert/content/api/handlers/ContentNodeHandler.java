package com.itexpert.content.api.handlers;

import com.itexpert.content.api.helpers.ContentHelper;
import com.itexpert.content.api.helpers.NodeHelper;
import com.itexpert.content.api.mappers.ContentNodeMapper;
import com.itexpert.content.api.repositories.ContentNodeRepository;
import com.itexpert.content.api.utils.ContentNodeView;
import com.itexpert.content.api.utils.RulesUtils;
import com.itexpert.content.lib.enums.ContentTypeEnum;
import com.itexpert.content.lib.enums.StatusEnum;
import com.itexpert.content.lib.models.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Slf4j
@AllArgsConstructor
@Service
public class ContentNodeHandler {
    private final ContentNodeRepository contentNodeRepository;
    private final ContentNodeMapper contentNodeMapper;
    private final ContentDisplayHandler contentDisplayHandler;
    private final ContentHelper contentHelper;

    private final NodeHelper nodeHelper;

    private Resource resourceFromContentNode(com.itexpert.content.lib.entities.ContentNode contentNode) {
        byte[] decodedBytes = Base64.getDecoder().decode(contentNode.getContent());
        return new ByteArrayResource(decodedBytes);
    }

    private Mono<ContentNode> addDisplay(ContentNode contentNode) {
        return this.contentDisplayHandler.findByContentCode(contentNode.getCode())
                .switchIfEmpty(Mono.just(new ContentDisplay()))
                .map(contentDisplay -> {
                    if (ObjectUtils.isEmpty(contentDisplay.getContentCode())) {
                        contentDisplay.setContentCode(contentNode.getCode());
                    }
                    return this.contentDisplayHandler.addDisplay(contentDisplay.getContentCode())
                            .map(contentDisplayMono -> contentNode);
                }).flatMap(Mono::from);
    }

    public Flux<ContentNodeView> findAllByNodeCode(String code,
                                                   StatusEnum status,
                                                   String translation,
                                                   boolean fillValues) {
        return
                this.nodeHelper.evaluateNode(code, status).map(node -> code)
                        .map(nodeCode ->
                                this.contentNodeRepository.findAllByNodeCodeAndStatus(nodeCode, status.name())
                                        .map(contentNode -> this.findContentNodeByCode(contentNode.getCode(), status, translation))
                                        .flatMap(Mono::from)
                                        .map(contentNode -> RulesUtils.evaluateContentNode(contentNode)
                                                .filter(aBoolean -> aBoolean)
                                                .map(aBoolean -> contentNode)
                                        )
                                        .flatMap(Mono::from)
                                        .flatMap(this::addDisplay)
                                        .map(this.contentNodeMapper::toView).collectList()
                        ).flatMap(Mono::from).flatMapIterable(contentNodeViews -> contentNodeViews);
    }


    private Mono<Node> evaluateNodeByCodeContent(String codeContent, StatusEnum status) {
        return
                this.contentNodeRepository.findByCodeAndStatus(codeContent, status.name())
                        .map(contentNode -> this.nodeHelper.evaluateNode(contentNode.getParentCode(), status)).flatMap(Mono::from);
    }

    public Mono<ContentNodeView> findByCodeAndStatus(String code,
                                                     StatusEnum status,
                                                     String translation) {
        return this.evaluateNodeByCodeContent(code, status)
                .map(node ->
                        this.findContentNodeByCode(code, status, translation)
                                .flatMap(this::addDisplay)
                                .map(this.contentNodeMapper::toView)
                ).flatMap(Mono::from);
    }

    private Mono<ContentNode> findContentNodeByCode(String code,
                                                    StatusEnum status,
                                                    String translation) {
        return this.contentNodeRepository.findByCodeAndStatus(code, status.name())
                .filter(contentNode -> !contentNode.getType().equals(ContentTypeEnum.FILE) && !contentNode.getType().equals(ContentTypeEnum.PICTURE))
                .flatMap(contentNode -> this.contentHelper.fillContents(contentNode, status))
                .flatMap(contentNode -> this.contentHelper.fillValues(contentNode, status))
                .flatMap(contentNode -> this.contentHelper.translate(contentNode, ObjectUtils.isNotEmpty(translation) ? translation : contentNode.getLanguage(), status))
                .filter(ObjectUtils::isNotEmpty)
                .map(contentNodeMapper::fromEntity)
                .flatMap(contentNode -> RulesUtils.evaluateContentNode(contentNode)
                        .filter(aBoolean -> aBoolean)
                        .map(aBoolean -> contentNode)

                );
    }


    public Mono<Value> saveData(String code, Value value) {
        return this.contentNodeRepository.findByCodeAndStatus(code, StatusEnum.PUBLISHED.name())
                .map(contentNode -> {
                    if (ObjectUtils.isEmpty(contentNode.getDatas())) {
                        contentNode.setDatas(new ArrayList<>());
                    }
                    return Tuples.of(contentNode, this.getDatasIndexByKey(contentNode.getDatas(), value.getKey()));
                })
                .map(tuple -> {
                    if (tuple.getT2() >= 0) {
                        tuple.getT1().getDatas().remove(tuple.getT2().intValue());
                    }
                    tuple.getT1().getDatas().add(value);
                    return tuple.getT1();
                })
                .flatMap(this.contentNodeRepository::save)
                .flatMap(contentNode ->
                        this.contentNodeRepository.findByCodeAndStatus(code, StatusEnum.SNAPSHOT.name()).map(contentSnapshot -> {
                            contentSnapshot.setDatas(contentNode.getDatas());
                            return contentSnapshot;
                        }).flatMap(this.contentNodeRepository::save)
                )
                .map(contentNode -> value);
    }

    private Integer getDatasIndexByKey(List<Value> datas, String key) {
        for (int i = 0; i < datas.size(); i++) {
            if (datas.get(i).getKey().equals(key))
                return i;
        }
        return -1;
    }

    public Mono<Value> getValueByContentNodeCodeAndKey(String code, String key, StatusEnum status) {
        return this.contentNodeRepository.findByCodeAndStatus(code, status.name())
                .filter(contentNode -> ObjectUtils.isNotEmpty(contentNode.getDatas()))
                .map(contentNode ->
                        contentNode.getDatas().stream()
                                .filter(value1 -> value1.getKey().equals(key))
                                .findFirst().get()
                );

    }

    public Flux<Value> getValueByContentNodeCode(String code, StatusEnum status) {
        return this.contentNodeRepository.findByCodeAndStatus(code, status.name())
                .filter(contentNode -> ObjectUtils.isNotEmpty(contentNode.getDatas()))
                .map(com.itexpert.content.lib.entities.ContentNode::getDatas)
                .flatMapIterable(values -> values);

    }

    public Mono<ContentFile> findResourceByCode(String code, StatusEnum status) {
        return
                this.evaluateNodeByCodeContent(code, status)
                        .map(node ->
                                this.contentNodeRepository.findByCodeAndStatus(code, status.name())
                                        .map(contentNodeMapper::fromEntity)
                                        .flatMap(contentNode -> RulesUtils.evaluateContentNode(contentNode)
                                                .filter(aBoolean -> aBoolean)
                                                .map(aBoolean -> contentNode)
                                        )
                                        .filter(contentNode -> ObjectUtils.isNotEmpty(contentNode.getFile()))
                                        .flatMap(this::addDisplay)
                                        .map(ContentNode::getFile)
                        ).flatMap(Mono::from);
    }
}

