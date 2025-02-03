package com.itexpert.content.lib.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.util.UUID;

@Document(collection = "parameters")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Parameter implements Serializable, Cloneable {

    @Id
    private UUID id;


    @Indexed(unique = true)
    private String key;

    private String value;

    private String description;

}
