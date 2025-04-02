package com.itexpert.content.core.repositories;

import com.itexpert.content.lib.entities.UserRole;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserRoleRepository extends ReactiveMongoRepository<UserRole, UUID> {
   Mono<UserRole> findByCode(String code);

    @Query("{  code : { $in :  ?0  } }")
    Flux<UserRole> findByCodes(List<String> codes);
}
