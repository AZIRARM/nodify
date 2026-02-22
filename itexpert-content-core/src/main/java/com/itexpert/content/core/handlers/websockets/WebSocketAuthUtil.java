package com.itexpert.content.core.handlers.websockets;

import org.springframework.web.reactive.socket.WebSocketSession;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class WebSocketAuthUtil {

    public static final String AUTH_ATTR = "authenticated";
    public static final String TOKEN_ATTR = "token";

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
            return false;
        }

        boolean isValid = token.length() > 0;

        if (isValid) {
            session.getAttributes().put(AUTH_ATTR, true);
            session.getAttributes().put(TOKEN_ATTR, token);
        }

        return isValid;
    }

    public static boolean isAuthenticated(WebSocketSession session) {
        Boolean authenticated = (Boolean) session.getAttributes().get(AUTH_ATTR);
        return authenticated != null && authenticated;
    }

    public static boolean closeIfNotAuthenticated(WebSocketSession session) throws Exception {
        if (!isAuthenticated(session)) {
            return true;
        }
        return false;
    }
}