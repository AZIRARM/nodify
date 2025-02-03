package com.itexpert.content.core.repositories;

import com.itexpert.content.lib.entities.Notification;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.awt.print.Pageable;
import java.util.UUID;

@Repository
public interface NotificationRepository extends ReactiveMongoRepository<Notification, UUID> {
    @Query(value = "{readers:{ $nin: [?0] }}")
    Flux<Notification> unreaderByUserId(String userId, PageRequest pageable);

    @Query(value = "{readers:{ $in: [?0] }}")
    Flux<Notification> readerByUserId(String userId, PageRequest pageable);

    @Query(value = "{readers:{ $nin: [?0] }}")
    Flux<Notification> findAllUnreadedByUserId(String userId);


    @Query(value = "{readers:{ $nin: [?0] }}", count = true)
    Mono<Long> countUnreadedByUserId(String userId);

    @Query(value = "{readers:{ $in: [?0] }}", count = true)
    Mono<Long> countReadedByUserId(String userId);
}
