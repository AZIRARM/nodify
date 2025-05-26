package com.itexpert.content.lib.entities;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.util.UUID;

@Document(collection = "plugins-files")
@Data
public class PluginFile implements Serializable, Cloneable {
    @Id
    private UUID id;

    private UUID pluginId;

    private String fileName;

    private String description;

    private String data;
}
