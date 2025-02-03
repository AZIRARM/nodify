package com.itexpert.content.core.repositories;

import com.itexpert.content.lib.entities.User;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface UserRepository extends ReactiveMongoRepository<User, UUID> {
    Mono<User> findByEmail(String email);
}
