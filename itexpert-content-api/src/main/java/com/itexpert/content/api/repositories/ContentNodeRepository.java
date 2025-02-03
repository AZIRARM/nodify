package com.itexpert.content.api.repositories;

import com.itexpert.content.lib.entities.ContentNode;
import com.itexpert.content.lib.entities.Node;
import com.itexpert.content.lib.enums.StatusEnum;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.stream.IntStream;

@Repository
public interface ContentNodeRepository extends ReactiveMongoRepository<ContentNode, UUID> {
    Mono<ContentNode> findByCode(String code);

    @Query("{ 'code' : ?0, 'status': ?1 }")
    Mono<ContentNode> findByCodeAndStatus(String code, String status);

    @Query("{ 'parentCode' : ?0, 'status': ?1 }")
    Flux<ContentNode> findAllByNodeCodeAndStatus(String nodeCope, String status);

    @Query("{ 'status': ?0 }")
    Flux<ContentNode> findAllByStatus(String status);
}
