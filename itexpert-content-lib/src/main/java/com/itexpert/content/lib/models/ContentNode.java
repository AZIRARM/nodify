package com.itexpert.content.lib.models;

import com.itexpert.content.lib.enums.ContentTypeEnum;
import com.itexpert.content.lib.enums.StatusEnum;
import lombok.Data;
import org.springframework.data.annotation.Id;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Data
public class ContentNode implements Serializable, Cloneable {

    @Id
    private UUID id;

    private String parentCode;

    private String parentCodeOrigin;

    private String code;

    private String environmentCode;

    private String language;

    private ContentTypeEnum type;

    private String title;
    private String description;
    private String redirectUrl;
    private String iconUrl;
    private String pictureUrl;

    private String content;

    private List<ContentUrl> urls;

    private ContentFile file;

    private List<String> tags;
    private List<Value> values;
    private List<String> roles;
    private List<Rule> rules;

    private Long creationDate;
    private Long modificationDate;
    private UUID modifiedBy;

    private String version;
    private Long publicationDate;
    private StatusEnum status;

    private boolean favorite;

    private String publicationStatus;

    private List<Translation> translations;

    public ContentNode clone() throws CloneNotSupportedException {
        return (ContentNode) super.clone();
    }
}
