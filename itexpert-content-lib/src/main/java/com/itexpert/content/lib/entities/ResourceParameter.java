package com.itexpert.content.lib.entities;

import com.itexpert.content.lib.enums.ResourceActionEnum;
import com.itexpert.content.lib.enums.ResourceTypeEnum;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.util.UUID;

@Document(collection = "resource-parameter")
@lombok.Data
public class ResourceParameter implements Serializable, Cloneable {
    @Id
    private UUID id;

    private String code;

    private ResourceActionEnum action;

    private Integer value;

    private String description;

    private ResourceTypeEnum type;
}
