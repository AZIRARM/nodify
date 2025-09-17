package com.itexpert.content.core.handlers;

import com.itexpert.content.core.mappers.UserRoleMapper;
import com.itexpert.content.core.repositories.UserRoleRepository;
import com.itexpert.content.lib.enums.NotificationEnum;
import com.itexpert.content.lib.models.UserRole;
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
public class UserRoleHandler {
    private final UserRoleRepository userRoleRepository;
    private final UserRoleMapper userRoleMapper;
    private final NotificationHandler notificationHandler;

    public Flux<UserRole> findAll() {
        return userRoleRepository.findAll().map(role -> {
            return userRoleMapper.fromEntity(role);
        });
    }

    public Mono<UserRole> findById(UUID uuid) {
        return userRoleRepository.findById(uuid).map(userRoleMapper::fromEntity);
    }

    public Mono<UserRole> save(UserRole role) {
        if (ObjectUtils.isEmpty(role.getId())) {
            role.setId(UUID.randomUUID());
        }
        return userRoleRepository.findByCode(role.getCode())
                .switchIfEmpty(userRoleRepository.save(userRoleMapper.fromModel(role)))
                .map(userRoleMapper::fromEntity)
                .flatMap(node -> this.notify(node, NotificationEnum.CREATION_OR_UPDATE));
    }

    public Flux<UserRole> saveAll(List<UserRole> roles) {
        return Flux.fromIterable(roles).flatMap(this::save);
    }

    public Mono<Boolean> delete(UUID uuid) {
        return this.userRoleRepository.findById(uuid)
                .flatMap(entity ->
                        this.notify(this.userRoleMapper.fromEntity(entity), NotificationEnum.DELETION)
                                .flatMap(notification ->
                                        this.userRoleRepository.deleteById(uuid)
                                                .thenReturn(Boolean.TRUE)
                                )
                                .onErrorReturn(Boolean.FALSE)
                );
    }

    public Mono<UserRole> findByCode(String code) {
        return userRoleRepository.findByCode(code)
                .map(userRoleMapper::fromEntity);
    }

    public Flux<UserRole> findByCodes(List<String> codes) {
        return userRoleRepository.findByCodes(codes)
                .map(userRoleMapper::fromEntity);
    }

    public Mono<UserRole> notify(UserRole model, NotificationEnum type) {
        return Mono.just(model).flatMap(user -> {
            return notificationHandler
                    .create(type, user.getCode(), null, "USER_ROLE", model.getCode(), null, Boolean.TRUE)
                    .map(notification -> model);
        });
    }
}

