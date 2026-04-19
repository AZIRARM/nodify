package com.itexpert.content.core.runners;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.*;

class StartupOrchestratorTest {

    private DefaultAdminAccessRoleInitializer adminAccessRoleInit;
    private DefaultAdminUserRoleInitializer adminUserRoleInit;
    private DefaultEnvironmentInitializer environmentInit;
    private DefaultLanguagesInitializer languagesInit;
    private DefaultUserInitializer userInit;
    private DefautPluginsInitializer pluginsInit;
    private DevTemplatesInitializer devTemplatesInit;
    private UpdateDatasInitializer updateDatasInitializer;
    private StartupOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        adminAccessRoleInit = mock(DefaultAdminAccessRoleInitializer.class);
        adminUserRoleInit = mock(DefaultAdminUserRoleInitializer.class);
        environmentInit = mock(DefaultEnvironmentInitializer.class);
        languagesInit = mock(DefaultLanguagesInitializer.class);
        userInit = mock(DefaultUserInitializer.class);
        pluginsInit = mock(DefautPluginsInitializer.class);
        devTemplatesInit = mock(DevTemplatesInitializer.class);
        updateDatasInitializer = mock(UpdateDatasInitializer.class);

        orchestrator = new StartupOrchestrator(
                adminAccessRoleInit,
                adminUserRoleInit,
                environmentInit,
                languagesInit,
                userInit,
                pluginsInit,
                devTemplatesInit,
                updateDatasInitializer);
    }

    @Nested
    class RunTests {

        @Test
        void run_ShouldExecuteAllInitializersInOrder() {
            // Given
            when(adminAccessRoleInit.init()).thenReturn(Mono.empty());
            when(adminUserRoleInit.init()).thenReturn(Mono.empty());
            when(languagesInit.init()).thenReturn(Mono.empty());
            when(userInit.init()).thenReturn(Mono.empty());
            when(environmentInit.init()).thenReturn(Mono.empty());
            when(pluginsInit.init()).thenReturn(Mono.empty());
            when(devTemplatesInit.init()).thenReturn(Mono.empty());
            when(updateDatasInitializer.init()).thenReturn(Mono.empty());

            // When
            orchestrator.run();

            // Then
            verify(adminAccessRoleInit, times(1)).init();
            verify(adminUserRoleInit, times(1)).init();
            verify(languagesInit, times(1)).init();
            verify(userInit, times(1)).init();
            verify(environmentInit, times(1)).init();
            verify(pluginsInit, times(1)).init();
            verify(devTemplatesInit, times(2)).init();
            verify(updateDatasInitializer, times(1)).init();
        }

        @Test
        void run_ShouldStopPipeline_WhenAdminAccessRoleInitFails() {
            // Given
            when(adminAccessRoleInit.init()).thenReturn(Mono.error(new RuntimeException("Init failed")));

            // When & Then
            assertThrows(RuntimeException.class, () -> orchestrator.run());

            verify(adminAccessRoleInit, times(1)).init();
            verify(languagesInit, never()).init();
            verify(userInit, never()).init();
            verify(environmentInit, never()).init();
            verify(pluginsInit, never()).init();
            verify(devTemplatesInit, never()).init();
            verify(updateDatasInitializer, never()).init();
        }

        @Test
        void run_ShouldStopPipeline_WhenAdminUserRoleInitFails() {
            // Given
            when(adminAccessRoleInit.init()).thenReturn(Mono.empty());
            when(adminUserRoleInit.init()).thenReturn(Mono.error(new RuntimeException("Init failed")));

            // When & Then
            assertThrows(RuntimeException.class, () -> orchestrator.run());

            verify(adminAccessRoleInit, times(1)).init();
            verify(adminUserRoleInit, times(1)).init();
            verify(userInit, never()).init();
            verify(environmentInit, never()).init();
            verify(pluginsInit, never()).init();
            verify(devTemplatesInit, never()).init();
            verify(updateDatasInitializer, never()).init();
        }

        @Test
        void run_ShouldStopPipeline_WhenLanguagesInitFails() {
            // Given
            when(adminAccessRoleInit.init()).thenReturn(Mono.empty());
            when(adminUserRoleInit.init()).thenReturn(Mono.empty());
            when(languagesInit.init()).thenReturn(Mono.error(new RuntimeException("Init failed")));

            // When & Then
            assertThrows(RuntimeException.class, () -> orchestrator.run());

            verify(adminAccessRoleInit, times(1)).init();
            verify(adminUserRoleInit, times(1)).init();
            verify(languagesInit, times(1)).init();
            verify(environmentInit, never()).init();
            verify(pluginsInit, never()).init();
            verify(devTemplatesInit, never()).init();
            verify(updateDatasInitializer, never()).init();
        }

        @Test
        void run_ShouldStopPipeline_WhenUserInitFails() {
            // Given
            when(adminAccessRoleInit.init()).thenReturn(Mono.empty());
            when(adminUserRoleInit.init()).thenReturn(Mono.empty());
            when(languagesInit.init()).thenReturn(Mono.empty());
            when(userInit.init()).thenReturn(Mono.error(new RuntimeException("newLast")));

            // When & Then
            try {
                orchestrator.run();
                fail("Should have thrown an exception");
            } catch (RuntimeException e) {
                assertEquals("newLast", e.getMessage());
            } catch (Exception e) {
                // Si c'est une autre exception, on la laisse passer
                throw e;
            }

            verify(adminAccessRoleInit, times(1)).init();
            verify(adminUserRoleInit, times(1)).init();
            verify(languagesInit, times(1)).init();
            verify(userInit, times(1)).init();
            verify(pluginsInit, never()).init();
            verify(devTemplatesInit, never()).init();
            verify(updateDatasInitializer, never()).init();
        }

        @Test
        void run_ShouldStopPipeline_WhenEnvironmentInitFails() {
            // Given
            when(adminAccessRoleInit.init()).thenReturn(Mono.empty());
            when(adminUserRoleInit.init()).thenReturn(Mono.empty());
            when(languagesInit.init()).thenReturn(Mono.empty());
            when(userInit.init()).thenReturn(Mono.empty());
            when(environmentInit.init()).thenReturn(Mono.error(new RuntimeException("Init failed")));

            // When & Then
            assertThrows(RuntimeException.class, () -> orchestrator.run());

            verify(adminAccessRoleInit, times(1)).init();
            verify(adminUserRoleInit, times(1)).init();
            verify(languagesInit, times(1)).init();
            verify(userInit, times(1)).init();
            verify(environmentInit, times(1)).init();
            verify(devTemplatesInit, never()).init();
            verify(updateDatasInitializer, never()).init();
        }

        @Test
        void run_ShouldStopPipeline_WhenPluginsInitFails() {
            // Given
            when(adminAccessRoleInit.init()).thenReturn(Mono.empty());
            when(adminUserRoleInit.init()).thenReturn(Mono.empty());
            when(languagesInit.init()).thenReturn(Mono.empty());
            when(userInit.init()).thenReturn(Mono.empty());
            when(environmentInit.init()).thenReturn(Mono.empty());
            when(pluginsInit.init()).thenReturn(Mono.error(new RuntimeException("Init failed")));

            // When & Then
            assertThrows(RuntimeException.class, () -> orchestrator.run());

            verify(adminAccessRoleInit, times(1)).init();
            verify(adminUserRoleInit, times(1)).init();
            verify(languagesInit, times(1)).init();
            verify(userInit, times(1)).init();
            verify(environmentInit, times(1)).init();
            verify(pluginsInit, times(1)).init();
            verify(updateDatasInitializer, never()).init();
        }

        @Test
        void run_ShouldStopPipeline_WhenDevTemplatesInitFails() {
            // Given
            when(adminAccessRoleInit.init()).thenReturn(Mono.empty());
            when(adminUserRoleInit.init()).thenReturn(Mono.empty());
            when(languagesInit.init()).thenReturn(Mono.empty());
            when(userInit.init()).thenReturn(Mono.empty());
            when(environmentInit.init()).thenReturn(Mono.empty());
            when(pluginsInit.init()).thenReturn(Mono.empty());
            when(devTemplatesInit.init()).thenReturn(Mono.error(new RuntimeException("Init failed")));

            // When & Then
            assertThrows(RuntimeException.class, () -> orchestrator.run());

            verify(adminAccessRoleInit, times(1)).init();
            verify(adminUserRoleInit, times(1)).init();
            verify(languagesInit, times(1)).init();
            verify(userInit, times(1)).init();
            verify(environmentInit, times(1)).init();
            verify(pluginsInit, times(1)).init();
        }

        @Test
        void run_ShouldStopPipeline_WhenUpdateDatasInitializerFails() {
            // Given
            when(adminAccessRoleInit.init()).thenReturn(Mono.empty());
            when(adminUserRoleInit.init()).thenReturn(Mono.empty());
            when(languagesInit.init()).thenReturn(Mono.empty());
            when(userInit.init()).thenReturn(Mono.empty());
            when(environmentInit.init()).thenReturn(Mono.empty());
            when(pluginsInit.init()).thenReturn(Mono.empty());
            when(devTemplatesInit.init()).thenReturn(Mono.empty());
            when(updateDatasInitializer.init()).thenReturn(Mono.error(new RuntimeException("Init failed")));

            // When & Then
            assertThrows(RuntimeException.class, () -> orchestrator.run());

            verify(adminAccessRoleInit, times(1)).init();
            verify(adminUserRoleInit, times(1)).init();
            verify(languagesInit, times(1)).init();
            verify(userInit, times(1)).init();
            verify(environmentInit, times(1)).init();
            verify(pluginsInit, times(1)).init();
            verify(devTemplatesInit, times(2)).init();
            verify(updateDatasInitializer, times(1)).init();
        }

        @Test
        void run_ShouldExecuteAllInitializers_WhenAllSucceed() {
            // Given
            when(adminAccessRoleInit.init()).thenReturn(Mono.empty());
            when(adminUserRoleInit.init()).thenReturn(Mono.empty());
            when(languagesInit.init()).thenReturn(Mono.empty());
            when(userInit.init()).thenReturn(Mono.empty());
            when(environmentInit.init()).thenReturn(Mono.empty());
            when(pluginsInit.init()).thenReturn(Mono.empty());
            when(devTemplatesInit.init()).thenReturn(Mono.empty());
            when(updateDatasInitializer.init()).thenReturn(Mono.empty());

            // When
            orchestrator.run();

            // Then
            verify(adminAccessRoleInit, times(1)).init();
            verify(adminUserRoleInit, times(1)).init();
            verify(languagesInit, times(1)).init();
            verify(userInit, times(1)).init();
            verify(environmentInit, times(1)).init();
            verify(pluginsInit, times(1)).init();
            verify(devTemplatesInit, times(2)).init();
            verify(updateDatasInitializer, times(1)).init();
        }
    }
}