package com.itexpert.content.core.utils;

import org.apache.commons.lang3.ObjectUtils;

public class SnapshotUtils {


    public static String clearSnapshotIfCode(String text) {
        if (ObjectUtils.isEmpty(text)) {
            return text;
        }


        String newText = text
                .replaceAll("\\?status=SNAPSHOT(&)?", "?")
                .replaceAll("&status=SNAPSHOT", "");

        return newText;
    }
}