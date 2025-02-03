package com.itexpert.content.lib.enums;

import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum LicenseTypeEnum {
    FREE("FREE", 1, 2),
    BRONZE("BRONZE", 2, 5), //One User One Node
    SILVER("SILVER", 10, 10), //Unlimited Users Unlimited Nodes
    GOLD("GOLD", 50, 50),//10 Users 100 Nodes
    PLATINUM("PLATINUM", 1000000, 1000000);//100 Users 1000 Nodes

    public final Integer maxUsers;
    public final Integer maxNodes;
    public final String name;

    private LicenseTypeEnum(String name, Integer maxUsers, Integer maxNodes) {
        this.name = name;
        this.maxUsers = maxUsers;
        this.maxNodes = maxNodes;
    }
}
