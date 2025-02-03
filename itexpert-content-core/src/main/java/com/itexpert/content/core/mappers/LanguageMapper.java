package com.itexpert.content.core.mappers;

import com.itexpert.content.lib.models.Language;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface LanguageMapper {
    Language fromEntity(com.itexpert.content.lib.entities.Language source);

    com.itexpert.content.lib.entities.Language fromModel(Language source);
}
