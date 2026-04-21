package com.itexpert.content.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "security.openid")
public class OpenIDProperties {
    private boolean enabled = false;
    private OpenIDConfig config = new OpenIDConfig();

    @Data
    public static class OpenIDConfig {
        private String issuerUri;
        private String clientId;
        private String clientSecret;
        private String jwkSetUri;
        private String userInfoUri;
        private String authorizationUri;
        private String studioUri;
        private String tokenUri;
        private String logoutUri;
        private String redirectUri;
        private String rolesClaim = "roles";
        private String userNameClaim = "preferred_username";
    }
}