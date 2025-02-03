package com.itexpert.content.api.handlers;

import com.itexpert.content.api.mappers.ContentDisplayMapper;
import com.itexpert.content.api.repositories.ContentDisplayRepository;
import com.itexpert.content.api.repositories.ContentNodeRepository;
import com.itexpert.content.lib.enums.StatusEnum;
import com.itexpert.content.lib.models.ContentDisplay;
import com.itexpert.content.lib.models.ContentDisplayCharts;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@AllArgsConstructor
@Service
public class ContentDisplayHandler {
    private final ContentDisplayRepository contentDisplayRepository;
    private final ContentNodeRepository contentNodeRepository;
    private final ContentDisplayMapper contentDisplayMapper;

    public Flux<ContentDisplay> findAll() {
        return this.contentDisplayRepository.findAll().map(contentDisplayMapper::fromEntity
        );
    }

    public Mono<ContentDisplay> findByContentCode(String code) {
        return this.contentDisplayRepository.findByContentCode(code)
                .map(contentDisplayMapper::fromEntity);
    }

    public Mono<ContentDisplay> findById(UUID uuid) {
        return this.contentDisplayRepository.findById(uuid).map(contentDisplayMapper::fromEntity);
    }


    public Mono<Boolean> addDisplay(String code) {

        return this.contentDisplayRepository.findByContentCode(code)
                .switchIfEmpty(Mono.just(new com.itexpert.content.lib.entities.ContentDisplay()))
                .map(contentDisplay -> {
                    if(ObjectUtils.isEmpty(contentDisplay.getId())) {
                        contentDisplay.setId(UUID.randomUUID());
                        contentDisplay.setDisplays(0L);
                        contentDisplay.setContentCode(code);
                    }
                    contentDisplay.setDisplays(contentDisplay.getDisplays() + 1);
                    return contentDisplay;
                }).flatMap(this.contentDisplayRepository::save)
                .map(contentClick -> Boolean.TRUE);
    }


    public Mono<Long> saveAll(List<ContentDisplay> contentDisplays) {
        return this.contentDisplayRepository.saveAll(contentDisplays.stream().map(contentDisplayMapper::fromModel).collect(Collectors.toList())).count();

    }

    public Mono<Boolean> delete(UUID uuid) {
        return this.contentDisplayRepository.deleteById(uuid)
                .map(unused -> Boolean.TRUE)
                .onErrorReturn(Boolean.FALSE);
    }


    public Flux<ContentDisplayCharts> getCharts() {
        return contentNodeRepository.findAllByStatus(StatusEnum.PUBLISHED.name())
                .flatMap(
                        content -> {
                            return this.contentDisplayRepository.findByContentCode(content.getCode())
                                    .map(contentDisplay -> new ContentDisplayCharts(contentDisplay.getContentCode(), contentDisplay.getDisplays().toString()));
                        }

                );

    }
}

