package com.itexpert.content.api;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;

@Slf4j
@SpringBootApplication(exclude = { SecurityAutoConfiguration.class })
@EnableConfigurationProperties
@EnableReactiveMongoRepositories
@OpenAPIDefinition(info = @Info(title = "Swagger Content Expert", version = "1.0", description = "Documentation APIs v1.0"))
public class ContentApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ContentApiApplication.class, args);
    }
}
