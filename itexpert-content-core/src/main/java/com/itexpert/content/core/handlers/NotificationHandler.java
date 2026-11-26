package com.itexpert.content.core.handlers;

import com.itexpert.content.core.mappers.NotificationMapper;
import com.itexpert.content.core.repositories.NotificationRepository;
import com.itexpert.content.lib.enums.NotificationEnum;
import com.itexpert.content.lib.models.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationHandler {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper mapper;

    public Mono<Notification> create(NotificationEnum type,
                                     String description,
                                     String user,
                                     String elementType,
                                     String typeCode,
                                     String typeVersion,
                                     Boolean forAll) {
        boolean isForAll = Boolean.TRUE.equals(forAll);
        Notification model = Notification.builder()
                .id(UUID.randomUUID())
                .user(user)
                .modifiedBy(user)
                .type(elementType)
                .typeCode(typeCode)
                .typeVersion(typeVersion)
                .code(type.name())
                .date(Instant.now().toEpochMilli())
                .description(description)
                .read(false)
                .build();

        com.itexpert.content.lib.entities.Notification entity = mapper.toEntity(model, isForAll);
        return notificationRepository.save(entity)
                .map(saved -> mapper.toModel(saved, user));
    }

    public Mono<Notification> save(Notification notification) {
        boolean isForAll = notification.getUser() == null || notification.getUser().isBlank();
        return notificationRepository.findById(notification.getId())
                .map(existing -> {
                    List<String> readers = existing.getReaders() != null
                            ? new ArrayList<>(existing.getReaders())
                            : new ArrayList<>();
                    if (notification.isRead() && notification.getUser() != null && !readers.contains(notification.getUser())) {
                        readers.add(notification.getUser());
                    }
                    return mapper.toEntity(notification, isForAll, readers);
                })
                .defaultIfEmpty(mapper.toEntity(notification, isForAll))
                .flatMap(notificationRepository::save)
                .map(saved -> mapper.toModel(saved, notification.getUser()));
    }

    public Flux<Notification> unreadedByUser(String user, int currentPage, int pageSize) {
        int safePage = Math.max(0, currentPage);
        int safeSize = Math.max(1, pageSize);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "date"));

        return notificationRepository.findUnreadByUser(user, pageable)
                .map(entity -> mapper.toModel(entity, user));
    }

    public Mono<Long> countUnreaded(String user) {
        return notificationRepository.countUnreadByUser(user);
    }

    public Mono<Notification> markRead(String user, UUID notificationId) {
        return notificationRepository.findById(notificationId)
                .flatMap(entity -> {
                    List<String> readers = entity.getReaders() != null
                            ? new ArrayList<>(entity.getReaders())
                            : new ArrayList<>();
                    if (!readers.contains(user)) {
                        readers.add(user);
                        entity.setReaders(readers);
                        return notificationRepository.save(entity);
                    }
                    return Mono.just(entity);
                })
                .map(saved -> mapper.toModel(saved, user));
    }

    public Flux<Notification> markAllAsRead(String user) {
        return notificationRepository.findAllForUser(user)
                .flatMap(entity -> {
                    List<String> readers = entity.getReaders() != null
                            ? new ArrayList<>(entity.getReaders())
                            : new ArrayList<>();
                    if (!readers.contains(user)) {
                        readers.add(user);
                        entity.setReaders(readers);
                        return notificationRepository.save(entity);
                    }
                    return Mono.just(entity);
                })
                .map(saved -> mapper.toModel(saved, user));
    }
}