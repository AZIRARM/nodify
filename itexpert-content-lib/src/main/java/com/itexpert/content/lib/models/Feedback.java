package com.itexpert.content.lib.models;

import com.itexpert.content.lib.enums.BehaviorEnum;
import com.itexpert.content.lib.enums.OperatorEnum;
import com.itexpert.content.lib.enums.TypeEnum;
import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

@Data
public class Feedback implements Serializable, Cloneable {
    private UUID id;
    private String contentCode;
    private Long createdDate;
    private Long modifiedDate;
    private int evaluation;
    private String message;
    private String userId;
    private boolean verified;
}
