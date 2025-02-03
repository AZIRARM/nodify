package com.itexpert.content.core.mappers;

import com.itexpert.content.lib.models.ContentClick;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ContentClickMapper {
    ContentClick fromEntity(com.itexpert.content.lib.entities.ContentClick source);

    com.itexpert.content.lib.entities.ContentClick fromModel(ContentClick source);
}
