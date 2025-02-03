package com.itexpert.content.lib.models;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

@Data
@AllArgsConstructor
public class Chart implements Serializable, Cloneable {
    private String name;
    private String value;
    private boolean verified;
}
