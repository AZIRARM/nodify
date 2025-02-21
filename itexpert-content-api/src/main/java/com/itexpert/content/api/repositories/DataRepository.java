package com.itexpert.content.api.repositories;

import com.itexpert.content.lib.entities.Data;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface DataRepository extends ReactiveMongoRepository<Data, String> {
    Flux<Data> findByContentNodeCode(String code, Pageable pageable);

    Mono<Data> findByKey(String key);

    Mono<Boolean> deleteAllByContentNodeCode(String contentNodeCode);

    Mono<Boolean> deleteByKey(String key);
}
