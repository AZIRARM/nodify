package com.itexpert.content.core.handlers;

import com.itexpert.content.core.mappers.NotificationMapper;
import com.itexpert.content.core.repositories.UserRepository;
import com.itexpert.content.lib.enums.NotificationEnum;
import com.itexpert.content.lib.models.Notification;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Service
@AllArgsConstructor
@Slf4j
public class NotificationHandler {

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final NotificationMapper notificationMapper;
    private final UserRepository userRepository;

    private String keyForUser(String user) {
        return "NOTIFICATIONS:" + user;
    }

    /**
     * Crée une nouvelle notification
     */
    public Mono<Notification> create(NotificationEnum type,
                                     String description,
                                     String user,
                                     String elementType,
                                     String typeCode,
                                     String typeVersion,
                                     Boolean forAll) {


        if (forAll) {
            return this.userRepository.findAll()
                    .filter(userPost -> !userPost.getEmail().equals(user))
                    .flatMap(userPost -> createNotificationFactory(type, description, userPost.getEmail(), elementType, typeCode, typeVersion))
                    .flatMap(this::save)
                    .collectList()
                    .then(this.createNotificationFactory(type, description, user, elementType, typeCode, typeVersion));
        } else {

            return this.createNotificationFactory(type, description, user, elementType, typeCode, typeVersion)
                    .flatMap(this::save);
        }

    }

    private Mono<Notification> createNotificationFactory(NotificationEnum type,
                                                         String description,
                                                         String user,
                                                         String elementType,
                                                         String typeCode,
                                                         String typeVersion) {
        Notification notification = Notification.builder()
                .id(UUID.randomUUID())
                .user(user)
                .type(elementType)
                .typeCode(typeCode)
                .typeVersion(typeVersion)
                .code(type.name())
                .date(Instant.now().toEpochMilli())
                .description(description)
                .read(false)
                .build();
        return Mono.just(notification);
    }


    /**
     * Sauvegarde : supprime d'abord toute entrée existante ayant le même id,
     * puis ajoute la nouvelle JSON avec le score = date.
     */
    public Mono<Notification> save(Notification notification) {
        String key = keyForUser(notification.getUser());
        String newJson = notificationMapper.toJson(notification);

        Mono<Long> removedCount = redisTemplate.opsForZSet()
                .range(key, Range.<Long>unbounded())
                .flatMap(oldJson -> {
                    Notification existing;
                    try {
                        existing = notificationMapper.fromJson(oldJson);
                    } catch (Exception ex) {
                        log.warn("save(): failed to parse existing JSON (skip): {}", oldJson, ex);
                        return Mono.empty();
                    }
                    if (existing != null && existing.getId().equals(notification.getId())) {
                        log.debug("save(): removing old JSON for key {} id {}", key, existing.getId());
                        return redisTemplate.opsForZSet().remove(key, oldJson);
                    }
                    return Mono.empty();
                })
                .reduce(0L, Long::sum)
                .defaultIfEmpty(0L);

        return removedCount
                .flatMap(r -> redisTemplate.opsForZSet().add(key, newJson, notification.getDate()))
                .thenReturn(notification)
                .doOnSuccess(n -> log.debug("Saved notification for {} id={}", key, n.getId()));
    }

    /**
     * Notifications non lues avec pagination
     */
    public Flux<Notification> unreadedByUser(String user, int currentPage, int pageSize) {
        String key = keyForUser(user);

        return redisTemplate.opsForZSet()
                .reverseRange(key, Range.unbounded()) // récupérer toutes les notifications
                .flatMap(json -> {
                    try {
                        Notification n = notificationMapper.fromJson(json);
                        return Mono.just(n);
                    } catch (Exception ex) {
                        log.warn("unreadedByUser(): cannot parse JSON (skip): {}", json, ex);
                        return Mono.empty();
                    }
                })
                .filter(n -> !n.isRead())           // ne garder que les non lues
                .skip((long) currentPage * pageSize) // appliquer la page
                .take(pageSize);                     // limiter le nombre d'éléments
    }


    public Mono<Long> countUnreaded(String user) {
        return findAll(user)
                .filter(n -> !n.isRead())
                .count();
    }

    /**
     * Récupère toutes les notifications (du plus récent au plus ancien)
     */
    private Flux<Notification> findAll(String user) {
        String key = keyForUser(user);
        return redisTemplate.opsForZSet()
                .reverseRange(key, Range.<Long>unbounded())
                .flatMap(json -> {
                    try {
                        return Mono.just(notificationMapper.fromJson(json));
                    } catch (Exception ex) {
                        log.warn("findAll(): cannot parse JSON (skip): {}", json, ex);
                        return Mono.empty();
                    }
                });
    }

    public Mono<Notification> markRead(String user, UUID notificationId) {
        return findById(user, notificationId)
                .flatMap(n ->
                        delete(notificationId)
                                .flatMap(deleted -> deleted ? Mono.just(n) : Mono.empty())
                );
    }

    public Flux<Notification> markAllAsRead(String user) {
        return findAll(user)
                .flatMap(n -> delete(n.getId())
                        .flatMap(deleted -> deleted ? Mono.just(n) : Mono.empty())
                );
    }

    /**
     * Récupère une notification par ID
     */
    private Mono<Notification> findById(String user, UUID notificationId) {
        String key = keyForUser(user);
        return redisTemplate.opsForZSet()
                .range(key, Range.<Long>unbounded())
                .flatMap(json -> {
                    try {
                        return Mono.just(notificationMapper.fromJson(json));
                    } catch (Exception ex) {
                        log.warn("findById(): cannot parse JSON (skip): {}", json, ex);
                        return Mono.empty();
                    }
                })
                .filter(n -> n.getId().equals(notificationId))
                .next();
    }

    /**
     * Supprime une notification par ID (parcourt toutes les clés NOTIFICATIONS:*)
     */
    private Mono<Boolean> delete(UUID notificationId) {
        return redisTemplate.keys("NOTIFICATIONS:*")
                .flatMap(key -> redisTemplate.opsForZSet()
                        .range(key, Range.<Long>unbounded())
                        .flatMap(json -> {
                            Notification n;
                            try {
                                n = notificationMapper.fromJson(json);
                            } catch (Exception ex) {
                                log.warn("delete(): cannot parse JSON (skip): {}", json, ex);
                                return Mono.empty();
                            }
                            if (n.getId().equals(notificationId)) {
                                log.debug("delete(): removing json for key {} id {}", key, notificationId);
                                return redisTemplate.opsForZSet().remove(key, json);
                            }
                            return Mono.empty();
                        }))
                .next()
                .map(count -> count > 0)
                .defaultIfEmpty(false);
    }
}
