package com.itexpert.content.core.repositories;

import com.itexpert.content.lib.entities.Node;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@Repository
public interface NodeRepository extends ReactiveMongoRepository<Node, UUID> {
    @Query("{ 'parentCode' : {$exists:  false}, 'status': ?0 }")
    Flux<Node> findParentsNodesByStatus(String status);

    @Query("{ 'parentCode' : ?0, 'status': ?1}")
    Flux<Node> findChildrenByCodeAndStatus(String code, String status);

    @Query("{ 'parentCode' : ?0}")
    Flux<Node> findByCodeParent(String code);

    @Query("{ 'code' : ?0, 'status': ?1 }")
    Mono<Node> findByCodeAndStatus(String code, String status);

    @Query("{  $or:  [ {'code' : ?0},{'parentCodeOrigin' : ?0} ] ,'status': ?1}")
    Flux<Node> findByCodeParentOriginAndStatus(String codeParentOrigin, String status);

    @Query("{  $or:  [ {'code' : ?0},{'parentCodeOrigin' : ?0} ]}")
    Flux<Node> findByCodeOrCodeParentOrigin(String codeParentOrigin);

    @Query("{  $or:  [ {'code' : ?0},{'parentCode' : ?0} ]}")
    Flux<Node> findByCodeOrCodeParent(String codeParent);


    @Query("{  $or:  [ {'code' : ?0},{'parentCode' : ?0} ] ,'status': ?1}")
    Flux<Node> findByCodeParentAndStatus(String codeParent, String status);

    @Query("{ 'code' : ?0}")
    Flux<Node> findByCode(String code);

    @Query("{ 'status': ?0 }")
    Flux<Node> findAllByStatus(String status);


    @Query("{ 'status': ?0, code : { $in :  ?1  } }")
    Flux<Node> findByStatusAndCodes(String status, List<String> codes);

    @Query("{ 'code' : ?0, 'version': ?1}")
    Mono<Node> findByCodeAndVersion(String code, String name);

    Mono<Long> countDistinctByParentCode(String code);

    @Query("{ 'parentCodeOrigin' : null, 'status': 'SNAPSHOT'}")
    Flux<Node> findAllParentOrigin();

    @Query(value = "{ 'code' : {$in:?0}}", delete = true)
    Mono<?> deleteAllByCode(List<String> strings);
}
