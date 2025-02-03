package com.itexpert.content.core.repositories;

import com.itexpert.content.lib.entities.AccessRole;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface AccessRoleRepository extends ReactiveMongoRepository<AccessRole, UUID> {
    Mono<AccessRole> findByCode(String code);
}
