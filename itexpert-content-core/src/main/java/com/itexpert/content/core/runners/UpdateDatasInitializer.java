package com.itexpert.content.core.runners;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.itexpert.content.core.entities.ChangeLog;
import com.itexpert.content.core.handlers.NodeHandler;
import com.itexpert.content.core.handlers.UserHandler;
import com.itexpert.content.core.repositories.ChangeLogRepository;
import com.itexpert.content.lib.enums.StatusEnum;
import com.itexpert.content.lib.models.Node;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@AllArgsConstructor
public class UpdateDatasInitializer {

    private final UserHandler userHandler;
    private final ChangeLogRepository changeLogRepository;

    private static final String SCRIPT_NAME = "INIT_USER_VALIDATED_FIELD";

    public Mono<Void> init() {
        return changeLogRepository.findByName(SCRIPT_NAME)
                .flatMap(existingLog -> {
                    log.info("Script {} already executed. Skipping.", SCRIPT_NAME);
                    return Mono.empty();
                })
                .switchIfEmpty(executeMigration())
                .then();
    }

    private Mono<ChangeLog> executeMigration() {
        log.info("Starting migration: {}", SCRIPT_NAME);

        return userHandler.findAll()
                .flatMap(userPost -> {
                    userPost.setValidated(true);
                    return userHandler.save(userPost);
                })
                .then(saveChangeLog());
    }

    private Mono<ChangeLog> saveChangeLog() {
        ChangeLog changeLog = new ChangeLog();
        changeLog.setCreated(Instant.now());
        changeLog.setName(SCRIPT_NAME);
        changeLog.setDescription("Init new field validated with true");

        return changeLogRepository.save(changeLog);
    }
}