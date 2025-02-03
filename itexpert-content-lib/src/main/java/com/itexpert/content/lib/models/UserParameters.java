package com.itexpert.content.lib.models;

import lombok.Data;

import java.util.UUID;

@Data
public class UserParameters {

    private UUID id;

    private UUID userId;

    private boolean acceptNotifications;
    private String defaultLanguage;
    private boolean darkMode;
}
