package com.itexpert.content.api.mappers;

import com.google.gson.Gson;
import com.itexpert.content.api.utils.ContentNodeView;
import com.itexpert.content.lib.models.ContentNode;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface ContentNodeMapper {
    ContentNode fromEntity(com.itexpert.content.lib.entities.ContentNode source);

    com.itexpert.content.lib.entities.ContentNode fromModel(ContentNode source);

    @Mappings({
            @Mapping(source = "content", target = "payload", qualifiedByName = "fromString")
    })
    ContentNodeView toView(ContentNode source);

    @Named("fromString")
    default Object fromJsonToMap(String content) {

        try {
            Gson gson = new Gson();
            Object json = gson.fromJson(content, Object.class);
            return json;
        } catch (Exception ex) {
            return content;
        }
    }
}
