package com.itexpert.content.core.repositories;

import com.itexpert.content.lib.entities.ContentNode;
import com.itexpert.content.lib.entities.Node;
import com.itexpert.content.lib.enums.StatusEnum;
import org.reactivestreams.Publisher;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@Repository
public interface ContentNodeRepository extends ReactiveMongoRepository<ContentNode, UUID> {

    @Query("{parentCode: ?0, status:  ?1}")
    Flux<ContentNode> findByNodeCodeAndStatus(String code, String status);


    @Query("{status:  ?0}")
    Flux<ContentNode> findAllByStatus(String status);


    @Query("{parentCode:  ?0}")
    Flux<ContentNode> findByNodeCode(String code);

    @Query("{code:  ?0}")
    Flux<ContentNode> findAllByCode(String code);

    @Query("{code: ?0, status:  ?1}")
    Mono<ContentNode> findByCodeAndStatus(String code, String status);

    @Query("{ 'code' : ?0, 'version': ?1}")
    Mono<ContentNode>  findByCodeAndVersion(String code, String version);

    Mono<Long> countDistinctByParentCode(String code);

    @Query("{_id: ?0, status:  ?1}")
    Mono<ContentNode> findByIdAndStatus(UUID contentNodeUuid, StatusEnum statusEnum);

    Flux<ContentNode> findBySlugAndStatusAndCodeNotIn(String slug, String status, List<String> code);

    Mono<Boolean> existsBySlug(String slug);

    Flux<ContentNode> findBySlugAndCode(String slug, String code);
}
