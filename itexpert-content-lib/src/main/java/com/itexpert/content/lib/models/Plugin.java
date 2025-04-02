package com.itexpert.content.lib.entities;

import lombok.Data;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.util.UUID;

@Document(collection = "content-plugins")
@Data
public class Plugin implements Serializable, Cloneable {

    private UUID id;

    private String description;

    @Indexed(unique = true)
    private String name;

    private String code;

    private String entrypoint;
}
