package com.itexpert.content.core.mappers;

import com.itexpert.content.lib.models.ResourceParameter;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ResourceParameterMapper {
    ResourceParameter fromEntity(com.itexpert.content.lib.entities.ResourceParameter source);

    com.itexpert.content.lib.entities.ResourceParameter fromModel(ResourceParameter source);
}
