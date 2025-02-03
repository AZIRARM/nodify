package com.itexpert.content.core.mappers;

import com.itexpert.content.lib.models.ContentNode;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ContentNodeMapper {
    ContentNode fromEntity(com.itexpert.content.lib.entities.ContentNode source);

    com.itexpert.content.lib.entities.ContentNode fromModel(ContentNode source);
}
