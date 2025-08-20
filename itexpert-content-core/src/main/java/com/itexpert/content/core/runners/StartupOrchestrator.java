package com.itexpert.content.core.runners;

import lombok.AllArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@AllArgsConstructor
public class StartupOrchestrator implements CommandLineRunner {

    private final DefaultAdminAccessRoleInitializer adminAccessRoleInit;
    private final DefaultAdminUserRoleInitializer adminUserRoleInit;
    private final DefaultEnvironmentInitializer environmentInit;
    private final DefaultLanguagesInitializer languagesInit;
    private final DefaultUserInitializer userInit;
    private final DefautPluginsInitializer pluginsInit;
    private final DevTemplatesInitializer devTemplatesInit;

    @Override
    public void run(String... args) {
        Mono<Void> pipeline =
                adminAccessRoleInit.init()
                        .then(adminUserRoleInit.init())
                        .then(languagesInit.init())
                        .then(userInit.init())
                        .then(environmentInit.init())
                        .then(pluginsInit.init())
                        .then(devTemplatesInit.init()
                        );

        pipeline.block(); // on attend toute la séquence
    }
}
