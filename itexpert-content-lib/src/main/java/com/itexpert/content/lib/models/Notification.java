package com.itexpert.content.lib.models;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    private UUID id;
    private String type;
    private String typeCode;
    private String typeVersion;
    private String code;
    private Long date;
    private String description;
    private String user;
    private boolean read;
}
