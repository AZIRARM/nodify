package com.itexpert.content.core.entities;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "change-logs")
public class ChangeLog {
    @Id
    private UUID id;
    private String name;
    private String description;
    private String createdBy;
    private Instant created;

}
