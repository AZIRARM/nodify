package com.itexpert.content.core.handlers;

import com.itexpert.content.core.mappers.LanguageMapper;
import com.itexpert.content.core.repositories.LanguageRepository;
import com.itexpert.content.lib.enums.NotificationEnum;
import com.itexpert.content.lib.models.Language;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@Slf4j
@AllArgsConstructor
@Service
public class LanguageHandler {
    private final LanguageRepository languageRepository;
    private final LanguageMapper languageMapper;
    private final NotificationHandler notificationHandler;

    public Flux<Language> findAll() {
        return languageRepository.findAll().map(user -> {
            return languageMapper.fromEntity(user);
        });
    }

    public Mono<Language> findById(UUID uuid) {
        return languageRepository.findById(uuid).map(languageMapper::fromEntity);
    }

    public Mono<Language> save(Language language) {
        if (ObjectUtils.isEmpty(language.getId())) {
            language.setId(UUID.randomUUID());
        }
        return languageRepository.save(languageMapper.fromModel(language))
                .map(languageMapper::fromEntity)
                .flatMap(model -> this.notify(model, NotificationEnum.CREATION_OR_UPDATE));
    }

    public Mono<Language> notify(Language model, NotificationEnum type) {
        return Mono.just(model).flatMap(language -> {
            return notificationHandler
                    .create(type, language.getCode(), null, "LANGUAGE", model.getCode(), null)
                    .map(notification -> model);
        });
    }

    public Flux<Language> saveAll(List<Language> languages) {

        return Flux.fromIterable(languages).map(language ->
                languageRepository.findByCode(language.getCode())
                        .switchIfEmpty(languageRepository.save(languageMapper.fromModel(language)))
                        .map(languageMapper::fromEntity)
        ).flatMap(Mono::from);

    }

    public Mono<Boolean> delete(UUID uuid) {
        return this.languageRepository.findById(uuid)
                .flatMap(entity ->
                        this.notify(this.languageMapper.fromEntity(entity), NotificationEnum.DELETION)
                                .flatMap(notification ->
                                        this.languageRepository.deleteById(uuid)
                                                .thenReturn(Boolean.TRUE)
                                )
                                .onErrorReturn(Boolean.FALSE)
                );
    }

}

