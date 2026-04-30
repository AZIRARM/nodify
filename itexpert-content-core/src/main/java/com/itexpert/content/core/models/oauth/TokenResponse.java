package com.itexpert.content.core.models.oauth;

import lombok.Data;

@Data
public class TokenResponse {
    private String access_token;
    private String refresh_token;
    private String id_token;
    private String token_type;
    private int expires_in;

    public String getAccessToken() {
        return access_token;
    }
}