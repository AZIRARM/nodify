package com.itexpert.content.core.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;

@Slf4j
public class SlugsUtils {

    public static String generateSlug(String slug, int rec) {
        if (ObjectUtils.isNotEmpty(slug)) {
            // Supprime le suffixe "-nombre" s'il existe
            slug = slug.replaceAll("-\\d+$", "");
            return slug + "-" + rec;
        }
        return null;
    }

    // Permet de récupérer le compteur rec d'un slug existant
    public static int extractRec(String slug) {
        String digits = slug.replaceAll(".*?(\\d+)$", "$1");
        return digits.matches("\\d+") ? Integer.parseInt(digits) : 0;
    }
}
