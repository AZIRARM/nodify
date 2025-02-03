package com.itexpert.content.api.repositories;

import com.itexpert.content.lib.entities.ContentDisplay;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface ContentDisplayRepository extends ReactiveMongoRepository<ContentDisplay, UUID> {
    @Query(value = "{contentCode: ?0}")
    Mono<ContentDisplay> findByContentCode(String code);
}
