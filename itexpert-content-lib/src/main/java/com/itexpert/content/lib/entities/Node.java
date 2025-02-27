package com.itexpert.content.lib.entities;

import com.itexpert.content.lib.enums.StatusEnum;
import com.itexpert.content.lib.models.Rule;
import com.itexpert.content.lib.models.Translation;
import com.itexpert.content.lib.models.Value;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Document(collection = "nodes")
@Data
public class Node implements Serializable, Cloneable {

    @Id
    private UUID id;

    private String code;

    private String parentCode;

    private String parentCodeOrigin;

    private String name;

    private String description;

    private String defaultLanguage;

    private String type;

    private List<String> subNodes;


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

    private boolean favorite;

    private List<Translation> translations;

    public Node clone() throws CloneNotSupportedException {
        return (Node) super.clone();
    }
}
