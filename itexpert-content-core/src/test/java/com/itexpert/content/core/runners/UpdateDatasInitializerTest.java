package com.itexpert.content.core.runners;

import com.itexpert.content.core.entities.ChangeLog;
import com.itexpert.content.core.handlers.UserHandler;
import com.itexpert.content.core.repositories.ChangeLogRepository;
import com.itexpert.content.lib.models.UserPost;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class UpdateDatasInitializerTest {

    private UserHandler userHandler;
    private ChangeLogRepository changeLogRepository;
    private UpdateDatasInitializer updateDatasInitializer;

    private static final String SCRIPT_NAME = "INIT_USER_VALIDATED_FIELD";

    @BeforeEach
    void setUp() {
        userHandler = mock(UserHandler.class);
        changeLogRepository = mock(ChangeLogRepository.class);

        updateDatasInitializer = new UpdateDatasInitializer(userHandler, changeLogRepository);
    }

    @Test
    void init_ShouldSkip_WhenScriptAlreadyExists() {
        ChangeLog existingLog = new ChangeLog();
        existingLog.setName(SCRIPT_NAME);

        when(changeLogRepository.findByName(SCRIPT_NAME)).thenReturn(Mono.just(existingLog));
        when(userHandler.findAll()).thenReturn(Flux.empty());
        when(changeLogRepository.save(any(ChangeLog.class))).thenReturn(Mono.just(new ChangeLog()));

        StepVerifier.create(updateDatasInitializer.init())
                .verifyComplete();

        verify(changeLogRepository).findByName(SCRIPT_NAME);
        verify(userHandler, never()).save(any());
    }

    @Test
    void init_ShouldRunMigration_WhenScriptDoesNotExist() {
        UserPost user = new UserPost();
        user.setEmail("test@itexpert.com");
        user.setValidated(false);

        when(changeLogRepository.findByName(SCRIPT_NAME)).thenReturn(Mono.empty());
        when(userHandler.findAll()).thenReturn(Flux.just(user));
        when(userHandler.save(any(UserPost.class))).thenReturn(Mono.just(user));
        when(changeLogRepository.save(any(ChangeLog.class))).thenReturn(Mono.just(new ChangeLog()));

        StepVerifier.create(updateDatasInitializer.init())
                .verifyComplete();

        verify(changeLogRepository).findByName(SCRIPT_NAME);
        verify(userHandler).findAll();
        verify(userHandler).save(argThat(UserPost::getValidated));
        verify(changeLogRepository).save(argThat(log -> SCRIPT_NAME.equals(log.getName())));
    }
}