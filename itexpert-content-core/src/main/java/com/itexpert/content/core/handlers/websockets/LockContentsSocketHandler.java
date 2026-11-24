package com.itexpert.content.core.handlers.websockets;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itexpert.content.core.handlers.LockHandler;
import com.itexpert.content.core.models.LockInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class LockContentsSocketHandler implements WebSocketHandler {

    private final LockHandler lockHandler;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        String token = WebSocketAuthUtil.extractToken(session.getHandshakeInfo().getUri());
        if (!WebSocketAuthUtil.authenticate(session, token)) {
            log.warn("WebSocket connection rejected - authentication failed");
            return session.close(CloseStatus.POLICY_VIOLATION);
        }

        log.info("WebSocket connection established for lock contents - session: {}", session.getId());

        Flux<List<LockInfo>> locksListFlux = Flux.concat(
                        Mono.just(0L),
                        Flux.interval(Duration.ofSeconds(5))
                )
                .flatMap(tick -> lockHandler.getAllLocks()
                        .collectList()
                        .onErrorResume(error -> Mono.just(List.of()))
                )
                .distinctUntilChanged();

        Flux<String> jsonFlux = locksListFlux
                .map(locksList -> {
                    try {
                        return objectMapper.writeValueAsString(locksList);
                    } catch (JsonProcessingException e) {
                        log.error("Error serializing locks list to JSON", e);
                        return "[]";
                    }
                });

        return session.send(jsonFlux.map(session::textMessage))
                .doFinally(signalType -> log.info("WebSocket connection closed: {}", signalType));
    }
}