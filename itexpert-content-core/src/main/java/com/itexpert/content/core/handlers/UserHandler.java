package com.itexpert.content.core.handlers;

import com.itexpert.content.core.mappers.UserMapper;
import com.itexpert.content.core.repositories.UserRepository;
import com.itexpert.content.core.utils.auth.PBKDF2Encoder;
import com.itexpert.content.lib.enums.NotificationEnum;
import com.itexpert.content.lib.models.UserPassword;
import com.itexpert.content.lib.models.UserPost;
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
public class UserHandler {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PBKDF2Encoder passwordEncoder;
    private final NotificationHandler notificationHandler;

    public Flux<UserPost> findAll() {
        return userRepository.findAll().map(userMapper::fromEntity);
    }

    public Mono<UserPost> findById(UUID uuid) {
        return userRepository.findById(uuid).switchIfEmpty(Mono.empty()).map(userMapper::fromEntity);
    }

    public Mono<UserPost> save(UserPost user) {
        if (ObjectUtils.isEmpty(user.getId())) {
            user.setId(UUID.randomUUID());
            user.setPassword(passwordEncoder.encode(user.getPassword()));

            return userRepository.findByEmail(user.getEmail())
                    .switchIfEmpty(
                            this.findAll().hasElements()
                                    .doOnNext(aBoolean -> {
                                        log.info("Have elements {}", aBoolean);
                                    })
                                    .filter(exists -> exists)
                                    .map(exists -> userRepository.save(userMapper.fromModel(user)))
                                    .switchIfEmpty(
                                            Mono.just(user)
                                                    .doOnNext(element -> {
                                                        log.info("Have elements {}", element);
                                                    })
                                                    .map(userMapper::fromModel)
                                                    .map(this.userRepository::save)
                                                    .onErrorContinue((throwable, o) -> log.error(throwable.getMessage()))

                                    ).flatMap(Mono::from)
                    )
                    .map(userMapper::fromEntity)
                    .flatMap(model -> this.notify(model, NotificationEnum.CREATION_OR_UPDATE, Boolean.TRUE));
        } else {
            return userRepository.findById(user.getId())
                    .filter(userDb -> userDb.getEmail().toUpperCase().equals(user.getEmail().toUpperCase()))
                    .map(userDb -> userMapper.fromModel(user))
                    .flatMap(userRepository::save)
                    .map(userMapper::fromEntity)
                    .flatMap(model -> this.notify(model, NotificationEnum.CREATION_OR_UPDATE, Boolean.TRUE));
        }

    }


    public Mono<Boolean> delete(UUID uuid) {
        return this.userRepository.findById(uuid)
                .flatMap(entity -> {
                    if (entity.getRoles() != null && entity.getRoles().contains("ADMIN")) {
                        return Mono.just(Boolean.FALSE);
                    }
                    return this.notify(this.userMapper.fromEntity(entity), NotificationEnum.DELETION, Boolean.TRUE)
                            .flatMap(notification ->
                                    this.userRepository.deleteById(uuid)
                                            .thenReturn(Boolean.TRUE)
                            )
                            .onErrorReturn(Boolean.FALSE);
                });
    }


    public Mono<UserPost> findByEmail(String username) {
        return userRepository.findByEmail(username).map(userMapper::fromEntity);
    }

    public Mono<Boolean> changePassword(UserPassword userPassword) {
        return userRepository.findById(UUID.fromString(userPassword.getUserId()))
                .map(user -> {
                    if (passwordEncoder.encode(user.getPassword()).equals(
                            passwordEncoder.encode(user.getPassword()))) {
                        user.setPassword(passwordEncoder.encode(userPassword.getNewPassword()));
                        return userRepository.save(user)
                                .map(this.userMapper::fromEntity)
                                .flatMap(userPost -> this.notify(userPost, NotificationEnum.PASSWORD_CHANGE, Boolean.FALSE))
                                .map(userBDD -> Boolean.TRUE);
                    }
                    return userRepository.save(user)
                            .map(this.userMapper::fromEntity)
                            .flatMap(userPost -> this.notify(userPost, NotificationEnum.PASSWORD_CHANGE, Boolean.FALSE))
                            .map(userPost -> Boolean.FALSE);
                })
                .flatMap(Mono::from)
                .switchIfEmpty(Mono.just(Boolean.FALSE));
    }


    public Mono<UserPost> notify(UserPost model, NotificationEnum type, Boolean notifyAll) {
        return Mono.just(model).flatMap(user -> {
            return notificationHandler
                    .create(type,
                           "",
                            user.getEmail(),
                            "USER",
                            "",
                            "",
                            notifyAll
                            )
                    .map(notification -> model);
        });
    }
}

