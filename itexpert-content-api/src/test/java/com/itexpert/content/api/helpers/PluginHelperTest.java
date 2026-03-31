package com.itexpert.content.api.helpers;

import com.itexpert.content.api.repositories.PluginRepository;
import com.itexpert.content.lib.entities.ContentNode;
import com.itexpert.content.lib.entities.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class PluginHelperTest {

    @Mock
    private PluginRepository pluginRepository;

    @InjectMocks
    private PluginHelper pluginHelper;

    private ContentNode contentNode;
    private Plugin plugin;

    @BeforeEach
    void setUp() {
        contentNode = new ContentNode();
        contentNode.setContent("Here is a plugin: $with(testPlugin) executed.");

        plugin = new Plugin();
        plugin.setName("testPlugin");
        plugin.setCode("console.log('test');");
        plugin.setEnabled(true);
    }

    @Test
    void testFillPlugin_PluginEnabled() {
        when(pluginRepository.findByName("testPlugin")).thenReturn(Mono.just(plugin));

        StepVerifier.create(pluginHelper.fillPlugin(contentNode))
                .assertNext(res -> {
                    assertTrue(res.getContent().contains("<script>"));
                    assertTrue(res.getContent().contains("console.log('test');"));
                })
                .verifyComplete();
    }

    @Test
    void testFillPlugin_PluginDisabled() {
        plugin.setEnabled(false);
        when(pluginRepository.findByName("testPlugin")).thenReturn(Mono.just(plugin));

        StepVerifier.create(pluginHelper.fillPlugin(contentNode))
                .assertNext(res -> {
                    assertTrue(!res.getContent().contains("$with(testPlugin)"));
                    assertTrue(!res.getContent().contains("<script>"));
                })
                .verifyComplete();
    }

    @Test
    void testFillPlugin_NoPluginMatch() {
        contentNode.setContent("No plugins here.");

        StepVerifier.create(pluginHelper.fillPlugin(contentNode))
                .assertNext(res -> {
                    assertTrue(res.getContent().equals("No plugins here."));
                })
                .verifyComplete();
    }
}
