package com.itexpert.content.core.config;

import com.itexpert.content.core.handlers.websockets.DatasSocketHandler;
import com.itexpert.content.core.handlers.websockets.LockContentsSocketHandler;
import com.itexpert.content.core.handlers.websockets.NotificationSocketHandler;
import com.itexpert.content.core.handlers.websockets.RedisSocketHandler;
import com.itexpert.content.core.handlers.websockets.ContentNodeSocketHandler;
import com.itexpert.content.core.handlers.websockets.NodeSocketHandler;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;

import java.util.HashMap;
import java.util.Map;


@Configuration
@AllArgsConstructor
public class WebSocketConfig {
    private final NotificationSocketHandler notificationSocketHandler;
    private final RedisSocketHandler redisSocketHandler;
    private final DatasSocketHandler datasSocketHandler;
    private final LockContentsSocketHandler lockContentSocketHandler;
    private final ContentNodeSocketHandler contentNodeSocketHandler;
    private final NodeSocketHandler nodeSocketHandler;


    @Bean
    public HandlerMapping webSocketMapping() {
        Map<String, WebSocketHandler> map = new HashMap<>();

        map.put("/ws/notifications", notificationSocketHandler);
        map.put("/ws/owner/**", redisSocketHandler);
        map.put("/ws/datas/contentCode/**", datasSocketHandler);
        map.put("/ws/lock-contents/**", lockContentSocketHandler);
        map.put("/ws/contents/**", contentNodeSocketHandler);
        map.put("/ws/nodes/**", nodeSocketHandler);

        SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
        mapping.setUrlMap(map);
        mapping.setOrder(-1); // priorité max

        return mapping;
    }

    @Bean
    public WebSocketHandlerAdapter handlerAdapter() {
        return new WebSocketHandlerAdapter();
    }
}

