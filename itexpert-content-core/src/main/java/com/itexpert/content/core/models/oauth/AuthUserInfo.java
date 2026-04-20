package com.itexpert.content.core.models.oauth;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

@Data
@NoArgsConstructor
public class AuthUserInfo {
    private String username;
    private String email;
    private List<String> roles;
    private List<String> projects;
    private String authType;
    private Map<String, Object> attributes;

    public AuthUserInfo(String username, List<String> roles, String authType, Map<String, Object> attributes) {
        this.username = username;
        this.email = username;
        this.roles = roles;
        this.projects = new ArrayList<>();
        this.authType = authType;
        this.attributes = attributes;
    }

    public AuthUserInfo(String username, String email, List<String> roles, List<String> projects, String authType,
            Map<String, Object> attributes) {
        this.username = username;
        this.email = email;
        this.roles = roles;
        this.projects = projects;
        this.authType = authType;
        this.attributes = attributes;
    }
}