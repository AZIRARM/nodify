package com.itexpert.content.api.handlers;

import com.itexpert.content.api.mappers.PluginFileMapper;
import com.itexpert.content.api.repositories.PluginFileRepository;
import com.itexpert.content.api.repositories.PluginRepository;
import com.itexpert.content.lib.entities.Plugin;
import com.itexpert.content.lib.entities.PluginFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PluginHandlerTest {

    @Mock
    private PluginFileRepository pluginFileRepository;

    @Mock
    private PluginRepository pluginRepository;

    @Mock
    private PluginFileMapper pluginFileMapper;

    @InjectMocks
    private PluginHandler pluginHandler;

    private Plugin plugin;
    private PluginFile entityPluginFile;
    private com.itexpert.content.lib.models.PluginFile modelPluginFile;
    private UUID pluginId;
    private String pluginName;
    private String fileName;

    @BeforeEach
    void setUp() {
        pluginId = UUID.randomUUID();
        pluginName = "test-plugin";
        fileName = "test-file.txt";

        plugin = new Plugin();
        plugin.setId(pluginId);
        plugin.setName(pluginName);

        entityPluginFile = new PluginFile();
        entityPluginFile.setId(UUID.randomUUID());
        entityPluginFile.setPluginId(pluginId);
        entityPluginFile.setFileName(fileName);
        entityPluginFile.setData("base64EncodedData");

        modelPluginFile = new com.itexpert.content.lib.models.PluginFile();
        modelPluginFile.setId(entityPluginFile.getId());
        modelPluginFile.setPluginId(pluginId);
        modelPluginFile.setFileName(fileName);
        modelPluginFile.setData("base64EncodedData");
    }

    @Test
    void findResourceByPluginNameAndFileNameShouldReturnPluginFile() {
        when(pluginRepository.findByName(pluginName)).thenReturn(Mono.just(plugin));
        when(pluginFileRepository.findByPluginIdAndFileName(pluginId, fileName)).thenReturn(Mono.just(entityPluginFile));
        when(pluginFileMapper.fromEntity(entityPluginFile)).thenReturn(modelPluginFile);

        StepVerifier.create(pluginHandler.findResourceByPluginNameAndFileName(pluginName, fileName))
                .expectNext(modelPluginFile)
                .verifyComplete();
    }

    @Test
    void findResourceByPluginNameAndFileNameShouldReturnErrorWhenPluginNotFound() {
        when(pluginRepository.findByName(pluginName)).thenReturn(Mono.empty());

        StepVerifier.create(pluginHandler.findResourceByPluginNameAndFileName(pluginName, fileName))
                .verifyComplete();
    }

    @Test
    void findResourceByPluginNameAndFileNameShouldReturnErrorWhenPluginFileNotFound() {
        when(pluginRepository.findByName(pluginName)).thenReturn(Mono.just(plugin));
        when(pluginFileRepository.findByPluginIdAndFileName(pluginId, fileName)).thenReturn(Mono.empty());

        StepVerifier.create(pluginHandler.findResourceByPluginNameAndFileName(pluginName, fileName))
                .verifyComplete();
    }

    @Test
    void findResourceByPluginNameAndFileNameShouldPropagateRepositoryError() {
        when(pluginRepository.findByName(pluginName)).thenReturn(Mono.error(new RuntimeException("Database error")));

        StepVerifier.create(pluginHandler.findResourceByPluginNameAndFileName(pluginName, fileName))
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException)
                .verify();
    }
}