package com.itexpert.content.core.mappers;

import com.itexpert.content.core.models.AccessRole;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AccessRoleMapper {
    AccessRole fromEntity(com.itexpert.content.lib.entities.AccessRole source);

    com.itexpert.content.lib.entities.AccessRole fromModel(AccessRole source);
}
