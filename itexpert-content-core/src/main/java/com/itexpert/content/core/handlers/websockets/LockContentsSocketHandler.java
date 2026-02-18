package com.itexpert.content.core.handlers.websockets;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itexpert.content.core.handlers.RedisHandler;
import com.itexpert.content.core.models.LockInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
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

    private final RedisHandler redisHandler;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        log.info("WebSocket connection established for lock contents");

        // Accumuler tous les locks dans une liste
        Flux<List<LockInfo>> locksListFlux = Flux.interval(Duration.ofSeconds(2))
                .flatMap(tick -> redisHandler.getAllLocks()
                        .collectList())  // Collecter en liste
                .doOnError(error -> log.error("Error retrieving locks from Redis", error))
                .retry(3)
                .onErrorResume(error -> Flux.empty());

        Flux<String> jsonFlux = locksListFlux
                .map(locksList -> {
                    try {
                        // Sérialiser la LISTE complète
                        return objectMapper.writeValueAsString(locksList);
                    } catch (JsonProcessingException e) {
                        log.error("Error serializing locks list to JSON", e);
                        return "[]"; // Retourner un tableau vide
                    }
                });

        return session.send(jsonFlux.map(session::textMessage))
                .doFinally(signalType -> log.info("WebSocket connection closed: {}", signalType));
    }
}