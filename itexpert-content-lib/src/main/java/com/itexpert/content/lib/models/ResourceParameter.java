package com.itexpert.content.lib.models;

import com.itexpert.content.lib.enums.ResourceActionEnum;
import com.itexpert.content.lib.enums.ResourceTypeEnum;

import java.io.Serializable;
import java.util.UUID;

@lombok.Data
public class ResourceParameter implements Serializable, Cloneable {
    private UUID id;

    private String code;

    private ResourceActionEnum action;

    private Integer value;

    private String description;

    private ResourceTypeEnum type;
}
