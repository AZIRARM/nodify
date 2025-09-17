package com.itexpert.content.core.handlers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itexpert.content.core.handlers.NotificationHandler;
import com.itexpert.content.core.utils.auth.JWTUtil;
import com.itexpert.content.lib.models.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;


@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationSocketHandler implements WebSocketHandler {

    private final NotificationHandler notificationHandler;
    private final JWTUtil jwtUtil;

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        URI uri = session.getHandshakeInfo().getUri();
        MultiValueMap<String, String> queryParams = UriComponentsBuilder.fromUri(uri).build().getQueryParams();

        String token = queryParams.getFirst("token");
        String userId = jwtUtil.getUsernameFromToken(token);

        int page = queryParams.getFirst("page") != null ? Integer.parseInt(queryParams.getFirst("page")) : 0;
        int limit = queryParams.getFirst("limit") != null ? Integer.parseInt(queryParams.getFirst("limit")) : 50;

        Flux<String> notifFlux = Flux.interval(Duration.ofSeconds(5))
                .flatMap(tick ->
                        notificationHandler.countUnreaded(userId)
                                .zipWith(notificationHandler.unreadedByUser(userId, page, limit).collectList())
                                .map(tuple -> Map.of(
                                        "count", tuple.getT1(),
                                        "unread", tuple.getT2()
                                ))
                )
                .map(obj -> {
                    try {
                        return new ObjectMapper().writeValueAsString(obj);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                });

        return session.send(notifFlux.map(session::textMessage));
    }
}
