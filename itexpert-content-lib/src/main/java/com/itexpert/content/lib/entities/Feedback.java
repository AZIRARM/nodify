package com.itexpert.content.lib.entities;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.util.UUID;

@Document(collection = "content-feedback")
@Data
public class Feedback implements Serializable, Cloneable {
    @Id
    private UUID id;
    private String contentCode;
    private int evaluation;
    private String message;
    private String userId;
    private boolean verified;
}
