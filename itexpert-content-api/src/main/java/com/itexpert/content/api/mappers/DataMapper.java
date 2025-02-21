package com.itexpert.content.api.mappers;

import com.itexpert.content.lib.models.Data;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DataMapper {
    Data fromEntity(com.itexpert.content.lib.entities.Data source);

    com.itexpert.content.lib.entities.Data fromModel(Data source);
}
