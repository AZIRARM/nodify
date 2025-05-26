package com.itexpert.content.api.repositories;

import com.itexpert.content.lib.entities.Plugin;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface PluginRepository extends ReactiveMongoRepository<Plugin, UUID> {
    Mono<Plugin> findByName(String code);
}
