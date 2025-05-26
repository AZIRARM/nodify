package com.itexpert.content.lib.entities;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.UUID;

@Document(collection = "user-parameters")
@Data
public class UserParameters {

    @Id
    private UUID id;

    private UUID userId;

    private boolean acceptNotifications;
    private String defaultLanguage;
    private String theme;
    private boolean ai;
}
