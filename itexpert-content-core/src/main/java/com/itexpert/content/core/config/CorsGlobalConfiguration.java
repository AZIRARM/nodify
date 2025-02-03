package com.itexpert.content.core.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.ServerCodecConfigurer;
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

    @Override
    public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
        // 50 MB
        int MAX_MEMORY_SIZE = 50 * 1024 * 1024;
        configurer.defaultCodecs().maxInMemorySize(MAX_MEMORY_SIZE);
    }
}