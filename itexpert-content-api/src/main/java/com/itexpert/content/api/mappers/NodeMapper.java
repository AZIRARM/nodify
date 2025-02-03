package com.itexpert.content.api.mappers;

import com.itexpert.content.lib.models.Node;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NodeMapper {
    @Mapping(source="creationDate", target = "creationDate")
    @Mapping(source="modificationDate", target = "modificationDate")
    @Mapping(source="values", target = "values")
    @Mapping(target = "contents", ignore = true)
    Node fromEntity(com.itexpert.content.lib.entities.Node source);

    @Mapping(source="creationDate", target = "creationDate")
    @Mapping(source="modificationDate", target = "modificationDate")
    @Mapping(source="values", target = "values")
    com.itexpert.content.lib.entities.Node fromModel(Node source);
}
