package com.itexpert.content.core.handlers;

import com.itexpert.content.core.mappers.AccessRoleMapper;
import com.itexpert.content.core.models.AccessRole;
import com.itexpert.content.core.repositories.AccessRoleRepository;
import com.itexpert.content.lib.enums.NotificationEnum;
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
public class AccessRoleHandler {
    private final AccessRoleRepository accessRoleRepository;
    private final AccessRoleMapper accessRoleMapper;
    private final NotificationHandler notificationHandler;

    public Flux<AccessRole> findAll() {
        return accessRoleRepository.findAll().map(accessRoleMapper::fromEntity);
    }

    public Mono<AccessRole> findById(UUID uuid) {
        return accessRoleRepository.findById(uuid).map(accessRoleMapper::fromEntity);
    }

    public Mono<AccessRole> save(AccessRole role) {
        if (ObjectUtils.isEmpty(role.getId())) {
            role.setId(UUID.randomUUID());
        }
        return accessRoleRepository.findByCode(role.getCode())
                .switchIfEmpty(accessRoleRepository.save(accessRoleMapper.fromModel(role)))
                .map(accessRoleMapper::fromEntity)
                .flatMap(model -> this.notify(model, NotificationEnum.CREATION_OR_UPDATE));
    }


    public Mono<Boolean> delete(UUID uuid) {
        return this.accessRoleRepository.findById(uuid)
                .flatMap(entity ->
                        this.notify(this.accessRoleMapper.fromEntity(entity), NotificationEnum.DELETION)
                                .flatMap(accessRole ->
                                        this.accessRoleRepository.deleteById(accessRole.getId())
                                                .thenReturn(Boolean.TRUE)
                                )
                                .onErrorReturn(Boolean.FALSE)
                );
    }

    public Mono<AccessRole> findByCode(String code) {
        return accessRoleRepository.findByCode(code).map(accessRoleMapper::fromEntity);
    }

    public Flux<AccessRole> saveAll(List<AccessRole> roles) {
        return Flux.fromIterable(roles).flatMap(this::save);
    }

    public Mono<AccessRole> notify(AccessRole model, NotificationEnum type) {
        return Mono.just(model).flatMap(accessRole -> {
            return notificationHandler
                    .create(type, accessRole.getCode(), null, "ACCESS_ROLE", model.getCode(), null, Boolean.TRUE)
                    .map(notification -> model);
        });
    }
}
