package com.itexpert.content.lib.entities;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.util.UUID;

@Document(collection = "languages")
@Data
public class Language implements Serializable, Cloneable {

    @Id
    private UUID id;

    @Indexed(unique = true)
    private String code;

    private String name;

    private String urlIcon;

    private String description;

    public Language clone() throws CloneNotSupportedException {
        return (Language) super.clone();
    }
}
