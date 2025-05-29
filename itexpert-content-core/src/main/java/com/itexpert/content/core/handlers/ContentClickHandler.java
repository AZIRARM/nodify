package com.itexpert.content.core.handlers;

import com.itexpert.content.core.mappers.ContentClickMapper;
import com.itexpert.content.core.repositories.ContentClickRepository;
import com.itexpert.content.core.repositories.ContentNodeRepository;
import com.itexpert.content.lib.enums.StatusEnum;
import com.itexpert.content.lib.models.ContentClick;
import com.itexpert.content.lib.models.ContentClickCharts;
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
public class ContentClickHandler {
    private final ContentClickRepository contentClickRepository;
    private final ContentNodeRepository contentNodeRepository;
    private final NodeHandler nodeHandler;
    private final ContentClickMapper contentClickMapper;

    public Flux<ContentClick> findAll() {
        return ContentClickHandler.this.contentClickRepository.findAll().map(contentClickMapper::fromEntity
        );
    }

    public Flux<ContentClick> findByContentCode(String code) {
        return ContentClickHandler.this.contentClickRepository.findByContentCode(code).map(contentClickMapper::fromEntity
        );
    }

    public Mono<ContentClick> findById(UUID uuid) {
        return ContentClickHandler.this.contentClickRepository.findById(uuid).map(contentClickMapper::fromEntity);
    }

    public Mono<ContentClick> save(ContentClick contentClick) {
        if (ObjectUtils.isEmpty(contentClick.getId())) {
            contentClick.setId(UUID.randomUUID());
        }
        return ContentClickHandler.this.contentClickRepository.save(contentClickMapper.fromModel(contentClick))
                .map(contentClickMapper::fromEntity);
    }


    public Mono<Long> saveAll(List<ContentClick> contentClicks) {
        return ContentClickHandler.this.contentClickRepository.saveAll(contentClicks.stream().map(contentClickMapper::fromModel).collect(Collectors.toList())).count();

    }

    public Mono<Boolean> delete(UUID uuid) {
        return ContentClickHandler.this.contentClickRepository.deleteById(uuid)
                .map(unused -> Boolean.TRUE)
                .onErrorReturn(Boolean.FALSE);
    }


    public Flux<ContentClickCharts> getCharts() {
        return contentNodeRepository.findAllByStatus(StatusEnum.PUBLISHED.name())
                .flatMap(
                        content -> {
                            return this.contentClickRepository.countClicks(content.getCode())
                                    .map(contentClick -> {
                                        return new ContentClickCharts(contentClick.getContentCode(), contentClick.getClicks().toString());
                                    });
                        }

                );

    }

    public Flux<ContentClickCharts> getChartsByNodeCode(String code) {
        return this.nodeHandler.findAllChildren(code)
                .flatMap(node -> contentNodeRepository.findByNodeCodeAndStatus(node.getCode(), StatusEnum.PUBLISHED.name()))
                .flatMap(
                        content -> {
                            return ContentClickHandler.this.contentClickRepository.countClicks(content.getCode())
                                    .map(contentClick -> {
                                        return new ContentClickCharts(contentClick.getContentCode(), contentClick.getClicks().toString());
                                    });
                        }

                );

    }

    public Flux<ContentClickCharts> getChartsByContentCode(String code) {
        return ContentClickHandler.this.contentClickRepository.countClicks(code)
                .map(contentClick -> {
                    return new ContentClickCharts(contentClick.getContentCode(), contentClick.getClicks().toString());
                }).flux();
    }
}

