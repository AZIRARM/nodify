package com.itexpert.content.lib.enums;

public enum LanguagesEnum {

    FR("fr", null),
    EN("en", null),
    ES("sp", null),
    AR("ar", null);

    public final String label;
    public final String urlFlag;

    private LanguagesEnum(String label, String urlFlag) {
        this.label = label;
        this.urlFlag = urlFlag;
    }
}
