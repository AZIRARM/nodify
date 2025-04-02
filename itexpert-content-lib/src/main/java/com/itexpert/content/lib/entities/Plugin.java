package com.itexpert.content.lib.entities;

import lombok.Data;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.util.UUID;

@Document(collection = "content-environments")
@Data
public class Environment implements Serializable, Cloneable {

    private UUID id;

    private String description;

    @Indexed(unique = true)
    private String code;

    private String name;

    private String nodeCode;
}
