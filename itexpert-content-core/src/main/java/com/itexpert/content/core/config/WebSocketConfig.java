package com.itexpert.content.core.config;
import com.itexpert.content.core.handlers.NotificationSocketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;

import java.util.Map;


@Configuration
public class WebSocketConfig {
    private final NotificationSocketHandler handler;

    public WebSocketConfig(NotificationSocketHandler handler) {
        this.handler = handler;
    }

    @Bean
    public HandlerMapping webSocketMapping() {
        return new SimpleUrlHandlerMapping(Map.of("/ws/notifications", handler), -1);
    }

    @Bean
    public WebSocketHandlerAdapter handlerAdapter() {
        return new WebSocketHandlerAdapter();
    }
}

