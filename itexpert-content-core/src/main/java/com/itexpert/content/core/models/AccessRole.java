package com.itexpert.content.core.models;

import lombok.Data;

import java.util.UUID;

@Data
public class AccessRole {
    private UUID id;

    private String name;

    private String description;

    private String code;
}
