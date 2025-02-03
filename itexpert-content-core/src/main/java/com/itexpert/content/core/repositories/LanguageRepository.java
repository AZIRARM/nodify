package com.itexpert.content.core.repositories;

import com.itexpert.content.lib.entities.Language;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@Repository
public interface LanguageRepository extends ReactiveMongoRepository<Language, UUID> {
    @Query(value = "{code:  {$in: ?0}}")
    Flux<Language> findInArray(List<String> languages);

    Mono<Language> findByCode(String code);
}
