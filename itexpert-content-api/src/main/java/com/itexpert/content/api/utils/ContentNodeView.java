package com.itexpert.content.api.utils;

import com.itexpert.content.lib.enums.ContentTypeEnum;
import com.itexpert.content.lib.enums.StatusEnum;
import com.itexpert.content.lib.models.*;
import lombok.Data;
import org.springframework.data.annotation.Id;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Data
public class ContentNodeView implements Serializable, Cloneable {

    private String parentCode;

    private String code;

    private String language;

    private ContentTypeEnum type;

    private String title;
    private String description;
    private String redirectUrl;
    private String iconUrl;
    private String pictureUrl;
    
    private Object payload;

    private List<ContentUrl> urls;


    private ContentFile file;

    private List<String> tags;
    private List<Value> values;
    private List<String> roles;
    private List<Rule> rules;
    
    private List<Value> datas;

    private Long creationDate;
    private Long modificationDate;

    private Long publicationDate;


    private List<Translation> translations;

    public ContentNodeView clone() throws CloneNotSupportedException {
        return (ContentNodeView) super.clone();
    }
}
