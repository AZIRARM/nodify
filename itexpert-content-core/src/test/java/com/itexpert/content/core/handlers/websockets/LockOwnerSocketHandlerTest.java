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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@Slf4j
public class LockOwnerSocketHandlerTest {

    private final LockHandler mockLockHandler = mock(LockHandler.class);
    private final ObjectMapper mockObjectMapper = mock(ObjectMapper.class);
    private final WebSocketSession mockSession = mock(WebSocketSession.class);
    private final HandshakeInfo mockHandshakeInfo = mock(HandshakeInfo.class);

    private LockOwnerSocketHandler lockOwnerSocketHandler;
    private MockedStatic<WebSocketAuthUtil> mockWebSocketAuthUtil;

    @BeforeEach
    void setUp() {
        this.lockOwnerSocketHandler = new LockOwnerSocketHandler(
                mockLockHandler,
                mockObjectMapper);

        this.mockWebSocketAuthUtil = mockStatic(WebSocketAuthUtil.class);

        lenient().when(mockSession.getHandshakeInfo()).thenReturn(mockHandshakeInfo);
        lenient().when(mockHandshakeInfo.getUri())
                .thenReturn(URI.create("ws://localhost/ws/owner/res-1?token=abc"));
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

        Mono<Void> result = lockOwnerSocketHandler.handle(mockSession);

        result.subscribe(unused -> log.info("Completed with auth failure"),
                error -> log.error("Error: {}", error.getMessage()));

        verify(mockSession, times(1)).close(CloseStatus.POLICY_VIOLATION);
        verify(mockSession, times(0)).send(any());
    }

    // Tests for handle() - missing resourceCode (BAD_DATA)
    @Test
    public void testHandleMissingResourceCode() {
        mockWebSocketAuthUtil.when(() -> WebSocketAuthUtil.extractToken(any())).thenReturn("good");
        mockWebSocketAuthUtil.when(() -> WebSocketAuthUtil.authenticate(any(), any())).thenReturn(true);
        mockWebSocketAuthUtil.when(() -> WebSocketAuthUtil.extractEmailFromToken(anyString()))
                .thenReturn("alice@test.com");

        when(mockHandshakeInfo.getUri()).thenReturn(URI.create("ws://localhost/other/path?token=abc"));
        when(mockSession.close(CloseStatus.BAD_DATA)).thenReturn(Mono.empty());

        Mono<Void> result = lockOwnerSocketHandler.handle(mockSession);

        result.subscribe(unused -> log.info("Completed with missing resourceCode"),
                error -> log.error("Error: {}", error.getMessage()));

        verify(mockSession, times(1)).close(CloseStatus.BAD_DATA);
        verify(mockSession, times(0)).send(any());
        verify(mockLockHandler, times(0)).getLockInfo(any(), any());
    }

    // Tests for handle() - success with resourceCode from path
    @Test
    public void testHandleWithResourceCodeFromPath() throws JsonProcessingException {
        LockInfo lockInfo = new LockInfo("alice@test.com", true, true, "res-1");

        mockWebSocketAuthUtil.when(() -> WebSocketAuthUtil.extractToken(any())).thenReturn("good");
        mockWebSocketAuthUtil.when(() -> WebSocketAuthUtil.authenticate(any(), any())).thenReturn(true);
        mockWebSocketAuthUtil.when(() -> WebSocketAuthUtil.extractEmailFromToken(anyString()))
                .thenReturn("alice@test.com");

        when(mockLockHandler.getLockInfo("res-1", "alice@test.com")).thenReturn(Mono.just(lockInfo));
        when(mockObjectMapper.writeValueAsString(any())).thenReturn("{\"resourceCode\":\"res-1\"}");

        Mono<Void> result = lockOwnerSocketHandler.handle(mockSession);

        result.subscribe(unused -> log.info("Completed with success"),
                error -> log.error("Error: {}", error.getMessage()));

        verify(mockLockHandler, atLeastOnce()).getLockInfo("res-1", "alice@test.com");
        verify(mockObjectMapper, atLeastOnce()).writeValueAsString(any());
        verify(mockSession, times(1)).send(any());
    }

    // Tests for handle() - success with resourceCode from query param
    @Test
    public void testHandleWithResourceCodeFromQuery() throws JsonProcessingException {
        LockInfo lockInfo = new LockInfo("bob@test.com", false, true, "res-2");

        mockWebSocketAuthUtil.when(() -> WebSocketAuthUtil.extractToken(any())).thenReturn("good");
        mockWebSocketAuthUtil.when(() -> WebSocketAuthUtil.authenticate(any(), any())).thenReturn(true);
        mockWebSocketAuthUtil.when(() -> WebSocketAuthUtil.extractEmailFromToken(anyString()))
                .thenReturn("bob@test.com");

        when(mockHandshakeInfo.getUri())
                .thenReturn(URI.create("ws://localhost/ws/owner/ignored?token=abc&code=res-2"));
        when(mockLockHandler.getLockInfo("res-2", "bob@test.com")).thenReturn(Mono.just(lockInfo));
        when(mockObjectMapper.writeValueAsString(any())).thenReturn("{\"resourceCode\":\"res-2\"}");

        Mono<Void> result = lockOwnerSocketHandler.handle(mockSession);

        result.subscribe(unused -> log.info("Completed with query code"),
                error -> log.error("Error: {}", error.getMessage()));

        verify(mockLockHandler, atLeastOnce()).getLockInfo("res-2", "bob@test.com");
        verify(mockObjectMapper, atLeastOnce()).writeValueAsString(any());
        verify(mockSession, times(1)).send(any());
    }

    // Tests for handle() - serialization failure fallback
    @Test
    public void testHandleSerializationFails() throws JsonProcessingException {
        LockInfo lockInfo = new LockInfo("alice@test.com", true, true, "res-1");

        mockWebSocketAuthUtil.when(() -> WebSocketAuthUtil.extractToken(any())).thenReturn("good");
        mockWebSocketAuthUtil.when(() -> WebSocketAuthUtil.authenticate(any(), any())).thenReturn(true);
        mockWebSocketAuthUtil.when(() -> WebSocketAuthUtil.extractEmailFromToken(anyString()))
                .thenReturn("alice@test.com");

        when(mockLockHandler.getLockInfo("res-1", "alice@test.com")).thenReturn(Mono.just(lockInfo));
        when(mockObjectMapper.writeValueAsString(any()))
                .thenThrow(new JsonProcessingException("boom") {});

        Mono<Void> result = lockOwnerSocketHandler.handle(mockSession);

        result.subscribe(unused -> log.info("Completed with serialization failure"),
                error -> log.error("Error: {}", error.getMessage()));

        verify(mockObjectMapper, atLeastOnce()).writeValueAsString(any());
        verify(mockSession, times(1)).send(any());
    }

    // Tests for handle() - lock handler error fallback (Mono.empty)
    @Test
    public void testHandleLockHandlerFails() {
        mockWebSocketAuthUtil.when(() -> WebSocketAuthUtil.extractToken(any())).thenReturn("good");
        mockWebSocketAuthUtil.when(() -> WebSocketAuthUtil.authenticate(any(), any())).thenReturn(true);
        mockWebSocketAuthUtil.when(() -> WebSocketAuthUtil.extractEmailFromToken(anyString()))
                .thenReturn("alice@test.com");

        when(mockLockHandler.getLockInfo("res-1", "alice@test.com"))
                .thenReturn(Mono.error(new RuntimeException("db error")));

        Mono<Void> result = lockOwnerSocketHandler.handle(mockSession);

        result.subscribe(unused -> log.info("Completed with lock handler failure"),
                error -> log.error("Error: {}", error.getMessage()));

        verify(mockLockHandler, atLeastOnce()).getLockInfo("res-1", "alice@test.com");
        verify(mockSession, times(1)).send(any());
    }
}