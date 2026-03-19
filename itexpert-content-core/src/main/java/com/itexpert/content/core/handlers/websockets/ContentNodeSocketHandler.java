package com.itexpert.content.core.handlers.websockets;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itexpert.content.core.handlers.ContentNodeHandler;
import com.itexpert.content.core.utils.auth.JWTUtil;
import com.itexpert.content.lib.enums.StatusEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;


@Slf4j
@Component
@RequiredArgsConstructor
public class ContentNodeSocketHandler implements WebSocketHandler {

    private final ContentNodeHandler contentNodeHandler;
    private final JWTUtil jwtUtil;

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        String token = WebSocketAuthUtil.extractToken(session.getHandshakeInfo().getUri());
        if (!WebSocketAuthUtil.authenticate(session, token)) {
            log.warn("WebSocket connection rejected - authentication failed");
            return session.close(CloseStatus.POLICY_VIOLATION);
        }

        URI uri = session.getHandshakeInfo().getUri();
        MultiValueMap<String, String> queryParams =
                UriComponentsBuilder.fromUri(uri).build().getQueryParams();

        // 1️⃣ Paramètres
        String nodeCode = queryParams.getFirst("code");

        Flux<String> contentNodesJson = Flux.interval(Duration.ofSeconds(2))
                .flatMap(tick ->
                        contentNodeHandler.findByNodeCodeAndStatus(nodeCode, StatusEnum.SNAPSHOT.name())
                )
                .map(contentNode -> {
                    try {
                        return new ObjectMapper().writeValueAsString(contentNode);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                });

        return session.send(
                contentNodesJson.map(session::textMessage) // Conversion String → WebSocketMessage
        );
    }


}
