package com.itexpert.content.lib.models;

import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

@Data
public class Language implements Serializable, Cloneable {
    private UUID id;

    private String name;

    private String code;

    private String urlIcon;

    private String description;

    public Language clone() throws CloneNotSupportedException {
        return (Language) super.clone();
    }
}
