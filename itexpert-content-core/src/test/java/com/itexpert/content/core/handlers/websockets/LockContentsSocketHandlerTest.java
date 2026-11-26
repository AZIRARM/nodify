package com.itexpert.content.core.handlers.websockets;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itexpert.content.core.handlers.LockHandler;
import com.itexpert.content.core.models.LockInfo;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@Slf4j
public class LockContentsSocketHandlerTest {

    private final LockHandler mockLockHandler = mock(LockHandler.class);
    private final ObjectMapper mockObjectMapper = mock(ObjectMapper.class);
    private final WebSocketSession mockSession = mock(WebSocketSession.class);
    private final HandshakeInfo mockHandshakeInfo = mock(HandshakeInfo.class);

    private LockContentsSocketHandler lockContentsSocketHandler;
    private MockedStatic<WebSocketAuthUtil> mockWebSocketAuthUtil;

    @BeforeEach
    void setUp() {
        this.lockContentsSocketHandler = new LockContentsSocketHandler(
                mockLockHandler,
                mockObjectMapper);

        this.mockWebSocketAuthUtil = mockStatic(WebSocketAuthUtil.class);

        lenient().when(mockSession.getHandshakeInfo()).thenReturn(mockHandshakeInfo);
        lenient().when(mockHandshakeInfo.getUri())
                .thenReturn(URI.create("ws://localhost/locks?token=abc"));
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

    // Tests for handle() - authentication failed
    @Test
    public void testHandleAuthenticationFails() {
        mockWebSocketAuthUtil.when(() -> WebSocketAuthUtil.extractToken(any())).thenReturn("bad");
        mockWebSocketAuthUtil.when(() -> WebSocketAuthUtil.authenticate(any(), any())).thenReturn(false);
        when(mockSession.close(CloseStatus.POLICY_VIOLATION)).thenReturn(Mono.empty());

        Mono<Void> result = lockContentsSocketHandler.handle(mockSession);

        result.subscribe(unused -> log.info("Completed with auth failure"),
                error -> log.error("Error: {}", error.getMessage()));

        verify(mockSession, times(1)).close(CloseStatus.POLICY_VIOLATION);
        verify(mockSession, times(0)).send(any());
    }

    // Tests for handle() - authentication success + serialization
    @Test
    public void testHandleAuthenticated() throws JsonProcessingException {
        LockInfo lockInfo = new LockInfo("alice", true, true, "res-1");

        mockWebSocketAuthUtil.when(() -> WebSocketAuthUtil.extractToken(any())).thenReturn("good");
        mockWebSocketAuthUtil.when(() -> WebSocketAuthUtil.authenticate(any(), any())).thenReturn(true);

        when(mockLockHandler.getAllLocks()).thenReturn(Flux.just(lockInfo));
        when(mockObjectMapper.writeValueAsString(any())).thenReturn("[{\"owner\":\"alice\"}]");

        Mono<Void> result = lockContentsSocketHandler.handle(mockSession);

        result.subscribe(unused -> log.info("Completed with auth success"),
                error -> log.error("Error: {}", error.getMessage()));

        verify(mockLockHandler, atLeastOnce()).getAllLocks();
        verify(mockObjectMapper, atLeastOnce()).writeValueAsString(any());
        verify(mockSession, times(1)).send(any());
    }

    // Tests for handle() - serialization failure fallback
    @Test
    public void testHandleSerializationFails() throws JsonProcessingException {
        LockInfo lockInfo = new LockInfo("bob", false, false, "res-2");

        mockWebSocketAuthUtil.when(() -> WebSocketAuthUtil.extractToken(any())).thenReturn("good");
        mockWebSocketAuthUtil.when(() -> WebSocketAuthUtil.authenticate(any(), any())).thenReturn(true);

        when(mockLockHandler.getAllLocks()).thenReturn(Flux.just(lockInfo));
        when(mockObjectMapper.writeValueAsString(any()))
                .thenThrow(new JsonProcessingException("boom") {});

        Mono<Void> result = lockContentsSocketHandler.handle(mockSession);

        result.subscribe(unused -> log.info("Completed with serialization failure"),
                error -> log.error("Error: {}", error.getMessage()));

        verify(mockObjectMapper, atLeastOnce()).writeValueAsString(any());
        verify(mockSession, times(1)).send(any());
    }

    // Tests for handle() - lock handler error fallback
    @Test
    public void testHandleLockHandlerFails() throws JsonProcessingException {
        mockWebSocketAuthUtil.when(() -> WebSocketAuthUtil.extractToken(any())).thenReturn("good");
        mockWebSocketAuthUtil.when(() -> WebSocketAuthUtil.authenticate(any(), any())).thenReturn(true);

        when(mockLockHandler.getAllLocks()).thenReturn(Flux.error(new RuntimeException("db error")));
        when(mockObjectMapper.writeValueAsString(List.of())).thenReturn("[]");

        Mono<Void> result = lockContentsSocketHandler.handle(mockSession);

        result.subscribe(unused -> log.info("Completed with lock handler failure"),
                error -> log.error("Error: {}", error.getMessage()));

        verify(mockLockHandler, atLeastOnce()).getAllLocks();
        verify(mockObjectMapper, atLeastOnce()).writeValueAsString(List.of());
        verify(mockSession, times(1)).send(any());
    }
}