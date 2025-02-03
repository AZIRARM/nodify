package com.itexpert.content.lib.models;

import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

@Data
public class UserRole implements Serializable, Cloneable {
    private UUID id;

    private String name;

    private String description;

    private String code;

    public UserRole clone() throws CloneNotSupportedException {
        return (UserRole) super.clone();
    }
}
