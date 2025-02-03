package com.itexpert.content.lib.models;

import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

@Data
public class Environment implements Serializable, Cloneable {

    private UUID id;

    private String description;

    private String code;

    private String name;

    private String nodeCode;
}
