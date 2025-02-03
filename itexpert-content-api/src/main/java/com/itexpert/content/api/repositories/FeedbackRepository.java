package com.itexpert.content.api.repositories;

import com.itexpert.content.lib.entities.Feedback;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface FeedbackRepository extends ReactiveMongoRepository<Feedback, UUID> {
    Flux<Feedback> findByUserId(String userId);
    Flux<Feedback> findByContentCode(String contentCode);
    Flux<Feedback> findByEvaluation(int evaluation);
    Flux<Feedback> findByVerified(boolean verified);
    @Query(value = "{evaluation: ?0}", count = true)
    Mono<Long> countEvaluation(int evaluation);


    @Query(value = "{contentCode: ?0}")
    Mono<Long> countEvaluationForContent(String code);

    @Query(value = "{contentCode: ?0,evaluation: ?1}", count = true)
    Mono<Long> countEvaluationForContentAndEvaluation(String code, String evaluation);



    @Query(value = "{contentCode: ?0,evaluation: ?1}", count = true)
    Mono<Long> countByContentCodeAndEvaluation(String ContentCode, Integer evaluation);

    Flux<Feedback> findFirst100ByContentCodeOrderByEvaluationDesc(String contentCode);

    @Query(value = "{contentCode: ?0}", count = true)
    Flux<Long> countDistinctByEvaluation(String contentCode);


    Mono<Feedback> findFirstByOrderByEvaluationDesc();
}
