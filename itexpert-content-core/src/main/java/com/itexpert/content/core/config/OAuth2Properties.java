package com.itexpert.content.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "security.oauth2")
public class OAuth2Properties {
    private boolean enabled = false;
    private OAuth2Config config = new OAuth2Config();

    @Data
    public static class OAuth2Config {
        private String tokenUri;
        private String userInfoUri;
        private String authorizationUri;
        private String redirectUri;
        private String frontendTargetUrl;
        private String clientId;
        private String clientSecret;
        private String scope;
        private String rolesClaim = "roles";
        private String userNameClaim = "username";
    }
}