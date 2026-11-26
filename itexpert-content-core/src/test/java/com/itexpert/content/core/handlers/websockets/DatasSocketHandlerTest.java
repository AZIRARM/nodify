package com.itexpert.content.core.handlers.websockets;

import com.itexpert.content.core.handlers.DataHandler;
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

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@Slf4j
public class DatasSocketHandlerTest {

    private final DataHandler mockDataHandler = mock(DataHandler.class);
    private final WebSocketSession mockSession = mock(WebSocketSession.class);
    private final HandshakeInfo mockHandshakeInfo = mock(HandshakeInfo.class);

    private DatasSocketHandler datasSocketHandler;
    private MockedStatic<WebSocketAuthUtil> mockWebSocketAuthUtil;

    @BeforeEach
    void setUp() {
        this.datasSocketHandler = new DatasSocketHandler(mockDataHandler);

        this.mockWebSocketAuthUtil = mockStatic(WebSocketAuthUtil.class);

        lenient().when(mockSession.getHandshakeInfo()).thenReturn(mockHandshakeInfo);
        lenient().when(mockHandshakeInfo.getUri())
                .thenReturn(URI.create("ws://localhost/ws/datas?token=abc&code=node-1"));
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

        Mono<Void> result = datasSocketHandler.handle(mockSession);

        result.subscribe(unused -> log.info("Completed with auth failure"),
                error -> log.error("Error: {}", error.getMessage()));

        verify(mockSession, times(1)).close(CloseStatus.POLICY_VIOLATION);
        verify(mockSession, times(0)).send(any());
        verify(mockDataHandler, times(0)).countByContentNodeCode(any());
    }

    @Test
    public void testHandleAuthenticatedWithCount() {
        mockWebSocketAuthUtil.when(() -> WebSocketAuthUtil.extractToken(any())).thenReturn("good");
        mockWebSocketAuthUtil.when(() -> WebSocketAuthUtil.authenticate(any(), any())).thenReturn(true);

        when(mockDataHandler.countByContentNodeCode("node-1")).thenReturn(Mono.just(5L));

        Mono<Void> result = datasSocketHandler.handle(mockSession);

        result.subscribe(unused -> log.info("Completed with count"),
                error -> log.error("Error: {}", error.getMessage()));

        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> verify(mockDataHandler, atLeastOnce())
                        .countByContentNodeCode("node-1"));

        verify(mockSession, times(1)).send(any());
    }

    @Test
    public void testHandleAuthenticatedWithZeroCount() {
        mockWebSocketAuthUtil.when(() -> WebSocketAuthUtil.extractToken(any())).thenReturn("good");
        mockWebSocketAuthUtil.when(() -> WebSocketAuthUtil.authenticate(any(), any())).thenReturn(true);

        when(mockDataHandler.countByContentNodeCode("node-1")).thenReturn(Mono.just(0L));

        Mono<Void> result = datasSocketHandler.handle(mockSession);

        result.subscribe(unused -> log.info("Completed with zero count"),
                error -> log.error("Error: {}", error.getMessage()));

        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> verify(mockDataHandler, atLeastOnce())
                        .countByContentNodeCode("node-1"));

        verify(mockSession, times(1)).send(any());
    }

    @Test
    public void testHandleMissingCodeParam() {
        mockWebSocketAuthUtil.when(() -> WebSocketAuthUtil.extractToken(any())).thenReturn("good");
        mockWebSocketAuthUtil.when(() -> WebSocketAuthUtil.authenticate(any(), any())).thenReturn(true);

        when(mockHandshakeInfo.getUri())
                .thenReturn(URI.create("ws://localhost/ws/datas?token=abc"));
        when(mockDataHandler.countByContentNodeCode(isNull())).thenReturn(Mono.just(0L));

        Mono<Void> result = datasSocketHandler.handle(mockSession);

        result.subscribe(unused -> log.info("Completed with missing code"),
                error -> log.error("Error: {}", error.getMessage()));

        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> verify(mockDataHandler, atLeastOnce())
                        .countByContentNodeCode(isNull()));

        verify(mockSession, times(1)).send(any());
    }

    @Test
    public void testHandleDataHandlerFails() {
        mockWebSocketAuthUtil.when(() -> WebSocketAuthUtil.extractToken(any())).thenReturn("good");
        mockWebSocketAuthUtil.when(() -> WebSocketAuthUtil.authenticate(any(), any())).thenReturn(true);

        when(mockDataHandler.countByContentNodeCode("node-1"))
                .thenReturn(Mono.error(new RuntimeException("db error")));

        Mono<Void> result = datasSocketHandler.handle(mockSession);

        result.subscribe(unused -> log.info("Completed with handler error"),
                error -> log.info("Propagated error: {}", error.getMessage()));

        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> verify(mockDataHandler, atLeastOnce())
                        .countByContentNodeCode("node-1"));

        verify(mockSession, times(1)).send(any());
    }
}