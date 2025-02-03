package com.itexpert.content.api.mappers;

import com.itexpert.content.lib.models.ContentDisplay;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ContentDisplayMapper {
    ContentDisplay fromEntity(com.itexpert.content.lib.entities.ContentDisplay source);

    com.itexpert.content.lib.entities.ContentDisplay fromModel(ContentDisplay source);
}
