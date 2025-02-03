package com.itexpert.content.lib.models;

import com.itexpert.content.lib.enums.UrlTypeEnum;
import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

@Data
public class ContentUrl implements Serializable, Cloneable {

    private UUID id;

    private String url;

    private String description;

    private UrlTypeEnum type;

    public ContentUrl clone() throws CloneNotSupportedException {
        return (ContentUrl) super.clone();
    }
}
