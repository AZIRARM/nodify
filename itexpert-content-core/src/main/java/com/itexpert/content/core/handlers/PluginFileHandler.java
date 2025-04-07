package com.itexpert.content.core.handlers;

import com.itexpert.content.core.mappers.PluginFileMapper;
import com.itexpert.content.core.repositories.PluginFileRepository;
import com.itexpert.content.core.repositories.PluginRepository;
import com.itexpert.content.lib.models.PluginFile;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@AllArgsConstructor
@Service
public class PluginFileHandler {
    private final PluginFileMapper pluginFileMapper;
    private final PluginFileRepository pluginFileRepository;
    private final PluginRepository pluginRepository;

    public Flux<PluginFile> findAll() {
        return pluginFileRepository.findAll().map(this.pluginFileMapper::fromEntity);

    }

    public Mono<PluginFile> findById(UUID uuid) {
        return pluginFileRepository.findById(uuid).map(pluginFileMapper::fromEntity);
    }

    public Flux<PluginFile> findByPluginId(UUID pluginId) {
        return pluginFileRepository.findByPluginId(pluginId).map(pluginFileMapper::fromEntity);
    }

    public Flux<PluginFile> findByPluginName(String pluginName) {
        return pluginRepository.findByName(pluginName).flatMapMany(plugin -> this.pluginFileRepository.findByPluginId(plugin.getId())).map(this.pluginFileMapper::fromEntity);
    }

    public Mono<PluginFile> save(PluginFile pluginFile) {
        if (ObjectUtils.isEmpty(pluginFile.getId())) {
            pluginFile.setId(UUID.randomUUID());
        }
        return pluginFileRepository
                .findByPluginIdAndFileName(pluginFile.getPluginId(), pluginFile.getFileName())
                .onErrorMap(error -> new RuntimeException("Duplicate fileName: ", error))
                .switchIfEmpty(
                        Mono.defer(() ->
                                Mono.just(pluginFileMapper.fromModel(pluginFile))
                                        .flatMap(pluginFileRepository::save)
                        )
                )
                .map(pluginFileMapper::fromEntity);

    }

    public Mono<Boolean> delete(UUID uuid) {
        return this.pluginFileRepository.deleteById(uuid).thenReturn(Boolean.TRUE).onErrorReturn(Boolean.FALSE);
    }
}

