package com.itexpert.content.core.mappers;

import com.itexpert.content.lib.models.Notification;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificationMapper {
    Notification fromEntity(com.itexpert.content.lib.entities.Notification source);

    com.itexpert.content.lib.entities.Notification fromModel(Notification source);
}
