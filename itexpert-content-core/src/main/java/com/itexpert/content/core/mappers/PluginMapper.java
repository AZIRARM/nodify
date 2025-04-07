package com.itexpert.content.core.mappers;

import com.itexpert.content.lib.models.Plugin;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PluginMapper {
    Plugin fromEntity(com.itexpert.content.lib.entities.Plugin source);

    com.itexpert.content.lib.entities.Plugin fromModel(Plugin source);
}
