package com.itexpert.content.lib.models;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

@Data
public class ContentDisplay implements Serializable, Cloneable {
    private UUID id;
    private String contentCode;
    private Long displays;
}
