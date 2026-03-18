package com.itexpert.content.core.handlers.websockets;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itexpert.content.core.handlers.NodeHandler;
import com.itexpert.content.lib.enums.StatusEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
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
public class NodeSocketHandler implements WebSocketHandler {

    private final NodeHandler nodeHandler;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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
        String parentCode = queryParams.getFirst("code");

        Flux<String> nodesJson = Flux.interval(Duration.ofSeconds(2))
                .flatMap(tick -> {
                    if (ObjectUtils.isNotEmpty(parentCode)) {
                        return nodeHandler.findAllByParentCodeAndStatus(parentCode, StatusEnum.SNAPSHOT.name());
                    } else {
                        return nodeHandler.findParentsNodesByStatus(StatusEnum.SNAPSHOT.name());
                    }
                })
                .map(contentNode -> {
                    try {
                        return new ObjectMapper().writeValueAsString(contentNode);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                });

        return session.send(
                nodesJson.map(session::textMessage)
        );
    }

}
