package com.itexpert.content.core.mappers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itexpert.content.lib.models.Notification;
import org.mapstruct.Mapper;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Notification fromJson(String json) {
        try {
            return objectMapper.readValue(json, Notification.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Erreur de désérialisation notification", e);
        }
    }

    public String toJson(Notification source) {
        try {
            return objectMapper.writeValueAsString(source);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Erreur de sérialisation notification", e);
        }
    }
}