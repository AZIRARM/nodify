package com.itexpert.content.lib.entities;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Document(collection = "users")
@Data
public class User implements Serializable, Cloneable {

    @Id
    private UUID id;

    private String name;

    private String firstname;

    private String lastname;

    @Indexed(unique = true)
    private String email;

    private String password;

    private List<String> roles;

    private List<String> projects;
}
