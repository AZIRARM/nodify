package com.itexpert.content.core.repositories;

import com.itexpert.content.lib.entities.Environment;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface EnvironmentRepository extends ReactiveMongoRepository<Environment, UUID> {

    Mono<Environment> findByCode(String code);

    Mono<Boolean> deleteByCode(String code);

    Mono<Environment> findByNodeCode(String nodeCode);
}
