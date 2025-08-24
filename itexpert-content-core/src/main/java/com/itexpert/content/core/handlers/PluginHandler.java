package com.itexpert.content.core.handlers;

import com.itexpert.content.core.mappers.PluginFileMapper;
import com.itexpert.content.core.mappers.PluginMapper;
import com.itexpert.content.core.repositories.PluginFileRepository;
import com.itexpert.content.core.repositories.PluginRepository;
import com.itexpert.content.lib.enums.NotificationEnum;
import com.itexpert.content.lib.models.Plugin;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@AllArgsConstructor
@Service
public class PluginHandler {
    private final PluginRepository pluginRepository;
    private final PluginMapper pluginMapper;
    private final PluginFileMapper pluginFileMapper;
    private final NotificationHandler notificationHandler;
    private final PluginFileRepository pluginFileRepository;
    private final UserHandler userHandler;

    public Flux<Plugin> findAll() {
        return pluginRepository.findAll()
                .map(this.pluginMapper::fromEntity);
    }

    public Flux<Plugin> findNotDeleted() {
        return pluginRepository.findNotDeleted()
                .doOnNext(plugin -> {
                    log.info("Deleting plugin {}", plugin.getName());
                })
                .map(this.pluginMapper::fromEntity);
    }

    public Mono<Plugin> findById(UUID uuid) {
        return pluginRepository.findById(uuid).map(pluginMapper::fromEntity);
    }

    public Mono<Plugin> save(Plugin plugin) {
        if (ObjectUtils.isEmpty(plugin.getId())) {
            plugin.setId(UUID.randomUUID());
            plugin.setCreationDate(Instant.now().toEpochMilli());
            plugin.setEnabled(true);
            plugin.setModificationDate(Instant.now().toEpochMilli());

            return pluginRepository.findByName(plugin.getName())
                    .onErrorMap(error -> new RuntimeException("Duplicate plugin", error))
                    .switchIfEmpty(
                            pluginRepository.save(pluginMapper.fromModel(plugin))
                    )
                    .map(pluginMapper::fromEntity)
                    .flatMap(entity -> this.notify(entity, NotificationEnum.CREATION_OR_UPDATE, entity.getModifiedBy()));

        } else {
            plugin.setModificationDate(Instant.now().toEpochMilli());

            return this.pluginRepository.save(pluginMapper.fromModel(plugin))
                    .map(pluginMapper::fromEntity)
                    .flatMap(entity -> this.notify(entity, NotificationEnum.CREATION_OR_UPDATE, entity.getModifiedBy()));

        }
    }


    public Mono<Boolean> delete(UUID uuid, UUID userId) {
        return this.pluginRepository.findById(uuid)
                .flatMap(entity ->
                        this.notify(this.pluginMapper.fromEntity(entity), NotificationEnum.DELETION, userId)
                                .flatMap(notification -> {
                                            entity.setDeleted(true);
                                            entity.setModifiedBy(userId);
                                            entity.setCreationDate(Instant.now().toEpochMilli());
                                            return this.pluginRepository.save(entity)
                                                    .thenReturn(entity);
                                        }

                                )
                                .flatMapMany(plugin -> this.pluginFileRepository.findByPluginId(entity.getId()))
                                .flatMap(this.pluginFileRepository::delete)
                                .collectList()
                                .thenReturn(Boolean.TRUE))
                .onErrorReturn(Boolean.FALSE);
    }

    public Mono<Plugin> findByName(String name) {
        return pluginRepository.findByName(name)
                .map(pluginMapper::fromEntity);
    }


    public Mono<Plugin> notify(Plugin model, NotificationEnum type, UUID userId) {
        return userHandler.findById(userId)
                .flatMap(userPost -> notificationHandler
                        .create(type, model.getCode(), userPost.getFirstname() + " " + userPost.getLastname() + " " + "(" + userPost.getRoles() + ")", "PLUGIN", model.getName(), null)
                        .map(notification -> model)
                );
    }

    public Mono<Plugin> enable(String name, UUID userId) {
        return pluginRepository.findByName(name).map(plugin -> {
                    plugin.setEnabled(Boolean.TRUE);
                    plugin.setModificationDate(Instant.now().toEpochMilli());
                    plugin.setModifiedBy(userId);
                    return plugin;
                }).flatMap(pluginRepository::save)
                .map(pluginMapper::fromEntity)
                .flatMap(plugin -> this.notify(plugin, NotificationEnum.ACTIVATION, userId));
    }

    public Mono<Plugin> disable(String name, UUID userId) {
        return pluginRepository.findByName(name).map(plugin -> {
                    plugin.setEnabled(Boolean.FALSE);
                    plugin.setModificationDate(Instant.now().toEpochMilli());
                    plugin.setModifiedBy(userId);
                    return plugin;
                }).flatMap(pluginRepository::save)
                .map(pluginMapper::fromEntity)
                .flatMap(plugin -> this.notify(plugin, NotificationEnum.DEACTIVATION, userId));
    }

    public Flux<Plugin> deleteds() {
        return pluginRepository.findDeleted().map(this.pluginMapper::fromEntity);
    }

    public Mono<Boolean> deleteDefinitively(UUID id, UUID userId) {
        return this.findById(id)
                .flatMap(plugin -> this.notify(plugin, NotificationEnum.DELETION_DEFINITIVELY, userId))
                .map(Plugin::getId)
                .flatMap(this.pluginRepository::deleteById)
                .hasElement();
    }

    public Mono<Plugin> activate(String name, UUID userId) {
        return pluginRepository.findByName(name).map(plugin -> {
                    plugin.setDeleted(Boolean.FALSE);
                    plugin.setModificationDate(Instant.now().toEpochMilli());
                    plugin.setModifiedBy(userId);
                    return plugin;
                }).flatMap(pluginRepository::save)
                .map(pluginMapper::fromEntity)
                .flatMap(plugin -> this.notify(plugin, NotificationEnum.REVERT, userId));
    }

    public Mono<Plugin> export(String name) {
        return this.pluginRepository.findByName(name)
                .map(this.pluginMapper::fromEntity)
                .flatMap(plugin -> this.pluginFileRepository.findByPluginId(plugin.getId())
                        .map(this.pluginFileMapper::fromEntity)
                        .collectList()
                        .map(pluginFiles -> {
                            plugin.setResources(pluginFiles);
                            return plugin;
                        })
                );
    }

    public Mono<Plugin> importPlugin(Plugin plugin) {
        return this.pluginRepository.findByName(plugin.getName())
                .flatMap(existingPlugin -> Mono.error(new IllegalStateException("Plugin already exists")))
                .switchIfEmpty(
                        Mono.defer(() -> {
                            var pluginEntity = this.pluginMapper.fromModel(plugin);
                            return this.pluginRepository.save(pluginEntity)
                                    .flatMap(savedPlugin ->
                                            Flux.fromIterable(plugin.getResources())
                                                    .map(this.pluginFileMapper::fromModel)
                                                    .doOnNext(file -> file.setPluginId(savedPlugin.getId())) // important !
                                                    .flatMap(this.pluginFileRepository::save)
                                                    .then(Mono.just(savedPlugin)) // 3. Retour du plugin
                                    );
                        })
                )
                .thenReturn(plugin);
    }

}

