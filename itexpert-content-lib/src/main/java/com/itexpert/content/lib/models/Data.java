package com.itexpert.content.lib.models;

import java.io.Serializable;

@lombok.Data
public class Data implements Serializable, Cloneable {

    private String contentNodeCode;

    private Long creationDate;

    private Long modificationDate;

    private String dataType;

    private String name;

    private String key;

    private String value;
}
