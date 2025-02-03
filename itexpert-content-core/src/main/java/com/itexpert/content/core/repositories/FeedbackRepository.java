package com.itexpert.content.core.repositories;

import com.itexpert.content.lib.entities.Feedback;
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
public interface FeedbackRepository extends ReactiveMongoRepository<Feedback, UUID> {
    Flux<Feedback> findByUserId(String userId);

    Flux<Feedback> findByContentCode(String contentCode);

    Flux<Feedback> findByEvaluation(int evaluation);

    Flux<Feedback> findByVerified(boolean verified);

    Mono<Long> countByEvaluation(int evaluation);

    Mono<Feedback> findFirstByContentCodeOrderByEvaluationDesc(String contentCode);


    @Query(value = "{contentCode: ?0}", count = true)
    Mono<Long> countByContentCode(String ContentCode);

    @Query(value = "{contentCode: ?0, verified: true}", count = true)
    Mono<Long> countVerifiedByContentCode(String ContentCode);

    @Query(value = "{contentCode: ?0, verified: false}", count = true)
    Mono<Long> countNotVerifiedByContentCode(String ContentCode);


    @Query(value = "{}", count = true)
    Mono<Long> countAll();

    @Query(value = "{verified: true}", count = true)
    Mono<Long> countAllVerified();

    @Query(value = "{verified: false}", count = true)
    Mono<Long> countAllNotVerified();


    @Query(value = "{contentCode: { $in :  ?0}}", count = true)
    Mono<Long> countAllByContentCodes(List<String> contentCodes);

    @Query(value = "{contentCode: { $in :  ?0}, verified: true}", count = true)
    Mono<Long> countAllVerifiedByContentCodes(List<String> contentCodes);

    @Query(value = "{contentCode: { $in :  ?0}, verified: false}", count = true)
    Mono<Long> countAllNotVerifiedByContentCodes(List<String> contentCodes);


    @Aggregation(pipeline = {
            "{ $group: { _id: '$evaluation' } }"
    })
    Flux<Long> findDistinctEvaluations();

    @Aggregation(pipeline = {
            "{ $match: { contentCode: { $in: ?0 } } }", // Filtre les documents par les contentCodes donnés
            "{ $group: { _id: '$evaluation' } }"      // Groupe les documents par evaluation
    })
    Flux<Long> findDistinctEvaluationsByContentCode(List<String> contentCodes);

    @Query(value = "{evaluation:  ?0}", count = true)
    Mono<Long> countAllByEvaluation(int evaluation);

    @Query(value = "{evaluation:  ?0, verified:  true}", count = true)
    Mono<Long> countAllVerifiedsByEvaluation(int evaluation);

    @Query(value = "{evaluation:  ?0, verified:  false}", count = true)
    Mono<Long> countAllNotVerifiedsByEvaluation(int evaluation);
}
