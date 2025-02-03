package com.itexpert.content.core.repositories;

import com.itexpert.content.lib.entities.ContentClick;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface ContentClickRepository extends ReactiveMongoRepository<ContentClick, UUID> {
    @Query(value = "{contentCode: ?0}")
    Mono<ContentClick> countClicks(String code);

    Flux<ContentClick> findByContentCode(String code);
}
