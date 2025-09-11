package com.itexpert.content.lib.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class Notification {

    private UUID id;

    private String type;
    private String typeCode;
    private String typeVersion;
    private String code;
    private Long date;
    private String description;
    private UUID user;
    private List<UUID> readers;
}
