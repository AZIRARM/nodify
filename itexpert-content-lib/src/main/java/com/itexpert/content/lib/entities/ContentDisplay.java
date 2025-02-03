package com.itexpert.content.lib.entities;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.util.UUID;

@Document(collection = "content-displays")
@Data
public class ContentDisplay implements Serializable, Cloneable {
    @Id
    private UUID id;

    @Indexed(unique = true)
    private String contentCode;

    private Long displays;
}
