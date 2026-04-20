package com.itexpert.content.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "security.auth")
public class SecurityProperties {
    private String mode = "internal";
}
