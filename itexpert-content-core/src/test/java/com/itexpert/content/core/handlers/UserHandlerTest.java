package com.itexpert.content.core.handlers;

import com.itexpert.content.core.mappers.UserMapper;
import com.itexpert.content.core.repositories.UserRepository;
import com.itexpert.content.core.utils.auth.PBKDF2Encoder;
import com.itexpert.content.lib.models.Node;
import com.itexpert.content.lib.entities.User;
import com.itexpert.content.lib.models.Notification;
import com.itexpert.content.lib.models.UserPost;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class UserHandlerTest {
    private UserRepository userRepository;
    private UserMapper userMapper;
    private PBKDF2Encoder passwordEncoder;
    private NotificationHandler notificationHandler;
    private NodeHandler nodeHandler;

    private UserHandler userHandler;

    @BeforeEach
    void setup() {
        passwordEncoder = mock(PBKDF2Encoder.class);
        userMapper = Mappers.getMapper(UserMapper.class);
        userRepository = mock(UserRepository.class);
        notificationHandler = mock(NotificationHandler.class);
        nodeHandler = mock(NodeHandler.class);
        userHandler = new UserHandler(
                userRepository,
                userMapper,
                passwordEncoder,
                notificationHandler,
                nodeHandler);

        Notification notification = Notification.builder()
                .id(UUID.randomUUID())
                .user("Admin")
                .type("USER_ACTIONS")
                .build();

        when(notificationHandler.create(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Mono.just(notification));

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
                    assert !aBoolean;
                })
                .verifyComplete();

        verify(userRepository, times(0)).deleteById(any(UUID.class));
    }

    @Test
    void subscribe_Success_ShouldCreateUserAndNode() {
        UserPost userPost = new UserPost();
        userPost.setEmail("test@example.com");
        userPost.setFirstname("John");
        userPost.setLastname("Doe");

        User entity = new User();
        entity.setEmail(userPost.getEmail());
        entity.setFirstname(userPost.getFirstname());
        entity.setLastname(userPost.getLastname());
        entity.setId(UUID.randomUUID());

        Node node = new Node();
        node.setCode("code");

        when(userRepository.findByEmail(anyString())).thenReturn(Mono.empty());
        when(userRepository.findAll()).thenReturn(Flux.empty());
        when(userRepository.findById(any(UUID.class))).thenReturn(Mono.just(entity));
        when(userRepository.save(any(User.class))).thenReturn(Mono.just(entity));
        when(nodeHandler.save(any(Node.class))).thenReturn(Mono.just(node));
        when(passwordEncoder.encode(any())).thenReturn("encoded_password");

        StepVerifier.create(userHandler.subscribe(userPost))
                .expectNextMatches(savedUser -> savedUser.getEmail().equals(userPost.getEmail()))
                .verifyComplete();

        verify(userRepository, times(2)).save(any(User.class));
        verify(nodeHandler).save(any(Node.class));
    }

    @Test
    void subscribe_AlreadyExists_ShouldReturnError() {
        UserPost userPost = new UserPost();
        userPost.setEmail("existing@example.com");

        User entity = new User();
        entity.setEmail(userPost.getEmail());
        entity.setId(UUID.randomUUID());

        when(userRepository.findAll()).thenReturn(Flux.just(entity));
        when(userRepository.findByEmail(userPost.getEmail())).thenReturn(Mono.just(entity));

        StepVerifier.create(userHandler.subscribe(userPost))
                .expectError(RuntimeException.class)
                .verify();

        verify(userRepository, never()).save(any());
        verify(nodeHandler, never()).save(any());
    }
}
