package com.itexpert.content.core.handlers.websockets;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itexpert.content.core.handlers.NotificationHandler;
import com.itexpert.content.lib.models.Notification;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationSocketHandler implements WebSocketHandler {

    private final NotificationHandler notificationHandler;
    private final ObjectMapper objectMapper;

    private static final class NotificationSnapshot {
        private final long count;
        private final List<Notification> unread;

        private NotificationSnapshot(long count, List<Notification> unread) {
            this.count = count;
            this.unread = unread;
        }

        public long getCount() {
            return count;
        }

        public List<Notification> getUnread() {
            return unread;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof NotificationSnapshot)) return false;
            NotificationSnapshot that = (NotificationSnapshot) o;
            return count == that.count && Objects.equals(unread, that.unread);
        }

        @Override
        public int hashCode() {
            return Objects.hash(count, unread);
        }
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        String token = WebSocketAuthUtil.extractToken(session.getHandshakeInfo().getUri());
        if (!WebSocketAuthUtil.authenticate(session, token)) {
            log.warn("WebSocket connection rejected - authentication failed");
            return session.close(CloseStatus.POLICY_VIOLATION);
        }

        URI uri = session.getHandshakeInfo().getUri();
        MultiValueMap<String, String> queryParams = UriComponentsBuilder.fromUri(uri).build().getQueryParams();

        String userId = WebSocketAuthUtil.extractEmailFromToken(token);

        int page = parseIntOrDefault(queryParams.getFirst("page"), 0);
        int limit = parseIntOrDefault(queryParams.getFirst("limit"), 50);

        Flux<NotificationSnapshot> snapshots = Flux.concat(
                        Mono.just(0L),
                        Flux.interval(Duration.ofSeconds(10))
                )
                .flatMap(tick -> notificationHandler.countUnreaded(userId)
                        .zipWith(notificationHandler.unreadedByUser(userId, page, limit).collectList())
                        .map(tuple -> new NotificationSnapshot(tuple.getT1(), tuple.getT2()))
                        .onErrorResume(ex -> {
                            log.warn("Failed to fetch notifications for user={}", userId, ex);
                            return Mono.empty();
                        }))
                .distinctUntilChanged();

        Flux<String> notifFlux = snapshots
                .flatMap(snapshot -> {
                    Map<String, Object> payload = Map.of(
                            "count", snapshot.getCount(),
                            "unread", snapshot.getUnread()
                    );
                    try {
                        return Mono.just(objectMapper.writeValueAsString(payload));
                    } catch (JsonProcessingException e) {
                        log.error("Failed to serialize notifications for user={}", userId, e);
                        return Mono.empty();
                    }
                });

        return session.send(notifFlux.map(session::textMessage));
    }

    private int parseIntOrDefault(String value, int defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }
}