package com.itexpert.content.core.handlers.websockets;

import org.springframework.web.reactive.socket.WebSocketSession;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class WebSocketAuthUtil {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static final String AUTH_ATTR = "authenticated";
    public static final String TOKEN_ATTR = "token";
    public static final String EMAIL_ATTR = "email";

    public static String extractToken(URI uri) {
        String query = uri.getQuery();
        if (query != null) {
            Map<String, String> params = parseQueryParams(query);
            if (params.containsKey("token")) {
                return params.get("token");
            }
            if (params.containsKey("authorization")) {
                String auth = params.get("authorization");
                if (auth.startsWith("Bearer ")) {
                    return auth.substring(7);
                }
                return auth;
            }
        }
        return null;
    }

    private static Map<String, String> parseQueryParams(String query) {
        Map<String, String> params = new HashMap<>();
        if (query != null && !query.isEmpty()) {
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=", 2);
                if (keyValue.length == 2) {
                    params.put(keyValue[0], keyValue[1]);
                }
            }
        }
        return params;
    }

    public static boolean authenticate(WebSocketSession session, String token) {
        if (token == null || token.isEmpty()) {
            log.warn("No token provided");
            return false;
        }

        String email = extractEmailFromToken(token);

        if (email == null) {
            log.warn("No email found in token");
            return false;
        }

        session.getAttributes().put(AUTH_ATTR, true);
        session.getAttributes().put(TOKEN_ATTR, token);
        session.getAttributes().put(EMAIL_ATTR, email);

        log.debug("WebSocket authenticated for email: {}", email);
        return true;
    }

    public static String extractEmailFromToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return null;
            }

            String payload = parts[1];
            byte[] decodedBytes = Base64.getDecoder().decode(payload);
            String decoded = new String(decodedBytes);

            com.fasterxml.jackson.databind.JsonNode json = objectMapper.readTree(decoded);

            if (json.has("email")) {
                return json.get("email").asText();
            }
            if (json.has("preferred_username")) {
                return json.get("preferred_username").asText();
            }
            if (json.has("sub")) {
                return json.get("sub").asText();
            }

            return null;
        } catch (Exception e) {
            log.error("Failed to extract email from token: {}", e.getMessage());
            return null;
        }
    }

    public static boolean isAuthenticated(WebSocketSession session) {
        Boolean authenticated = (Boolean) session.getAttributes().get(AUTH_ATTR);
        return authenticated != null && authenticated;
    }

    public static boolean closeIfNotAuthenticated(WebSocketSession session) {
        return !isAuthenticated(session);
    }
}