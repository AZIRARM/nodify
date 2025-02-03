package com.itexpert.content.core.mappers;

import com.itexpert.content.lib.models.Node;
import com.itexpert.content.lib.models.Value;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface NodeMapper {
    Node fromEntity(com.itexpert.content.lib.entities.Node source);
    com.itexpert.content.lib.entities.Node fromModel(Node source);
}
