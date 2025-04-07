package com.itexpert.content.core.mappers;

import com.itexpert.content.lib.models.PluginFile;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PluginFileMapper {
    PluginFile fromEntity(com.itexpert.content.lib.entities.PluginFile source);

    com.itexpert.content.lib.entities.PluginFile fromModel(PluginFile source);
}
