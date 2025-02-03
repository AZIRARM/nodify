package com.itexpert.content.core.handlers;

import com.itexpert.content.core.repositories.UserParametersRepository;
import com.itexpert.content.core.mappers.UserParametersMapper;
import com.itexpert.content.lib.models.UserParameters;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@AllArgsConstructor
@Service
public class UserParametersHandler {
    private final UserParametersRepository userParametersRepository;
    private final UserParametersMapper userParametersMapper;


    public Flux<UserParameters> findAll() {
        return  userParametersRepository.findAll().map(userParametersMapper::fromEntity);
    }

    public Mono<UserParameters> findById(UUID uuid) {
        return userParametersRepository.findById(uuid).map(userParametersMapper::fromEntity);
    }

    public Mono<UserParameters> save(UserParameters notification) {
        if (ObjectUtils.isEmpty(notification.getId())) {
            notification.setId(UUID.randomUUID());
        }

        return userParametersRepository.save(userParametersMapper.fromModel(notification)).map(userParametersMapper::fromEntity);
    }


    public Mono<Boolean> delete(UUID uuid) {
        return userParametersRepository.deleteById(uuid)
                .map(unused -> Boolean.TRUE)
                .onErrorReturn(Boolean.FALSE);
    }

    public Mono<UserParameters> findByUserId(UUID userId) {
        return userParametersRepository.findByUserId(userId)
                .switchIfEmpty(userParametersRepository.save(this.getUserParameters(userId)))
                .map(userParametersMapper::fromEntity);
    }

    private com.itexpert.content.lib.entities.UserParameters getUserParameters(UUID userId) {
        com.itexpert.content.lib.entities.UserParameters userParameters = new com.itexpert.content.lib.entities.UserParameters();
        userParameters.setUserId(userId);
        userParameters.setId(UUID.randomUUID());
        return  userParameters;
    }
}

