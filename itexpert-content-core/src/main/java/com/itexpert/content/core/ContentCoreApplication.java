package com.itexpert.content.core;

import com.itexpert.content.core.handlers.UserHandler;
import com.itexpert.content.core.runners.DefaultAdminAccessRoleCommandLineRunner;
import com.itexpert.content.core.runners.DefaultAdminUserRoleCommandLineRunner;
import com.itexpert.content.core.runners.DefaultLanguagesCommandLineRunner;
import com.itexpert.content.core.runners.DefaultUserCommandLineRunner;
import com.itexpert.content.lib.models.UserPost;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;

@Slf4j
@SpringBootApplication
@EnableConfigurationProperties
@EnableReactiveMongoRepositories
@AllArgsConstructor
@OpenAPIDefinition(info = @Info(title = "Swagger Content Expert", version = "1.0", description = "Documentation APIs v1.0"))
public class ContentCoreApplication {

    private final UserHandler userHandler;
    public static void main(String[] args) {
        SpringApplication.run(ContentCoreApplication.class, args);
    }

    @Bean
    public CommandLineRunner initializeLanguages(DefaultLanguagesCommandLineRunner defaultLanguagesCommandLineRunner) {
        return args -> {
            defaultLanguagesCommandLineRunner.run();
        };
    }

    @Bean
    public CommandLineRunner initializeAdminUserRoleCommandLineRunner(DefaultAdminUserRoleCommandLineRunner defaultAdminUserRoleCommandLineRunner) {
        return args -> {
            defaultAdminUserRoleCommandLineRunner.run();
        };
    }

    @Bean
    public CommandLineRunner initializeAdminAccessRole(DefaultAdminAccessRoleCommandLineRunner defaultAdminAccessRoleCommandLineRunner) {
        return args -> {
            defaultAdminAccessRoleCommandLineRunner.run();
        };
    }

    @Bean
    public CommandLineRunner initializeUserAdmin(DefaultUserCommandLineRunner defaultUserCommandLineRunner) {
        return args -> {
            defaultUserCommandLineRunner.run();
        };
    }

    @Bean("currentUser")
    public UserPost currentUser() {

        String userEmail = ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(authentication -> authentication.getName()).block();
        // continue
        return  this.userHandler.findByEmail(userEmail).block();

    }
}
