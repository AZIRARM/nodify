package com.itexpert.content.lib.entities;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.UUID;

@Document(collection = "access-roles")
@Data
public class AccessRole {

    @Id
    private UUID id;

    private String name;

    private String description;

    @Indexed(unique = true)
    private String code;

}
