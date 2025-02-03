package com.itexpert.content.api.handlers;

import com.itexpert.content.api.mappers.ContentClickMapper;
import com.itexpert.content.api.repositories.ContentClickRepository;
import com.itexpert.content.api.repositories.ContentNodeRepository;
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
    private final ContentClickMapper contentClickMapper;

    public Flux<ContentClick> findAll() {
        return this.contentClickRepository.findAll().map(contentClickMapper::fromEntity
        );
    }

    public Mono<ContentClick> findByContentCode(String code) {
        return this.contentClickRepository.findByContentCode(code).map(contentClickMapper::fromEntity);
    }

    public Mono<ContentClick> findById(UUID uuid) {
        return this.contentClickRepository.findById(uuid).map(contentClickMapper::fromEntity);
    }

    public Mono<Boolean> addClick(String code) {

        return this.contentClickRepository.findByContentCode(code)
                .switchIfEmpty(Mono.just(new com.itexpert.content.lib.entities.ContentClick()))
                .map(contentClick -> {
                    if(ObjectUtils.isEmpty(contentClick.getId())) {
                        contentClick.setId(UUID.randomUUID());
                        contentClick.setClicks(0L);
                        contentClick.setContentCode(code);
                    }
                    contentClick.setClicks(contentClick.getClicks() + 1);
                    return contentClick;
                }).flatMap(this.contentClickRepository::save)
                .map(contentClick -> Boolean.TRUE);
    }


    public Mono<Long> saveAll(List<ContentClick> contentClicks) {
        return this.contentClickRepository.saveAll(contentClicks.stream().map(contentClickMapper::fromModel).collect(Collectors.toList())).count();

    }

    public Mono<Boolean> delete(UUID uuid) {
        return this.contentClickRepository.deleteById(uuid)
                .map(unused -> Boolean.TRUE)
                .onErrorReturn(Boolean.FALSE);
    }


    public Flux<ContentClickCharts> getCharts() {
        return contentNodeRepository.findAllByStatus(StatusEnum.PUBLISHED.name())
                .flatMap(
                        content -> {
                            return this.contentClickRepository.findByContentCode(content.getCode())
                                    .map(contentClick -> {
                                        return new ContentClickCharts(contentClick.getContentCode(), contentClick.getClicks().toString());
                                    });
                        }

                );

    }
}

