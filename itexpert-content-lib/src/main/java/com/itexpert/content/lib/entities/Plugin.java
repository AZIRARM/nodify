package com.itexpert.content.lib.entities;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Document(collection = "plugins")
@Data
public class Plugin implements Serializable, Cloneable {
    @Id
    private UUID id;

    private boolean enabled;

    private boolean isEditable;

    private String description;

    @Indexed(unique = true)
    private String name;

    private String code;

    private String entrypoint;

    private Long creationDate;
    private Long modificationDate;
    private UUID modifiedBy;

    private boolean deleted;
}
