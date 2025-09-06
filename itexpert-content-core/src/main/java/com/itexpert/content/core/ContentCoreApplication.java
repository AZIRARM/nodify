package com.itexpert.content.core;

import com.itexpert.content.core.handlers.UserHandler;
import com.itexpert.content.lib.models.UserPost;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;

@Slf4j
@SpringBootApplication
@EnableConfigurationProperties
@EnableReactiveMongoRepositories
@EnableScheduling
@AllArgsConstructor
public class ContentCoreApplication {
    private final UserHandler userHandler;

    public static void main(String[] args) {
        SpringApplication.run(ContentCoreApplication.class, args);
    }

    @Bean("currentUser")
    public UserPost currentUser() {

        String userEmail = ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(authentication -> authentication.getName()).block();
        // continue
        return this.userHandler.findByEmail(userEmail).block();

    }
}
