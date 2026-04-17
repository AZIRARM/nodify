package com.itexpert.content.core.repositories;

import com.itexpert.content.core.entities.ChangeLog;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface ChangeLogRepository extends ReactiveMongoRepository<ChangeLog, UUID> {
    Mono<ChangeLog> findByName(String name);
}
