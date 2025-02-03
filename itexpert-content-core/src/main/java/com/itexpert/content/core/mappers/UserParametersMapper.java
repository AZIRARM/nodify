package com.itexpert.content.core.mappers;

import com.itexpert.content.lib.models.UserParameters;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserParametersMapper {
    UserParameters fromEntity(com.itexpert.content.lib.entities.UserParameters source);

    com.itexpert.content.lib.entities.UserParameters fromModel(UserParameters source);
}
