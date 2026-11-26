package com.itexpert.content.core.handlers.websockets;

import com.itexpert.content.core.handlers.ContentNodeHandler;
import com.itexpert.content.core.utils.auth.JWTUtil;
import com.itexpert.content.lib.enums.StatusEnum;
import com.itexpert.content.lib.models.ContentNode;
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
import static org.mockito.Mockito.*;

@Slf4j
public class ContentNodeSocketHandlerTest {

    private final ContentNodeHandler mockContentNodeHandler = mock(ContentNodeHandler.class);
    private final JWTUtil mockJwtUtil = mock(JWTUtil.class);
    private final WebSocketSession mockSession = mock(WebSocketSession.class);
    private final HandshakeInfo mockHandshakeInfo = mock(HandshakeInfo.class);

    private ContentNodeSocketHandler contentNodeSocketHandler;
    private MockedStatic<WebSocketAuthUtil> mockWebSocketAuthUtil;

    @BeforeEach
    void setUp() {
        this.contentNodeSocketHandler = new ContentNodeSocketHandler(
                mockContentNodeHandler,
                mockJwtUtil);

        this.mockWebSocketAuthUtil = mockStatic(WebSocketAuthUtil.class);

        lenient().when(mockSession.getHandshakeInfo()).thenReturn(mockHandshakeInfo);
        lenient().when(mockHandshakeInfo.getUri())
                .thenReturn(URI.create("ws://localhost/ws/content?token=abc&code=node-1"));
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

        Mono<Void> result = contentNodeSocketHandler.handle(mockSession);

        result.subscribe(unused -> log.info("Completed with auth failure"),
                error -> log.error("Error: {}", error.getMessage()));

        verify(mockSession, times(1)).close(CloseStatus.POLICY_VIOLATION);
        verify(mockSession, times(0)).send(any());
        verify(mockContentNodeHandler, times(0)).findByNodeCodeAndStatus(any(), any());
    }

    @Test
    public void testHandleAuthenticatedWithContentNodes() {
        ContentNode contentNode = new ContentNode();
        contentNode.setCode("node-1");

        mockWebSocketAuthUtil.when(() -> WebSocketAuthUtil.extractToken(any())).thenReturn("good");
        mockWebSocketAuthUtil.when(() -> WebSocketAuthUtil.authenticate(any(), any())).thenReturn(true);

        when(mockContentNodeHandler.findByNodeCodeAndStatus("node-1", StatusEnum.SNAPSHOT.name()))
                .thenReturn(Flux.just(contentNode));

        Mono<Void> result = contentNodeSocketHandler.handle(mockSession);

        result.subscribe(unused -> log.info("Completed with content nodes"),
                error -> log.error("Error: {}", error.getMessage()));

        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> verify(mockContentNodeHandler, atLeastOnce())
                        .findByNodeCodeAndStatus("node-1", StatusEnum.SNAPSHOT.name()));

        verify(mockSession, times(1)).send(any());
    }

    @Test
    public void testHandleAuthenticatedWithEmptyResult() {
        mockWebSocketAuthUtil.when(() -> WebSocketAuthUtil.extractToken(any())).thenReturn("good");
        mockWebSocketAuthUtil.when(() -> WebSocketAuthUtil.authenticate(any(), any())).thenReturn(true);

        when(mockContentNodeHandler.findByNodeCodeAndStatus("node-1", StatusEnum.SNAPSHOT.name()))
                .thenReturn(Flux.empty());

        Mono<Void> result = contentNodeSocketHandler.handle(mockSession);

        result.subscribe(unused -> log.info("Completed with empty result"),
                error -> log.error("Error: {}", error.getMessage()));

        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> verify(mockContentNodeHandler, atLeastOnce())
                        .findByNodeCodeAndStatus("node-1", StatusEnum.SNAPSHOT.name()));

        verify(mockSession, times(1)).send(any());
    }

    @Test
    public void testHandleMissingCodeParam() {
        mockWebSocketAuthUtil.when(() -> WebSocketAuthUtil.extractToken(any())).thenReturn("good");
        mockWebSocketAuthUtil.when(() -> WebSocketAuthUtil.authenticate(any(), any())).thenReturn(true);

        when(mockHandshakeInfo.getUri())
                .thenReturn(URI.create("ws://localhost/ws/content?token=abc"));
        when(mockContentNodeHandler.findByNodeCodeAndStatus(isNull(), eq(StatusEnum.SNAPSHOT.name())))
                .thenReturn(Flux.empty());

        Mono<Void> result = contentNodeSocketHandler.handle(mockSession);

        result.subscribe(unused -> log.info("Completed with missing code"),
                error -> log.error("Error: {}", error.getMessage()));

        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> verify(mockContentNodeHandler, atLeastOnce())
                        .findByNodeCodeAndStatus(isNull(), eq(StatusEnum.SNAPSHOT.name())));

        verify(mockSession, times(1)).send(any());
    }

    @Test
    public void testHandleContentNodeHandlerFails() {
        mockWebSocketAuthUtil.when(() -> WebSocketAuthUtil.extractToken(any())).thenReturn("good");
        mockWebSocketAuthUtil.when(() -> WebSocketAuthUtil.authenticate(any(), any())).thenReturn(true);

        when(mockContentNodeHandler.findByNodeCodeAndStatus("node-1", StatusEnum.SNAPSHOT.name()))
                .thenReturn(Flux.error(new RuntimeException("db error")));

        Mono<Void> result = contentNodeSocketHandler.handle(mockSession);

        result.subscribe(unused -> log.info("Completed with handler error"),
                error -> log.info("Propagated error: {}", error.getMessage()));

        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> verify(mockContentNodeHandler, atLeastOnce())
                        .findByNodeCodeAndStatus("node-1", StatusEnum.SNAPSHOT.name()));

        verify(mockSession, times(1)).send(any());
    }
}