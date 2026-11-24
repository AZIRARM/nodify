package com.itexpert.content.core.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "resource_locks")
public class ResourceLock {

    @Id
    private String id;

    @Indexed(unique = true)
    private String resourceCode;

    private String ownerId;

    @Indexed(expireAfterSeconds = 0)
    private Instant expiresAt;
}