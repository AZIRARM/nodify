package com.itexpert.content.core.handlers;

import com.itexpert.content.core.mappers.UserMapper;
import com.itexpert.content.core.repositories.UserRepository;
import com.itexpert.content.core.utils.auth.PBKDF2Encoder;
import com.itexpert.content.lib.entities.User;
import com.itexpert.content.lib.models.Notification;
import com.itexpert.content.lib.models.UserPost;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

public class UserHandlerTest {
    private UserRepository userRepository;
    private UserMapper userMapper;
    private PBKDF2Encoder passwordEncoder;
    private NotificationHandler notificationHandler;

    private UserHandler userHandler;

    @BeforeEach
    void setup() {
        passwordEncoder = mock(PBKDF2Encoder.class);
        userMapper = Mappers.getMapper(UserMapper.class);
        userRepository = mock(UserRepository.class);
        notificationHandler = mock(NotificationHandler.class);
        userHandler = new UserHandler(
                userRepository,
                userMapper,
                passwordEncoder,
                notificationHandler
        );

        Notification notification = Notification.builder()
                .id(UUID.randomUUID())
                .user("Admin")
                .type("USER_ACTIONS")
                .build();

        when(notificationHandler.create(any(), any(), any(), any(), any(), any())).thenReturn(Mono.just(notification));

    }

     @Test
    void delete_ShouldReturnTrue_WhenUserIsNotAdmin() {
        UUID userId = UUID.randomUUID();
        User normalUser = new User();
        normalUser.setId(userId);
        normalUser.setRoles(List.of("USER"));

        when(userRepository.findById(userId)).thenReturn(Mono.just(normalUser));
        when(userRepository.deleteById(userId)).thenReturn(Mono.empty());

        Mono<Boolean> result = userHandler.delete(userId);

        StepVerifier.create(result)
                .assertNext(aBoolean -> {
                    assert aBoolean;
                })
                .verifyComplete();

        verify(userRepository).deleteById(userId);
    }

    @Test
    void delete_ShouldReturnFalseWhenIsAdminUser() {
        UUID userId = UUID.randomUUID();
        User normalUser = new User();
        normalUser.setId(userId);
        normalUser.setRoles(List.of("ADMIN"));

        when(userRepository.findById(userId)).thenReturn(Mono.just(normalUser));

        Mono<Boolean> result = userHandler.delete(userId);

        StepVerifier.create(result)
                .assertNext(aBoolean -> {
                    assert !aBoolean
;                })
                .verifyComplete();

        verify(userRepository, times(0)).deleteById(any(UUID.class));
    }
}
