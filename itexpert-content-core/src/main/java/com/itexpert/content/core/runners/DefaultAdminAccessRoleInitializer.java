package com.itexpert.content.core.runners;

import com.itexpert.content.core.handlers.AccessRoleHandler;
import com.itexpert.content.core.models.AccessRole;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Component
@AllArgsConstructor
public class DefaultAdminAccessRoleInitializer {

    private final AccessRoleHandler accessRoleHandler;

    public Mono<Void> init() {
        AccessRole accessRoleAdmin = new AccessRole();
        accessRoleAdmin.setCode("ADMIN");
        accessRoleAdmin.setName("Administrator");
        accessRoleAdmin.setDescription("Administrator role");

        AccessRole accessRoleEditor = new AccessRole();
        accessRoleEditor.setCode("EDITOR");
        accessRoleEditor.setName("Editor");
        accessRoleEditor.setDescription("Editor role");

        AccessRole accessRoleReader = new AccessRole();
        accessRoleReader.setCode("READER");
        accessRoleReader.setName("Reader");
        accessRoleReader.setDescription("Reader role");

        List<AccessRole> roles = List.of(accessRoleAdmin, accessRoleEditor, accessRoleReader);

        return accessRoleHandler.findAll()
                .hasElements()
                .flatMapMany(hasAny -> {
                    if (!hasAny) {
                        return accessRoleHandler.saveAll(roles)
                                .doOnNext(role -> log.info("Access role '{}' saved", role.getCode()))
                                .onErrorResume(e -> {
                                    log.warn("Error while saving default access roles", e);
                                    return Mono.empty();
                                });
                    } else {
                        log.info("Default access roles already exist, skipping initialization.");
                        return Flux.empty();
                    }
                })
                .then();
    }

}