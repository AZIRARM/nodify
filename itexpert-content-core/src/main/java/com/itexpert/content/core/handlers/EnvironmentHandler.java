package com.itexpert.content.core.handlers;

import com.itexpert.content.core.mappers.EnvironmentMapper;
import com.itexpert.content.core.repositories.EnvironmentRepository;
import com.itexpert.content.lib.enums.NotificationEnum;
import com.itexpert.content.lib.models.Environment;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@Slf4j
@AllArgsConstructor
@Service
public class EnvironmentHandler {
    private final EnvironmentRepository environmentRepository;
    private final EnvironmentMapper environmentMapper;
    private final NotificationHandler notificationHandler;

    public Flux<Environment> findAll() {
        return environmentRepository.findAll().map(user -> {
            return environmentMapper.fromEntity(user);
        });
    }

    public Mono<Environment> findById(UUID uuid) {
        return environmentRepository.findById(uuid).map(environmentMapper::fromEntity);
    }

    @Transactional
    public Mono<Environment> save(Environment environment) {
        return this.environmentRepository.findByCode(environment.getCode())
                .map(this.environmentMapper::fromEntity)
                .switchIfEmpty(Mono.just(environment))
                .map(this.environmentMapper::fromModel)
                .map(entity -> {
                    if (ObjectUtils.isEmpty(entity.getId())) {
                        entity.setId(UUID.randomUUID());
                    }
                    entity.setName(environment.getName());
                    entity.setDescription(environment.getDescription());
                    entity.setNodeCode(environment.getNodeCode());
                    return entity;
                })
                .flatMap(this.environmentRepository::save)
                .map(environmentMapper::fromEntity)
                .flatMap(model -> this.notify(model, NotificationEnum.CREATION_OR_UPDATE));
    }

    @Transactional
    public Flux<Environment> saveAll(List<Environment> environments) {
        return Flux.fromIterable(environments).map(this::save).flatMap(Mono::from)
                .flatMap(model -> this.notify(model, NotificationEnum.CREATION_OR_UPDATE));
    }

    public Mono<Boolean> delete(String code) {
        return this.environmentRepository.findByCode(code)
                .flatMap(entity ->
                        this.notify(this.environmentMapper.fromEntity(entity), NotificationEnum.DELETION)
                                .flatMap(environment ->
                                        this.environmentRepository.deleteById(environment.getId())
                                                .thenReturn(Boolean.TRUE)
                                )
                                .onErrorReturn(Boolean.FALSE)
                );
    }

    public Mono<Environment> findByCode(String code) {
        return environmentRepository.findByCode(code).map(environmentMapper::fromEntity);

    }

    public Mono<Environment> findByNodeCode(String nodeParentCode) {
        return environmentRepository.findByNodeCode(nodeParentCode).map(environmentMapper::fromEntity);

    }

    public Mono<Environment> notify(Environment model, NotificationEnum type) {
        return Mono.just(model).flatMap(environment -> {
            return notificationHandler
                    .create(type, environment.getCode(), null, "ENVIRONMENT", model.getCode(), null)
                    .map(notification -> model);
        });
    }
}

