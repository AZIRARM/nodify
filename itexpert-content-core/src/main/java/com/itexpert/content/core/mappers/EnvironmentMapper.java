package com.itexpert.content.core.mappers;

import com.itexpert.content.lib.models.Environment;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface EnvironmentMapper {
    Environment fromEntity(com.itexpert.content.lib.entities.Environment source);

    com.itexpert.content.lib.entities.Environment fromModel(Environment source);
}
