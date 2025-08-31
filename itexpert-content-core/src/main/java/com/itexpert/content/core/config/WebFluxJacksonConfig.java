package com.itexpert.content.core.config;

import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;

@Configuration
public class WebFluxJacksonConfig {

    @Bean
    public ServerCodecConfigurer serverCodecConfigurer(ObjectMapper mapper) {
        // augmenter la limite Jackson (string base64)
        mapper.getFactory().setStreamReadConstraints(
                StreamReadConstraints.builder()
                        .maxStringLength(50_000_000) // 50MB au lieu de 5MB
                        .build()
        );

        // augmenter la limite de buffer WebFlux
        Jackson2JsonDecoder decoder = new Jackson2JsonDecoder(mapper);
        decoder.setMaxInMemorySize(50 * 1024 * 1024); // 50MB

        Jackson2JsonEncoder encoder = new Jackson2JsonEncoder(mapper);

        ServerCodecConfigurer configurer = ServerCodecConfigurer.create();
        configurer.defaultCodecs().jackson2JsonDecoder(decoder);
        configurer.defaultCodecs().jackson2JsonEncoder(encoder);

        return configurer;
    }
}
