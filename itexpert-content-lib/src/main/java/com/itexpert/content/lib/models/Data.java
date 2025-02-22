package com.itexpert.content.lib.models;

import java.io.Serializable;
import java.util.UUID;

@lombok.Data
public class Data implements Serializable, Cloneable {

    private UUID id;

    private String contentNodeCode;

    private Long creationDate;

    private Long modificationDate;

    private String dataType;

    private String name;

    private String key;

    private String value;
}
