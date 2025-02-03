package com.itexpert.content.lib.entities;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.util.UUID;

@Document(collection = "content-clicks")
@Data
public class ContentClick implements Serializable, Cloneable {
    @Id
    private UUID id;

    @Indexed(unique = true)
    private String contentCode;

    private Long clicks;
}
