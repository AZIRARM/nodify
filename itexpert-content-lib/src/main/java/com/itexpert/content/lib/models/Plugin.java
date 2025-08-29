package com.itexpert.content.lib.models;

import lombok.Data;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Data
public class Plugin implements Serializable, Cloneable {

    private UUID id;

    private boolean enabled;

    private boolean editable;

    private String description;

    @Indexed(unique = true)
    private String name;

    private String code;

    private String entrypoint;


    private Long creationDate;
    private Long modificationDate;
    private String modifiedBy;

    private boolean deleted;

    private List<PluginFile> resources;
}
