package com.itexpert.content.lib.entities;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.util.UUID;

@Document(collection = "user-roles")
@Data
public class UserRole implements Serializable, Cloneable {

    @Id
    private UUID id;

    private String name;

    private String description;

    @Indexed(unique = true)
    private String code;

    public UserRole clone() throws CloneNotSupportedException {
        return (UserRole) super.clone();
    }
}
