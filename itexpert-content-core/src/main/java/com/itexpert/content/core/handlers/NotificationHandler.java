package com.itexpert.content.core.handlers;

import com.itexpert.content.core.mappers.NotificationMapper;
import com.itexpert.content.core.repositories.NotificationRepository;
import com.itexpert.content.lib.enums.NotificationEnum;
import com.itexpert.content.lib.models.Notification;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.jaxb.SpringDataJaxb;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@AllArgsConstructor
public class NotificationHandler {
    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;

    public Flux<Notification> findAll() {
        return notificationRepository.findAll().map(notificationMapper::fromEntity);
    }

    public Mono<Notification> create(NotificationEnum type,
                                     String description,
                                     UUID userId,
                                     String elementType,
                                     String typeCode,
                                     String typeVersion) {


        Notification notification = Notification.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .type(elementType)
                .typeCode(typeCode)
                .typeVersion(typeVersion)
                .code(type.name())
                .date(Instant.now().toEpochMilli())
                .description(description)
                .build();
        return Mono.just(notification)
                .map(notificationMapper::fromModel)
                .flatMap(notificationRepository::save).
                map(notificationMapper::fromEntity);
    }

    public Mono<Notification> findById(UUID uuid) {
        return notificationRepository.findById(uuid).map(notificationMapper::fromEntity);
    }

    public Mono<Notification> save(Notification notification) {
        if (ObjectUtils.isEmpty(notification.getId())) {
            notification.setId(UUID.randomUUID());
        }

        return notificationRepository.save(notificationMapper.fromModel(notification)).map(notificationMapper::fromEntity);
    }


    public Mono<Boolean> delete(UUID uuid) {
        return notificationRepository.deleteById(uuid)
                .map(unused -> Boolean.TRUE)
                .onErrorReturn(Boolean.FALSE);
    }

    public Flux<Notification> unreadedByUserId(String userId, Integer currentPage, Integer pageSize) {
        return notificationRepository.unreaderByUserId(userId,  PageRequest.of(currentPage, pageSize, Sort.by(Sort.Order.by("date")).descending()))
                .map(notificationMapper::fromEntity);
    }


    public Flux<Notification> readedByUserId(String userId, Integer currentPage, Integer pageSize) {
        return notificationRepository.readerByUserId(userId,  PageRequest.of(currentPage, pageSize, Sort.by(Sort.Order.by("date")).descending()))
                .map(notificationMapper::fromEntity);
    }

    public Mono<Notification> markread(UUID notificationId, String userId) {
        return notificationRepository.findById(notificationId)
                .map(notification -> {
                    if (ObjectUtils.isEmpty(notification.getReaders())) {
                        notification.setReaders(List.of(userId));
                    } else {
                        notification.getReaders().add(userId);
                    }
                    return notification;
                }).flatMap(this.notificationRepository::save)
                .map(this.notificationMapper::fromEntity);
    }

    public Mono<Notification> markunread(UUID notificationId, String userId) {
        return notificationRepository.findById(notificationId)
                .map(notification -> {
                    if (ObjectUtils.isNotEmpty(notification.getReaders())) {
                        notification.getReaders().remove(userId);
                    }
                    return notification;
                }).flatMap(this.notificationRepository::save)
                .map(this.notificationMapper::fromEntity);
    }

    public Flux<Notification> markAllAsRead(String userId) {
        return this.notificationRepository.findAllUnreadedByUserId(userId)
                .map(notification -> {
                    if (ObjectUtils.isEmpty(notification.getReaders())) {
                        notification.setReaders(List.of(userId));
                    } else {
                        notification.getReaders().add(userId);
                    }
                    return notification;
                })
                .flatMap(this.notificationRepository::save)
                .map(this.notificationMapper::fromEntity);
    }

    public Mono<Long> countUnreaded(String userId) {
        return this.notificationRepository.countUnreadedByUserId(userId);
    }

    public Mono<Long> countReaded(String userId) {
        return this.notificationRepository.countReadedByUserId(userId);
    }
}

