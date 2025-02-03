package com.itexpert.content.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.WebFluxConfigurer;

@Configuration
@EnableWebFlux
public class CorsGlobalConfiguration implements WebFluxConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry corsRegistry) {
        corsRegistry.addMapping("/**")
                .allowedHeaders("Authorization", "Cache-Control", "Content-Type")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PUT","OPTIONS","PATCH", "DELETE")
                .maxAge(3600);

    }
}