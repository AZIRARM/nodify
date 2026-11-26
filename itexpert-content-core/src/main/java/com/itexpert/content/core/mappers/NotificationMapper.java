package com.itexpert.content.core.mappers;

import com.itexpert.content.lib.entities.Notification;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
public class NotificationMapper {

    public Notification toEntity(com.itexpert.content.lib.models.Notification model, boolean forAll) {
        return toEntity(model, forAll, new ArrayList<>());
    }

    public Notification toEntity(com.itexpert.content.lib.models.Notification model,
                                 boolean forAll,
                                 java.util.List<String> readers) {
        Notification entity = new Notification();
        entity.setId(model.getId());
        entity.setType(model.getType());
        entity.setTypeCode(model.getTypeCode());
        entity.setTypeVersion(model.getTypeVersion());
        entity.setCode(model.getCode());
        entity.setDate(model.getDate());
        entity.setDescription(model.getDescription());
        entity.setUserId(forAll ? null : model.getUser());
        entity.setReaders(readers != null ? new ArrayList<>(readers) : new ArrayList<>());
        return entity;
    }

    public com.itexpert.content.lib.models.Notification toModel(Notification entity, String currentUser) {
        boolean isRead = entity.getReaders() != null && entity.getReaders().contains(currentUser);
        return com.itexpert.content.lib.models.Notification.builder()
                .id(entity.getId())
                .type(entity.getType())
                .typeCode(entity.getTypeCode())
                .typeVersion(entity.getTypeVersion())
                .code(entity.getCode())
                .date(entity.getDate())
                .description(entity.getDescription())
                .user(entity.getUserId() != null ? entity.getUserId() : currentUser)
                .read(isRead)
                .build();
    }
}