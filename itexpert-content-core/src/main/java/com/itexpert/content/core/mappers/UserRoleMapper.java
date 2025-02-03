package com.itexpert.content.core.mappers;

import com.itexpert.content.lib.models.UserRole;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserRoleMapper {
    UserRole fromEntity(com.itexpert.content.lib.entities.UserRole source);

    com.itexpert.content.lib.entities.UserRole fromModel(UserRole source);
}
