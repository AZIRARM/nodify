package com.itexpert.content.api.handlers;

import com.itexpert.content.api.mappers.PluginFileMapper;
import com.itexpert.content.api.repositories.PluginFileRepository;
import com.itexpert.content.api.repositories.PluginRepository;
import com.itexpert.content.lib.models.PluginFile;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@AllArgsConstructor
@Service
public class PluginHandler {
    private final PluginFileRepository pluginFileRepository;
    private final PluginRepository pluginRepository;
    private final PluginFileMapper pluginFileMapper;

    public Mono<PluginFile> findResourceByPluginNameAndFileName(String pluginName, String fileName) {
        return this.pluginRepository.findByName(pluginName)
                .flatMap(plugin -> this.pluginFileRepository.findByPluginIdAndFileName(plugin.getId(), fileName))
                .map(this.pluginFileMapper::fromEntity);
    }
}

