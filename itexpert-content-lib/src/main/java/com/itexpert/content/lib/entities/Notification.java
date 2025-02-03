package com.itexpert.content.lib.entities;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Document(collection = "notifications")
@Data
public class Notification {

    @Id
    private UUID id;

    private String type;
    private String typeCode;
    private String typeVersion;
    private String code;
    private Long date;
    private String description;
    private String userId;
    private List<String> readers;
}
