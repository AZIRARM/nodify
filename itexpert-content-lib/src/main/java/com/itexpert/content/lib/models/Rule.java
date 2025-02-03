package com.itexpert.content.lib.models;

import com.itexpert.content.lib.enums.BehaviorEnum;
import com.itexpert.content.lib.enums.OperatorEnum;
import com.itexpert.content.lib.enums.TypeEnum;
import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

@Data
public class Rule implements Serializable, Cloneable {


    private TypeEnum type;
    private String name;
    private String code;
    private String value;
    private boolean editable;
    private boolean erasable;
    private OperatorEnum operator;

    private Boolean behavior;
    private Boolean enable;

    private String description;

    public Rule clone() throws CloneNotSupportedException {
        return (Rule) super.clone();
    }
}
