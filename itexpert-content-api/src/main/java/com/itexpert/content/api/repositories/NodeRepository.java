package com.itexpert.content.api.repositories;

import com.itexpert.content.lib.entities.Node;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface NodeRepository extends ReactiveMongoRepository<Node, UUID> {

    @Query("{ 'code' : ?0 }")
    Mono<Node> findByCode(String code);

    @Query("{ 'code' : ?0, 'status': ?1 }")
    Mono<Node> findByCodeAndStatus(String code, String status);

    @Query("{ $or: [  {'code' : ?0},{'parentCodeOrigin' : ?0},{'parentCode' : ?0}], 'status': ?1 }")
    Flux<Node> findAllByCodePatentAndStatus(String code, String status);

    @Query("{ 'codeParent' : ?0, 'status': ?1 }")
    Flux<Node> findChildreensByCodeParent(String code, String status);

    @Query("{ 'codeParent' :{ $exists: false }, 'status': ?0 }")
     Flux<Node> findParentsNodesByStatus(String status);

    @Query("{  $or:  [ {'code' : ?0},{'parentCode' : ?0} ], status:  ?1}")
    Flux<Node> findByCodeOrCodeParentAndStatus(String codeParent, String status);

}
