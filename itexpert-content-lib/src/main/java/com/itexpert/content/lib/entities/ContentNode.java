package com.itexpert.content.lib.entities;

import com.itexpert.content.lib.enums.ContentTypeEnum;
import com.itexpert.content.lib.enums.StatusEnum;
import com.itexpert.content.lib.models.*;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Document(collection = "content-nodes")
@Data
public class ContentNode implements Serializable, Cloneable {
    @Id
    private UUID id;

    private String parentCode;

    private String parentCodeOrigin;

    private String code;

    private String slug;

    private String language;

    private ContentTypeEnum type;

    private String content;
    private String title;
    private String description;

    private String redirectUrl;
    private String iconUrl;
    private String pictureUrl;

    private List<ContentUrl> urls;

    private ContentFile file;

    private List<String> tags;
    private List<Value> values;
    private List<String> roles;
    private List<Rule> rules;

    private String version;
    private Long publicationDate;
    private StatusEnum status;

    private boolean favorite;

    private Long creationDate;
    private Long modificationDate;
    private String modifiedBy;

    private List<Translation> translations;

    public ContentNode clone() throws CloneNotSupportedException {
        return (ContentNode) super.clone();
    }
}
