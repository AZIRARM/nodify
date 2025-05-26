package com.itexpert.content.api;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;

@Slf4j
@SpringBootApplication(exclude = {SecurityAutoConfiguration.class})
@EnableConfigurationProperties
@EnableReactiveMongoRepositories
public class ContentApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ContentApiApplication.class, args);
    }
}
