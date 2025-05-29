package com.itexpert.content.lib.models;

import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

@Data
public class Value implements Serializable, Cloneable {

    private UUID id;

    private String key;

    private String value;

}
