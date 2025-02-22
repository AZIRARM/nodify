package com.itexpert.content.lib.entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.util.UUID;

@Document(collection = "content-datas")
@lombok.Data
public class Data implements Serializable, Cloneable {
    @Id
    private UUID id;

    private String contentNodeCode;

    private Long creationDate;

    private Long modificationDate;

    private String dataType;

    private String name;

    @Indexed(unique=true)
    private String key;

    private String value;
}
