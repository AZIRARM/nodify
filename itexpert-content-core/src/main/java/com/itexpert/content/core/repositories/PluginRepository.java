package com.itexpert.content.core.repositories;

import com.itexpert.content.lib.entities.Plugin;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PluginRepository extends ReactiveMongoRepository<Plugin, UUID> {
    Mono<Plugin> findByName(String code);

    Flux<Plugin> findByEnabled(Boolean enabled);

    @Query("{deleted: true}")
    Flux<Plugin> findDeleted();

    Flux<Plugin> findByEnabledAndDeleted(boolean enabled, boolean deleted);

    @Query("{deleted: false}")
    Flux<Plugin> findNotDeleted();
}
