package com.itexpert.content.core.repositories;

import com.itexpert.content.lib.entities.UserParameters;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface UserParametersRepository extends ReactiveMongoRepository<UserParameters, UUID> {
    Mono<UserParameters> findByUserId(UUID userId);
}
