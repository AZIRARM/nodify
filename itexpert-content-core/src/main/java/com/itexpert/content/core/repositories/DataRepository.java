package com.itexpert.content.core.repositories;

import com.itexpert.content.lib.entities.Data;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface DataRepository extends ReactiveMongoRepository<Data, UUID> {
    Flux<Data> findByContentNodeCode(String code, Pageable pageable);

    Mono<Data> findByKey(String key);

    Mono<Boolean> deleteAllByContentNodeCode(String contentNodeCode);

    Mono<Long> countByContentNodeCode(String code);
}
