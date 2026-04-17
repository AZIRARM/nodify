package com.itexpert.content.core.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

public class CommonsUtils {
    public static String genererCode(String str) {
        if (str == null)
            return "";

        String resultat = str.replace("-", "_");

        resultat = resultat.replaceAll("[^a-zA-Z0-9_]", "");

        SimpleDateFormat sdf = new SimpleDateFormat("-yyyyMMddHHmmss");
        String timestamp = sdf.format(new Date());

        return resultat + timestamp;
    }
}
