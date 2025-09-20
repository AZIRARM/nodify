package com.itexpert.content.core.repositories;

import com.itexpert.content.lib.entities.ResourceParameter;
import com.itexpert.content.lib.enums.ResourceActionEnum;
import com.itexpert.content.lib.enums.ResourceTypeEnum;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface ResourceParamaterRepository extends ReactiveMongoRepository<ResourceParameter, UUID> {
    Flux<ResourceParameter> findByCode(String code);

    Mono<ResourceParameter> findByCodeAndAction(String code, ResourceActionEnum action);

    Flux<ResourceParameter> findByTypeAndAction(ResourceTypeEnum type, ResourceActionEnum action);
}
