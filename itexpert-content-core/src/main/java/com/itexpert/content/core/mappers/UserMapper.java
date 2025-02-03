package com.itexpert.content.core.mappers;

import com.itexpert.content.lib.models.UserPost;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserPost fromEntity(com.itexpert.content.lib.entities.User source);

    com.itexpert.content.lib.entities.User fromModel(UserPost source);
}
