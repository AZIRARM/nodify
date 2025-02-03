package com.itexpert.content.core.handlers;

import com.itexpert.content.core.mappers.ContentDisplayMapper;
import com.itexpert.content.core.mappers.NodeMapper;
import com.itexpert.content.core.repositories.ContentDisplayRepository;
import com.itexpert.content.core.repositories.ContentNodeRepository;
import com.itexpert.content.core.repositories.NodeRepository;
import com.itexpert.content.lib.enums.StatusEnum;
import com.itexpert.content.lib.models.ContentDisplay;
import com.itexpert.content.lib.models.ContentDisplayCharts;
import com.itexpert.content.lib.models.Node;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@AllArgsConstructor
@Service
public class ContentDisplayHandler {
    private final ContentDisplayRepository contentDisplayRepository;
    private final ContentNodeRepository contentNodeRepository;
    private final NodeRepository nodeRepository;
    private final ContentDisplayMapper contentDisplayMapper;
    private final NodeMapper nodeMapper;

    public Flux<ContentDisplay> findAll() {
        return ContentDisplayHandler.this.contentDisplayRepository.findAll().map(contentDisplayMapper::fromEntity
        );
    }

    public Mono<ContentDisplay> findByContentCode(String code) {
        return ContentDisplayHandler.this.contentDisplayRepository.findByContentCode(code).map(contentDisplayMapper::fromEntity
        );
    }

    public Mono<ContentDisplay> findById(UUID uuid) {
        return ContentDisplayHandler.this.contentDisplayRepository.findById(uuid).map(contentDisplayMapper::fromEntity);
    }

    public Mono<ContentDisplay> save(ContentDisplay contentDisplay) {
        if (ObjectUtils.isEmpty(contentDisplay.getId())) {
            contentDisplay.setId(UUID.randomUUID());
        }
        return ContentDisplayHandler.this.contentDisplayRepository.save(contentDisplayMapper.fromModel(contentDisplay))
                .map(contentDisplayMapper::fromEntity);
    }


    public Mono<Long> saveAll(List<ContentDisplay> contentDisplays) {
        return ContentDisplayHandler.this.contentDisplayRepository.saveAll(contentDisplays.stream().map(contentDisplayMapper::fromModel).collect(Collectors.toList())).count();

    }

    public Mono<Boolean> delete(UUID uuid) {
        return ContentDisplayHandler.this.contentDisplayRepository.deleteById(uuid)
                .map(unused -> Boolean.TRUE)
                .onErrorReturn(Boolean.FALSE);
    }


    public Flux<ContentDisplayCharts> getCharts() {
        return contentNodeRepository.findAllByStatus(StatusEnum.PUBLISHED.name())
                .flatMap(
                        content -> {
                            return ContentDisplayHandler.this.contentDisplayRepository.findByContentCode(content.getCode())
                                    .map(contentDisplay -> new ContentDisplayCharts(contentDisplay.getContentCode(), contentDisplay.getDisplays().toString()));
                        }

                );

    }

    public Flux<ContentDisplayCharts> getChartsByNodeCode(String code) {
        return
                this.getAllChildrenNodesByCodeAndStatus(code, StatusEnum.SNAPSHOT.name())
                        .map(node ->
                                contentNodeRepository.findByNodeCodeAndStatus(node.getCode(), StatusEnum.PUBLISHED.name())
                                        .flatMap(
                                                content -> {
                                                    return ContentDisplayHandler.this.contentDisplayRepository.findByContentCode(content.getCode())
                                                            .map(contentDisplay -> new ContentDisplayCharts(contentDisplay.getContentCode(), contentDisplay.getDisplays().toString()));
                                                }

                                        )).flatMap(Flux::from);

    }

    private Flux<Node> getAllChildrenNodesByCodeAndStatus(String code, String status) {
        return this.nodeRepository.findByCodeAndStatus(code, status)
                .map(this.nodeMapper::fromEntity)
                .flatMap(node ->
                        // Concatène le nœud courant avec ses enfants
                        Flux.concat(
                                Flux.just(node),
                                getAllChildrenNodesByCodeAndStatusFactory(node.getCode(), status)
                        ).cast(Node.class).collectList()
                ).flatMapIterable(list -> list);
    }


    private Flux<Node> getAllChildrenNodesByCodeAndStatusFactory(String code, String status) {
        return this.nodeRepository.findChildrenByCodeAndStatus(code, status)
                .map(this.nodeMapper::fromEntity)
                .flatMap(childNode ->
                        // Récupère récursivement tous les enfants pour chaque nœud
                        Flux.concat(
                                Flux.just(childNode),
                                getAllChildrenNodesByCodeAndStatusFactory(childNode.getCode(), status)
                        )
                );
    }


    public Flux<ContentDisplayCharts> getChartsByContentCode(String code) {
        return  this.contentDisplayRepository.findByContentCode(code)
                .map(contentDisplay -> new ContentDisplayCharts(contentDisplay.getContentCode(), contentDisplay.getDisplays().toString()))
                .flux();

    }


}

