package com.itexpert.content.core.runners;

import com.itexpert.content.core.handlers.UserRoleHandler;
import com.itexpert.content.core.models.auth.RoleEnum;
import com.itexpert.content.lib.models.UserRole;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Component
public class DefaultAdminUserRoleCommandLineRunner implements CommandLineRunner {

    private final UserRoleHandler userRoleHandler;

    DefaultAdminUserRoleCommandLineRunner(UserRoleHandler userRoleHandler) {

        this.userRoleHandler = userRoleHandler;
    }

    public void run(String... args) {
        this.start();
    }

    private void start() {
        UserRole adminRole = new UserRole();
        adminRole.setCode(RoleEnum.ADMIN.name());

        UserRole editorRole = new UserRole();
        editorRole.setCode(RoleEnum.EDITOR.name());

        UserRole readerRole = new UserRole();
        readerRole.setCode(RoleEnum.READER.name());

        List<UserRole> defaultRoles = List.of(adminRole, editorRole, readerRole);

        userRoleHandler.findAll()
                .hasElements()
                .flatMapMany(hasRoles -> {
                    if (!hasRoles) {
                        return userRoleHandler.saveAll(defaultRoles)
                                .doOnNext(role -> log.info("User role '{}' saved", role.getCode()))
                                .onErrorResume(e -> {
                                    log.warn("Error while saving default user roles", e);
                                    return Mono.empty();
                                });
                    } else {
                        log.info("Default user roles already exist, skipping initialization.");
                        return Flux.empty();
                    }
                })
                .subscribe();
    }


}