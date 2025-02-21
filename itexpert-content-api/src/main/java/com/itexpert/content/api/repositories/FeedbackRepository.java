package com.itexpert.content.api.repositories;

import com.itexpert.content.lib.entities.Feedback;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface FeedbackRepository extends ReactiveMongoRepository<Feedback, UUID> {
    Flux<Feedback> findByUserId(String userId, Pageable pageable);

    Flux<Feedback> findByContentCode(String contentCode, Pageable pageable);

    Flux<Feedback> findByEvaluation(int evaluation, Pageable pageable);

    Flux<Feedback> findByVerified(boolean verified, Pageable pageable);

    @Query(value = "{contentCode: ?0,evaluation: ?1}", count = true)
    Mono<Long> countByContentCodeAndEvaluation(String ContentCode, Integer evaluation);

    Mono<Feedback> findFirstByOrderByEvaluationDesc();

    Mono<Boolean> deleteAllByContentCode(String contentCode);
}
