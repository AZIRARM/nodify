package com.itexpert.content.core.repositories;

import com.itexpert.content.lib.entities.Parameter;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface ParameterRepository extends ReactiveMongoRepository<Parameter, UUID> {
    Mono<Parameter> findByKey(String key);
}
