package com.itexpert.content.lib.models;

import com.itexpert.content.lib.enums.StatusEnum;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Data
public class Node implements Serializable, Cloneable {

    private UUID id;

    private String parentCode;

    private String parentCodeOrigin;

    private String name;

    private String code;

    private String environmentCode;

    private String description;

    private String defaultLanguage;

    private String type;

    private List<String> subNodes;

    private List<ContentNode> contents;

    private List<String> tags;

    private List<Value> values;

    private List<String> roles;

    private List<Rule> rules;

    private List<String> languages;

    private Long creationDate;

    private Long modificationDate;

    private UUID modifiedBy;

    private String version;
    private Long publicationDate;
    private StatusEnum status;

    private String publicationStatus;

    private List<Translation> translations;

    public Node clone() throws CloneNotSupportedException {
        return (Node) super.clone();
    }
}
