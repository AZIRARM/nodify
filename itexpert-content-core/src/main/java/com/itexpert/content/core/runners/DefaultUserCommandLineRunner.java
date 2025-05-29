package com.itexpert.content.core.runners;

import com.itexpert.content.core.handlers.UserHandler;
import com.itexpert.content.lib.models.UserPost;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Component
public class DefaultUserCommandLineRunner implements CommandLineRunner {

    private static final String ADMIN_USER = "admin";

    @Value("${app.admin-password}")
    private String password;

    private final UserHandler userHandler;

    public DefaultUserCommandLineRunner(UserHandler userHandler) {
        this.userHandler = userHandler;
    }


    public void run(String... args) {
        this.start();
    }

    private void start() {
        if (ObjectUtils.isNotEmpty(password)) {

            UserPost userDB = new UserPost();
            userDB.setEmail(ADMIN_USER);
            userDB.setFirstname(ADMIN_USER);
            userDB.setLastname(ADMIN_USER);
            userDB.setPassword(password);
            userDB.setRoles(List.of("ADMIN"));

            this.userHandler.findAll()
                    .hasElements()
                    .flatMapMany(hasUsers -> {
                        if (!hasUsers) {
                            return this.userHandler.save(userDB)
                                    .doOnNext(result -> log.info("{} Default user saved", result))
                                    .onErrorResume(throwable -> {
                                        log.warn("Error to create default admin user", throwable);
                                        return Mono.empty();
                                    });
                        } else {
                            log.info("Users already exists.");
                            return Flux.empty();
                        }
                    })
                    .subscribe();
        }
    }


}