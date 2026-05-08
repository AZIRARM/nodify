package com.itexpert.content.api.mappers;

import com.google.gson.Gson;
import com.itexpert.content.lib.enums.ContentTypeEnum;
import com.itexpert.content.lib.models.ContentNode;
import org.apache.commons.lang3.ObjectUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface ContentNodeMapper {
    @Mappings({
            @Mapping(target = "payload", source = ".", qualifiedByName = "mapPayload"),
            @Mapping(target = "triggerUrl", ignore = true),
            @Mapping(target = "triggerSecret", ignore = true),
            @Mapping(target = "ssg", ignore = true)
    })
    ContentNode fromEntity(com.itexpert.content.lib.entities.ContentNode source);

    com.itexpert.content.lib.entities.ContentNode fromModel(ContentNode source);

    @Named("mapPayload")
    default Object mapPayload(com.itexpert.content.lib.entities.ContentNode source) {
        ContentTypeEnum type = source.getType();

        switch (type) {
            case FILE:
            case PICTURE:
                if (source.getFile() != null && source.getFile().getData() != null) {
                    return source.getFile().getData(); // base64 string
                }
                return null;

            case URLS:
                return source.getRedirectUrl();

            default:
                // HTML, MARKDOWN, etc.
                if (source.getContent() != null) {
                    try {
                        Gson gson = new Gson();
                        Object json = gson.fromJson(source.getContent(), Object.class);
                        return ObjectUtils.isEmpty(json) ? "" : json;
                    } catch (Exception ex) {
                        return source.getContent();
                    }
                }
                return null;
        }
    }
}