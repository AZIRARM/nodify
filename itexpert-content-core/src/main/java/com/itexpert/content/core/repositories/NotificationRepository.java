package com.itexpert.content.core.repositories;

import com.itexpert.content.lib.entities.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface NotificationRepository extends ReactiveMongoRepository<Notification, UUID> {

    @Query("{ $and: [ " +
            "{ $or: [ { 'userId': ?0 }, { 'userId': null }, { 'userId': '' } ] }, " +
            "{ 'readers': { $ne: ?0 } } " +
            "] }")
    Flux<Notification> findUnreadByUser(String user, Pageable pageable);

    @Query(value = "{ $and: [ " +
            "{ $or: [ { 'userId': ?0 }, { 'userId': null }, { 'userId': '' } ] }, " +
            "{ 'readers': { $ne: ?0 } } " +
            "] }", count = true)
    Mono<Long> countUnreadByUser(String user);

    @Query("{ $or: [ { 'userId': ?0 }, { 'userId': null }, { 'userId': '' } ] }")
    Flux<Notification> findAllForUser(String user);

    Mono<Long> countByUserId(String userId);

    @Query("{ 'userId': ?0 }")
    Flux<Notification> findOldestByUserId(String userId, Pageable pageable);

    Flux<Notification> findTopByOrderByDateAsc(Pageable pageable);

    default Flux<Notification> findOldestNotifications(Pageable pageable) {
        return findTopByOrderByDateAsc(pageable);
    }
}