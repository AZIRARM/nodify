package com.itexpert.content.core.utils;

import com.itexpert.content.lib.enums.OperatorEnum;
import com.itexpert.content.lib.enums.TypeEnum;
import com.itexpert.content.lib.models.ContentNode;
import com.itexpert.content.lib.models.Node;
import com.itexpert.content.lib.models.Rule;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.List;

@Slf4j
public class SlugsUtils {

    public static String generateSlug(String slug, int rec) {
        if (ObjectUtils.isNotEmpty(slug)) {
             return  slug + "-" + rec;
        }
        return null;
    }

    // Permet de récupérer le compteur rec d'un slug existant
    public static int extractRec(String slug) {
        String digits = slug.replaceAll(".*?(\\d+)$", "$1");
        return digits.matches("\\d+") ? Integer.parseInt(digits) : 0;
    }
}
