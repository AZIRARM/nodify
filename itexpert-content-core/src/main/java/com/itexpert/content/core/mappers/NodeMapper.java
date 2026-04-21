package com.itexpert.content.core.mappers;

import com.itexpert.content.lib.models.Node;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NodeMapper {
    Node fromEntity(com.itexpert.content.lib.entities.Node source);

    com.itexpert.content.lib.entities.Node fromModel(Node source);
}
