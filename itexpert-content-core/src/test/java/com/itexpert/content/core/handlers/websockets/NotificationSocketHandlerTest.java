package com.itexpert.content.core.handlers.websockets;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itexpert.content.core.handlers.NotificationHandler;
import com.itexpert.content.lib.models.Notification;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@Slf4j
public class NotificationSocketHandlerTest {

    private final NotificationHandler mockNotificationHandler = mock(NotificationHandler.class);
    private final ObjectMapper mockObjectMapper = mock(ObjectMapper.class);
    private final WebSocketSession mockSession = mock(WebSocketSession.class);
    private final HandshakeInfo mockHandshakeInfo = mock(HandshakeInfo.class);

    private NotificationSocketHandler notificationSocketHandler;
    private MockedStatic<WebSocketAuthUtil> mockWebSocketAuthUtil;

    @BeforeEach
    void setUp() {
        this.notificationSocketHandler = new NotificationSocketHandler(
                mockNotificationHandler,
                mockObjectMapper);

        this.mockWebSocketAuthUtil = mockStatic(WebSocketAuthUtil.class);

        lenient().when(mockSession.getHandshakeInfo()).thenReturn(mockHandshakeInfo);
        lenient().when(mockHandshakeInfo.getUri())
                .thenReturn(URI.create("ws://localhost/ws/notifications?token=abc"));
        lenient().when(mockSession.textMessage(anyString())).thenAnswer(inv -> {
            String payload = inv.getArgument(0);
            return new WebSocketMessage(WebSocketMessage.Type.TEXT,
                    new DefaultDataBufferFactory().wrap(payload.getBytes()));
        });
        lenient().when(mockSession.send(any())).thenAnswer(inv -> {
            Flux<WebSocketMessage> flux = inv.getArgument(0);
            return flux.then();
        });
    }

    @AfterEach
    void tearDown() {
        this.mockWebSocketAuthUtil.close();
    }

    @Test
    public void testHandleAuthenticationFails() {
        mockWebSocketAuthUtil.when(() -> WebSocketAuthUtil.extractToken(any())).thenReturn("bad");
        mockWebSocketAuthUtil.when(() -> WebSocketAuthUtil.authenticate(any(), any())).thenReturn(false);
        when(mockSession.close(CloseStatus.POLICY_VIOLATION)).thenReturn(Mono.empty());

        Mono<Void> result = notificationSocketHandler.handle(mockSession);

        result.subscribe(unused -> log.info("Completed with auth failure"),
                error -> log.error("Error: {}", error.getMessage()));

        verify(mockSession, times(1)).close(CloseStatus.POLICY_VIOLATION);
        verify(mockSession, times(0)).send(any());
        verify(mockNotificationHandler, times(0)).countUnreaded(any());
        verify(mockNotificationHandler, times(0)).unreadedByUser(any(), anyInt(), anyInt());
    }

    @Test
    public void testHandleAuthenticatedWithNotifications() throws Exception {
        Notification notification = new Notification();
        notification.setId(UUID.randomUUID());
        List<Notification> unread = Collections.singletonList(notification);

        mockWebSocketAuthUtil.when(() -> WebSocketAuthUtil.extractToken(any())).thenReturn("good");
        mockWebSocketAuthUtil.when(() -> WebSocketAuthUtil.authenticate(any(), any())).thenReturn(true);
        mockWebSocketAuthUtil.when(() -> WebSocketAuthUtil.extractEmailFromToken(anyString()))
                .thenReturn("alice@test.com");

        when(mockNotificationHandler.countUnreaded("alice@test.com")).thenReturn(Mono.just(1L));
        when(mockNotificationHandler.unreadedByUser("alice@test.com", 0, 50))
                .thenReturn(Flux.fromIterable(unread));
        when(mockObjectMapper.writeValueAsString(any()))
                .thenReturn("{\"count\":1,\"unread\":[]}");

        Mono<Void> result = notificationSocketHandler.handle(mockSession);

        result.subscribe(unused -> log.info("Completed with notifications"),
                error -> log.error("Error: {}", error.getMessage()));

        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> verify(mockNotificationHandler, atLeastOnce())
                        .countUnreaded("alice@test.com"));

        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> verify(mockNotificationHandler, atLeastOnce())
                        .unreadedByUser("alice@test.com", 0, 50));

        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> verify(mockObjectMapper, atLeastOnce())
                        .writeValueAsString(any()));

        verify(mockSession, times(1)).send(any());
    }

    @Test
    public void testHandleAuthenticatedWithEmptyNotifications() throws Exception {
        mockWebSocketAuthUtil.when(() -> WebSocketAuthUtil.extractToken(any())).thenReturn("good");
        mockWebSocketAuthUtil.when(() -> WebSocketAuthUtil.authenticate(any(), any())).thenReturn(true);
        mockWebSocketAuthUtil.when(() -> WebSocketAuthUtil.extractEmailFromToken(anyString()))
                .thenReturn("bob@test.com");

        when(mockNotificationHandler.countUnreaded("bob@test.com")).thenReturn(Mono.just(0L));
        when(mockNotificationHandler.unreadedByUser("bob@test.com", 0, 50))
                .thenReturn(Flux.empty());
        when(mockObjectMapper.writeValueAsString(any()))
                .thenReturn("{\"count\":0,\"unread\":[]}");

        Mono<Void> result = notificationSocketHandler.handle(mockSession);

        result.subscribe(unused -> log.info("Completed with empty notifications"),
                error -> log.error("Error: {}", error.getMessage()));

        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> verify(mockNotificationHandler, atLeastOnce())
                        .countUnreaded("bob@test.com"));

        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> verify(mockObjectMapper, atLeastOnce())
                        .writeValueAsString(any()));

        verify(mockSession, times(1)).send(any());
    }

    @Test
    public void testHandleWithCustomPagination() throws Exception {
        mockWebSocketAuthUtil.when(() -> WebSocketAuthUtil.extractToken(any())).thenReturn("good");
        mockWebSocketAuthUtil.when(() -> WebSocketAuthUtil.authenticate(any(), any())).thenReturn(true);
        mockWebSocketAuthUtil.when(() -> WebSocketAuthUtil.extractEmailFromToken(anyString()))
                .thenReturn("carol@test.com");

        when(mockHandshakeInfo.getUri())
                .thenReturn(URI.create("ws://localhost/ws/notifications?token=abc&page=2&limit=10"));
        when(mockNotificationHandler.countUnreaded("carol@test.com")).thenReturn(Mono.just(3L));
        when(mockNotificationHandler.unreadedByUser("carol@test.com", 2, 10))
                .thenReturn(Flux.empty());
        when(mockObjectMapper.writeValueAsString(any()))
                .thenReturn("{\"count\":3,\"unread\":[]}");

        Mono<Void> result = notificationSocketHandler.handle(mockSession);

        result.subscribe(unused -> log.info("Completed with custom pagination"),
                error -> log.error("Error: {}", error.getMessage()));

        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> verify(mockNotificationHandler, atLeastOnce())
                        .unreadedByUser("carol@test.com", 2, 10));

        verify(mockSession, times(1)).send(any());
    }

    @Test
    public void testHandleWithInvalidPageAndLimit() throws Exception {
        mockWebSocketAuthUtil.when(() -> WebSocketAuthUtil.extractToken(any())).thenReturn("good");
        mockWebSocketAuthUtil.when(() -> WebSocketAuthUtil.authenticate(any(), any())).thenReturn(true);
        mockWebSocketAuthUtil.when(() -> WebSocketAuthUtil.extractEmailFromToken(anyString()))
                .thenReturn("dave@test.com");

        when(mockHandshakeInfo.getUri())
                .thenReturn(URI.create("ws://localhost/ws/notifications?token=abc&page=abc&limit=xyz"));
        when(mockNotificationHandler.countUnreaded("dave@test.com")).thenReturn(Mono.just(0L));
        when(mockNotificationHandler.unreadedByUser("dave@test.com", 0, 50))
                .thenReturn(Flux.empty());
        when(mockObjectMapper.writeValueAsString(any()))
                .thenReturn("{\"count\":0,\"unread\":[]}");

        Mono<Void> result = notificationSocketHandler.handle(mockSession);

        result.subscribe(unused -> log.info("Completed with invalid pagination"),
                error -> log.error("Error: {}", error.getMessage()));

        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> verify(mockNotificationHandler, atLeastOnce())
                        .unreadedByUser("dave@test.com", 0, 50));

        verify(mockSession, times(1)).send(any());
    }

    @Test
    public void testHandleNotificationHandlerFails() {
        mockWebSocketAuthUtil.when(() -> WebSocketAuthUtil.extractToken(any())).thenReturn("good");
        mockWebSocketAuthUtil.when(() -> WebSocketAuthUtil.authenticate(any(), any())).thenReturn(true);
        mockWebSocketAuthUtil.when(() -> WebSocketAuthUtil.extractEmailFromToken(anyString()))
                .thenReturn("eve@test.com");

        when(mockNotificationHandler.countUnreaded("eve@test.com"))
                .thenReturn(Mono.error(new RuntimeException("db error")));
        when(mockNotificationHandler.unreadedByUser("eve@test.com", 0, 50))
                .thenReturn(Flux.empty());

        Mono<Void> result = notificationSocketHandler.handle(mockSession);

        result.subscribe(unused -> log.info("Completed with handler error"),
                error -> log.error("Error: {}", error.getMessage()));

        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> verify(mockNotificationHandler, atLeastOnce())
                        .countUnreaded("eve@test.com"));

        verify(mockSession, times(1)).send(any());
    }

    @Test
    public void testHandleSerializationFails() throws Exception {
        Notification notification = new Notification();
        notification.setId(UUID.randomUUID());

        mockWebSocketAuthUtil.when(() -> WebSocketAuthUtil.extractToken(any())).thenReturn("good");
        mockWebSocketAuthUtil.when(() -> WebSocketAuthUtil.authenticate(any(), any())).thenReturn(true);
        mockWebSocketAuthUtil.when(() -> WebSocketAuthUtil.extractEmailFromToken(anyString()))
                .thenReturn("frank@test.com");

        when(mockNotificationHandler.countUnreaded("frank@test.com")).thenReturn(Mono.just(1L));
        when(mockNotificationHandler.unreadedByUser("frank@test.com", 0, 50))
                .thenReturn(Flux.just(notification));
        when(mockObjectMapper.writeValueAsString(any()))
                .thenThrow(new com.fasterxml.jackson.core.JsonProcessingException("boom") {});

        Mono<Void> result = notificationSocketHandler.handle(mockSession);

        result.subscribe(unused -> log.info("Completed with serialization failure"),
                error -> log.error("Error: {}", error.getMessage()));

        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> verify(mockObjectMapper, atLeastOnce())
                        .writeValueAsString(any()));

        verify(mockSession, times(1)).send(any());
    }
}