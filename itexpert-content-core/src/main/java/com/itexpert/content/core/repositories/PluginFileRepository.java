package com.itexpert.content.core.repositories;

import com.itexpert.content.lib.entities.PluginFile;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface PluginFileRepository extends ReactiveMongoRepository<PluginFile, UUID> {
    Flux<PluginFile> findByPluginId(UUID pluginId);

    Mono<PluginFile> findByPluginIdAndFileName(UUID pluginId, String fileName);

    Flux<PluginFile> deleteAllByPluginId(UUID id);
}
