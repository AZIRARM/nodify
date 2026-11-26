package com.itexpert.content.core.handlers.websockets;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itexpert.content.core.handlers.LockHandler;
import com.itexpert.content.core.models.LockInfo;
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
public class LockOwnerSocketHandler implements WebSocketHandler {

    private final LockHandler lockHandler;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        String token = WebSocketAuthUtil.extractToken(session.getHandshakeInfo().getUri());
        if (!WebSocketAuthUtil.authenticate(session, token)) {
            log.warn("WebSocket connection rejected - authentication failed");
            return session.close(CloseStatus.POLICY_VIOLATION);
        }

        URI uri = session.getHandshakeInfo().getUri();
        String resourceCode = extractResourceCode(uri);
        String currentUser = WebSocketAuthUtil.extractEmailFromToken(token);

        if (resourceCode == null || resourceCode.isBlank()) {
            log.warn("Missing resourceCode in WebSocket path: {}", uri.getPath());
            return session.close(CloseStatus.BAD_DATA);
        }

        Flux<LockInfo> lockFlux = Flux.concat(
                        Mono.just(0L),
                        Flux.interval(Duration.ofSeconds(3))
                )
                .flatMap(tick -> lockHandler.getLockInfo(resourceCode, currentUser)
                        .onErrorResume(error -> {
                            log.error("Error fetching lock info for resource={}", resourceCode, error);
                            return Mono.empty();
                        }))
                .distinctUntilChanged();

        Flux<String> jsonFlux = lockFlux.map(info -> {
            try {
                return objectMapper.writeValueAsString(info);
            } catch (JsonProcessingException e) {
                log.error("Error serializing LockInfo for resource={}", resourceCode, e);
                return "{}";
            }
        });

        return session.send(jsonFlux.map(session::textMessage))
                .doFinally(signalType -> log.info("Owner WebSocket closed for resource={} ({})", resourceCode, signalType));
    }

    private String extractResourceCode(URI uri) {
        MultiValueMap<String, String> queryParams = UriComponentsBuilder.fromUri(uri).build().getQueryParams();
        String codeFromQuery = queryParams.getFirst("code");
        if (codeFromQuery != null && !codeFromQuery.isBlank()) {
            return codeFromQuery;
        }

        String path = uri.getPath();
        if (path == null) {
            return null;
        }
        String prefix = "/ws/owner/";
        int idx = path.indexOf(prefix);
        if (idx == -1) {
            return null;
        }
        String remainder = path.substring(idx + prefix.length());
        return remainder.isBlank() ? null : remainder;
    }
}