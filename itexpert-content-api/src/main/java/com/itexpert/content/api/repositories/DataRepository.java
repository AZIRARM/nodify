package com.itexpert.content.api.repositories;

import com.itexpert.content.lib.entities.Data;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DataRepository extends ReactiveMongoRepository<Data, UUID> {
    Flux<Data> findByContentNodeCode(String code, Pageable pageable);

    Mono<Data> findByKey(String key);

    Mono<Boolean> deleteAllByContentNodeCode(String contentNodeCode);

    Mono<Data>  findByContentNodeCodeAndKey(String code, String key);

    Flux<Data>  findByContentNodeCodeAndName(String code, String name);

    Mono<Long> countByContentNodeCode(String code);

    Mono<Long> countByContentNodeCodeAndDataType(String code, String dataType);

    Mono<Long> countByContentNodeCodeAndUser(String code, String user);

    Mono<Long> countByDataType(String dataType);

    Mono<Long> countByUser(String user);

    Mono<Boolean> deleteByContentNodeCodeAndKey(String code, String key);

    Flux<Data> findByDataType(String dataType, Pageable pageable);

    Flux<Data> findAllBy(Pageable pageable);

    Flux<Data> findByUser(String user, Pageable pageable);

    @Query("{ 'contentNodeCode': ?0, '$or': [ { 'name': { $regex: ?1, $options: 'i' } }, { 'value': { $regex: ?1, $options: 'i' } }, { 'key': { $regex: ?1, $options: 'i' } } ] }")
    Flux<Data> searchByContentNodeCodeAndKeyword(String code, String keyword, Pageable pageable);
}
