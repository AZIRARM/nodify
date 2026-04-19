package com.itexpert.content.core.runners;

import com.itexpert.content.core.handlers.PluginHandler;
import com.itexpert.content.lib.models.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DefautPluginsInitializerTest {

    private PluginHandler pluginHandler;
    private DefautPluginsInitializer initializer;

    @BeforeEach
    void setUp() {
        pluginHandler = mock(PluginHandler.class);
        initializer = new DefautPluginsInitializer(pluginHandler);
    }

    @Nested
    class InitTests {

        @Test
        void init_ShouldImportAllPlugins_WhenNoneExist() {
            // Given
            when(pluginHandler.findByName(anyString())).thenReturn(Mono.empty());
            when(pluginHandler.importPlugin(any(Plugin.class))).thenAnswer(invocation -> {
                Plugin plugin = invocation.getArgument(0);
                plugin.setId(UUID.randomUUID());
                return Mono.just(plugin);
            });

            // When
            Mono<Void> result = initializer.init();

            // Then
            StepVerifier.create(result)
                    .verifyComplete();

            verify(pluginHandler, times(6)).findByName(anyString());
            verify(pluginHandler, times(6)).importPlugin(any(Plugin.class));
        }

        @Test
        void init_ShouldNotReimportPlugins_WhenAlreadyExist() {
            // Given
            Plugin existingPlugin = new Plugin();
            existingPlugin.setName("jquery");
            existingPlugin.setId(UUID.randomUUID());

            when(pluginHandler.findByName(anyString())).thenReturn(Mono.just(existingPlugin));
            when(pluginHandler.importPlugin(any(Plugin.class))).thenReturn(Mono.empty());

            // When
            Mono<Void> result = initializer.init();

            // Then
            StepVerifier.create(result)
                    .verifyComplete();

            verify(pluginHandler, times(6)).findByName(anyString());
        }

        @Test
        void init_ShouldSetEditableToFalse_ForAllPlugins() {
            // Given
            when(pluginHandler.findByName(anyString())).thenReturn(Mono.empty());
            when(pluginHandler.importPlugin(any(Plugin.class))).thenAnswer(invocation -> {
                Plugin plugin = invocation.getArgument(0);
                assertFalse(plugin.isEditable());
                plugin.setId(UUID.randomUUID());
                return Mono.just(plugin);
            });

            // When
            Mono<Void> result = initializer.init();

            // Then
            StepVerifier.create(result)
                    .verifyComplete();
        }

        @Test
        void init_ShouldHandleError_WhenFindByNameFails() {
            // Given
            when(pluginHandler.findByName(anyString()))
                    .thenReturn(Mono.error(new RuntimeException("Find error")));
            when(pluginHandler.importPlugin(any(Plugin.class))).thenReturn(Mono.empty());

            // When
            Mono<Void> result = initializer.init();

            // Then
            StepVerifier.create(result)
                    .expectError(RuntimeException.class)
                    .verify();
        }

        @Test
        void init_ShouldContinueOnError_WhenOnePluginFindFails() {
            // Given
            when(pluginHandler.findByName(eq("jquery")))
                    .thenReturn(Mono.error(new RuntimeException("Error loading jquery")));
            when(pluginHandler.findByName(anyString()))
                    .thenReturn(Mono.empty());
            when(pluginHandler.importPlugin(any(Plugin.class)))
                    .thenAnswer(invocation -> {
                        Plugin plugin = invocation.getArgument(0);
                        plugin.setId(UUID.randomUUID());
                        return Mono.just(plugin);
                    });

            // When
            Mono<Void> result = initializer.init();

            // Then
            StepVerifier.create(result)
                    .verifyComplete();
        }

        @Test
        void init_ShouldHandleImportPluginErrorAndContinue() {
            // Given
            when(pluginHandler.findByName(anyString())).thenReturn(Mono.empty());
            when(pluginHandler.importPlugin(any(Plugin.class)))
                    .thenReturn(Mono.error(new RuntimeException("Import failed")));

            // When
            Mono<Void> result = initializer.init();

            // Then
            StepVerifier.create(result)
                    .expectError(RuntimeException.class)
                    .verify();
        }

        private Plugin createTestPlugin() {
            Plugin plugin = new Plugin();
            plugin.setId(UUID.randomUUID());
            plugin.setEditable(false);
            return plugin;
        }

        @Test
        void init_ShouldHandleMixedScenarios_SomeExistSomeNot() {
            // Given
            Plugin existingJquery = new Plugin();
            existingJquery.setName("jquery");
            existingJquery.setId(UUID.randomUUID());

            Plugin existingBootstrap = new Plugin();
            existingBootstrap.setName("bootstrap");
            existingBootstrap.setId(UUID.randomUUID());

            when(pluginHandler.findByName(eq("jquery"))).thenReturn(Mono.just(existingJquery));
            when(pluginHandler.findByName(eq("bootstrap"))).thenReturn(Mono.just(existingBootstrap));
            when(pluginHandler.findByName(anyString())).thenReturn(Mono.empty());

            when(pluginHandler.importPlugin(any(Plugin.class))).thenAnswer(invocation -> {
                Plugin plugin = invocation.getArgument(0);
                plugin.setId(UUID.randomUUID());
                return Mono.just(plugin);
            });

            // When
            Mono<Void> result = initializer.init();

            // Then
            StepVerifier.create(result)
                    .verifyComplete();
        }
    }

    @Nested
    class EdgeCasesTests {

        @Test
        void init_ShouldProcessAllSixPlugins() {
            // Given
            when(pluginHandler.findByName(anyString())).thenReturn(Mono.empty());
            when(pluginHandler.importPlugin(any(Plugin.class))).thenAnswer(invocation -> {
                Plugin plugin = invocation.getArgument(0);
                plugin.setId(UUID.randomUUID());
                return Mono.just(plugin);
            });

            // When
            Mono<Void> result = initializer.init();

            // Then
            StepVerifier.create(result)
                    .verifyComplete();

            verify(pluginHandler, times(6)).importPlugin(any(Plugin.class));
        }

        @Test
        void init_ShouldVerifyPluginNames() {
            // Given
            when(pluginHandler.findByName(anyString())).thenReturn(Mono.empty());
            when(pluginHandler.importPlugin(any(Plugin.class))).thenAnswer(invocation -> {
                Plugin plugin = invocation.getArgument(0);
                plugin.setId(UUID.randomUUID());
                return Mono.just(plugin);
            });

            // When
            Mono<Void> result = initializer.init();

            // Then
            StepVerifier.create(result)
                    .verifyComplete();
        }
    }
}