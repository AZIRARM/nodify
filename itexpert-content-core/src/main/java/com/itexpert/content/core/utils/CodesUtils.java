package com.itexpert.content.core.utils;

import java.security.SecureRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CodesUtils {

    private static final String DIGITS = "0123456789";
    private static final int CODE_LENGTH = 10;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CODES_REGEX = "[\\\\]*\"code[\\\\]*\"[ ]*:[ ]*[\\\\]*\"[a-zA-Z-_]*[0-9]*[\\\\]*\"";


    public static String changeCodes(String jsons, String environment, boolean fromFile) {
        String contentToReturn = cleanJson(jsons);
        Pattern pattern = Pattern.compile(CODES_REGEX);
        Matcher matcher = pattern.matcher(contentToReturn);

        while (matcher.find()) {
            String code = extractCode(matcher.group());
            String newCode = generateNewCode(code, environment, fromFile);
            contentToReturn = contentToReturn.replace(code, newCode);
        }

        return contentToReturn;
    }

    private static String extractCode(String matchedCode) {
        return matchedCode.split(":")[1].replaceAll("[\"\\\\]", "").trim();
    }

    private static String generateNewCode(String code, String environment, boolean fromFile) {
        String[] parts = code.split("-");
        String codeBegin = parts[0];
        String codeEnding = fromFile ? generateRandomCode() : parts[parts.length - 1];

        return (codeBegin + '-' + environment.split("-")[0] + "-" + codeEnding).replace("--", "-");
    }

    private static String generateRandomCode() {
       return generateRandomCodeFactory(CODE_LENGTH);
    }

    public static String generateRandomCodeFactory(int length) {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < length; i++) {
            sb.append(DIGITS.charAt(RANDOM.nextInt(DIGITS.length())));
        }
        return sb.toString();
    }

    private static String cleanJson(String json) {
        StringBuilder result = new StringBuilder(json.length());
        boolean inQuotes = false;
        boolean escapeMode = false;

        for (char character : json.toCharArray()) {
            if (escapeMode) {
                result.append(character);
                escapeMode = false;
            } else if (character == '"') {
                inQuotes = !inQuotes;
                result.append(character);
            } else if (character == '\\') {
                escapeMode = true;
                result.append(character);
            } else if (!inQuotes && character == ' ') {
                continue;
            } else {
                result.append(character);
            }
        }
        return result.toString();
    }
}
