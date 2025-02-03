package com.itexpert.content.core.runners;

import com.itexpert.content.core.handlers.AccessRoleHandler;
import com.itexpert.content.core.models.AccessRole;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Component
public class DefaultAdminAccessRoleCommandLineRunner implements CommandLineRunner {


    private final AccessRoleHandler accessRoleHandler;

    DefaultAdminAccessRoleCommandLineRunner(AccessRoleHandler accessRoleHandler) {
        this.accessRoleHandler = accessRoleHandler;
    }

    public void run(String... args) {
        this.start();
    }

    private void start() {
        AccessRole accessRoleAdmin = new AccessRole();
        accessRoleAdmin.setCode("ADMIN");
        accessRoleAdmin.setName("Administrator");
        accessRoleAdmin.setDescription("Administrator role");

        AccessRole accessRoleEditor = new AccessRole();
        accessRoleEditor.setCode("EDITOR");
        accessRoleEditor.setName("Editor");
        accessRoleEditor.setDescription("Editor rôle");

        AccessRole accessRoleReader = new AccessRole();
        accessRoleReader.setCode("READER");
        accessRoleReader.setName("Reader");
        accessRoleReader.setDescription("Reader rôle");

        List<AccessRole> roles = List.of(accessRoleAdmin, accessRoleEditor, accessRoleReader);

        accessRoleHandler.findByCode("ADMIN")
                .switchIfEmpty(
                        accessRoleHandler.save(accessRoleAdmin)
                )
                .subscribe(accessRole -> {
                    log.info("{} Access Roles saved", roles.toString());
                });

    }

}