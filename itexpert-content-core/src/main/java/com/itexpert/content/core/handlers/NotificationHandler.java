package com.itexpert.content.core.handlers;

import com.itexpert.content.core.mappers.NotificationMapper;
import com.itexpert.content.lib.enums.NotificationEnum;
import com.itexpert.content.lib.models.Notification;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
@Slf4j
public class NotificationHandler {

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final NotificationMapper notificationMapper;

    private String keyForUser(UUID userId) {
        return "NOTIFICATIONS:" + userId.toString();
    }

    /** Récupère toutes les notifications pour un utilisateur */
    public Flux<Notification> findAll(UUID userId) {
        return redisTemplate.opsForZSet()
                .reverseRange(keyForUser(userId), Range.unbounded())
                .map(notificationMapper::fromJson);
    }

    /** Crée une nouvelle notification pour un utilisateur */
    public Mono<Notification> create(NotificationEnum type,
                                     String description,
                                     String user,
                                     String elementType,
                                     String typeCode,
                                     String typeVersion) {

        Notification notification = Notification.builder()
                .id(UUID.randomUUID())
                .user(UUID.fromString(user))
                .type(elementType)
                .typeCode(typeCode)
                .typeVersion(typeVersion)
                .code(type.name())
                .date(Instant.now().toEpochMilli())
                .description(description)
                .readers(new ArrayList<>())
                .build();

        return save(notification);
    }

    /** Sauvegarde (ajoute ou met à jour) une notification */
    public Mono<Notification> save(Notification notification) {
        String key = keyForUser(notification.getUser());
        String json = notificationMapper.toJson(notification);

        // Supprimer l'ancienne version pour éviter les doublons
        return redisTemplate.opsForZSet()
                .remove(key, json)
                .then(redisTemplate.opsForZSet().add(key, json, notification.getDate()))
                .thenReturn(notification);
    }

    /** Supprime une notification par ID */
    public Mono<Boolean> delete(UUID notificationId) {
        return redisTemplate.keys("NOTIFICATIONS:*")
                .flatMap(key -> redisTemplate.opsForZSet()
                        .range(key, Range.unbounded())
                        .flatMap(json -> {
                            Notification n = notificationMapper.fromJson(json);
                            if (n.getId().equals(notificationId)) {
                                return redisTemplate.opsForZSet().remove(key, json);
                            }
                            return Mono.empty();
                        })
                )
                .next()
                .map(count -> count > 0)
                .defaultIfEmpty(false);
    }

    /** Compte les notifications non lues pour un utilisateur */
    public Mono<Long> countUnreaded(UUID userId) {
        return findAll(userId)
                .filter(n -> ObjectUtils.isEmpty(n.getReaders()) || !n.getReaders().contains(userId))
                .count();
    }

    /** Compte les notifications lues pour un utilisateur */
    public Mono<Long> countReaded(UUID userId) {
        return findAll(userId)
                .filter(n -> ObjectUtils.isNotEmpty(n.getReaders()) && n.getReaders().contains(userId))
                .count();
    }

    /** Récupère une notification par son ID */
    public Mono<Notification> findById(UUID userId, UUID notificationId) {
        return redisTemplate.opsForZSet()
                .range(keyForUser(userId), Range.unbounded())
                .map(notificationMapper::fromJson)
                .filter(n -> n.getId().equals(notificationId))
                .next();
    }

    /** Notifications non lues avec pagination */
    public Flux<Notification> unreadedByUserId(UUID userId, int currentPage, int pageSize) {
        long start = (long) currentPage * pageSize;
        long end = start + pageSize - 1;

        return redisTemplate.opsForZSet()
                .reverseRange(keyForUser(userId), Range.closed(start, end))
                .map(notificationMapper::fromJson)
                .filter(n -> ObjectUtils.isEmpty(n.getReaders()) || !n.getReaders().contains(userId));
    }

    /** Notifications lues avec pagination */
    public Flux<Notification> readedByUserId(UUID userId, int currentPage, int pageSize) {
        long start = (long) currentPage * pageSize;
        long end = start + pageSize - 1;

        return redisTemplate.opsForZSet()
                .reverseRange(keyForUser(userId), Range.closed(start, end))
                .map(notificationMapper::fromJson)
                .filter(n -> ObjectUtils.isNotEmpty(n.getReaders()) && n.getReaders().contains(userId));
    }

    /** Marque une notification comme lue */
    public Mono<Notification> markRead(UUID userId, UUID notificationId) {
        return findById(userId, notificationId)
                .flatMap(n -> {
                    if (ObjectUtils.isEmpty(n.getReaders())) {
                        n.setReaders(new ArrayList<>(List.of(userId)));
                    } else if (!n.getReaders().contains(userId)) {
                        n.getReaders().add(userId);
                    }
                    return save(n);
                });
    }

    /** Marque une notification comme non lue */
    public Mono<Notification> markUnread(UUID userId, UUID notificationId) {
        return findById(userId, notificationId)
                .flatMap(n -> {
                    if (ObjectUtils.isNotEmpty(n.getReaders())) {
                        n.getReaders().remove(userId);
                    }
                    return save(n);
                });
    }

    /** Marque toutes les notifications comme lues */
    public Flux<Notification> markAllAsRead(UUID userId) {
        return redisTemplate.opsForZSet()
                .range(keyForUser(userId), Range.unbounded())
                .map(notificationMapper::fromJson)
                .flatMap(n -> {
                    if (ObjectUtils.isEmpty(n.getReaders())) {
                        n.setReaders(new ArrayList<>(List.of(userId)));
                    } else if (!n.getReaders().contains(userId)) {
                        n.getReaders().add(userId);
                    }
                    return save(n);
                });
    }
}
