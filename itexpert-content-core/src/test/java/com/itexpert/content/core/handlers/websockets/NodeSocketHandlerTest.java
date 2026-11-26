package com.itexpert.content.core.handlers.websockets;

import com.itexpert.content.core.handlers.NodeHandler;
import com.itexpert.content.lib.enums.StatusEnum;
import com.itexpert.content.lib.models.Node;
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
public class NodeSocketHandlerTest {

    private final NodeHandler mockNodeHandler = mock(NodeHandler.class);
    private final WebSocketSession mockSession = mock(WebSocketSession.class);
    private final HandshakeInfo mockHandshakeInfo = mock(HandshakeInfo.class);

    private NodeSocketHandler nodeSocketHandler;
    private MockedStatic<WebSocketAuthUtil> mockWebSocketAuthUtil;

    @BeforeEach
    void setUp() {
        this.nodeSocketHandler = new NodeSocketHandler(mockNodeHandler);

        this.mockWebSocketAuthUtil = mockStatic(WebSocketAuthUtil.class);

        lenient().when(mockSession.getHandshakeInfo()).thenReturn(mockHandshakeInfo);
        lenient().when(mockHandshakeInfo.getUri())
                .thenReturn(URI.create("ws://localhost/ws/nodes?token=abc&code=parent-1"));
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

        Mono<Void> result = nodeSocketHandler.handle(mockSession);

        result.subscribe(unused -> log.info("Completed with auth failure"),
                error -> log.error("Error: {}", error.getMessage()));

        verify(mockSession, times(1)).close(CloseStatus.POLICY_VIOLATION);
        verify(mockSession, times(0)).send(any());
        verify(mockNodeHandler, times(0)).findAllByParentCodeAndStatus(any(), any());
        verify(mockNodeHandler, times(0)).findParentsNodesByStatus(any());
    }

    @Test
    public void testHandleWithParentCode() {
        Node node = new Node();
        node.setCode("child-1");

        mockWebSocketAuthUtil.when(() -> WebSocketAuthUtil.extractToken(any())).thenReturn("good");
        mockWebSocketAuthUtil.when(() -> WebSocketAuthUtil.authenticate(any(), any())).thenReturn(true);

        when(mockNodeHandler.findAllByParentCodeAndStatus("parent-1", StatusEnum.SNAPSHOT.name()))
                .thenReturn(Flux.just(node));

        Mono<Void> result = nodeSocketHandler.handle(mockSession);

        result.subscribe(unused -> log.info("Completed with parent code"),
                error -> log.error("Error: {}", error.getMessage()));

        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> verify(mockNodeHandler, atLeastOnce())
                        .findAllByParentCodeAndStatus("parent-1", StatusEnum.SNAPSHOT.name()));

        verify(mockNodeHandler, never()).findParentsNodesByStatus(any());
        verify(mockSession, times(1)).send(any());
    }

    @Test
    public void testHandleWithoutParentCode() {
        Node node = new Node();
        node.setCode("root-1");

        mockWebSocketAuthUtil.when(() -> WebSocketAuthUtil.extractToken(any())).thenReturn("good");
        mockWebSocketAuthUtil.when(() -> WebSocketAuthUtil.authenticate(any(), any())).thenReturn(true);

        when(mockHandshakeInfo.getUri())
                .thenReturn(URI.create("ws://localhost/ws/nodes?token=abc"));
        when(mockNodeHandler.findParentsNodesByStatus(StatusEnum.SNAPSHOT.name()))
                .thenReturn(Flux.just(node));

        Mono<Void> result = nodeSocketHandler.handle(mockSession);

        result.subscribe(unused -> log.info("Completed without parent code"),
                error -> log.error("Error: {}", error.getMessage()));

        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> verify(mockNodeHandler, atLeastOnce())
                        .findParentsNodesByStatus(StatusEnum.SNAPSHOT.name()));

        verify(mockNodeHandler, never()).findAllByParentCodeAndStatus(any(), any());
        verify(mockSession, times(1)).send(any());
    }

    @Test
    public void testHandleWithEmptyParentCode() {
        mockWebSocketAuthUtil.when(() -> WebSocketAuthUtil.extractToken(any())).thenReturn("good");
        mockWebSocketAuthUtil.when(() -> WebSocketAuthUtil.authenticate(any(), any())).thenReturn(true);

        when(mockHandshakeInfo.getUri())
                .thenReturn(URI.create("ws://localhost/ws/nodes?token=abc&code="));
        when(mockNodeHandler.findParentsNodesByStatus(StatusEnum.SNAPSHOT.name()))
                .thenReturn(Flux.empty());

        Mono<Void> result = nodeSocketHandler.handle(mockSession);

        result.subscribe(unused -> log.info("Completed with empty parent code"),
                error -> log.error("Error: {}", error.getMessage()));

        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> verify(mockNodeHandler, atLeastOnce())
                        .findParentsNodesByStatus(StatusEnum.SNAPSHOT.name()));

        verify(mockNodeHandler, never()).findAllByParentCodeAndStatus(any(), any());
        verify(mockSession, times(1)).send(any());
    }

    @Test
    public void testHandleNodeHandlerFails() {
        mockWebSocketAuthUtil.when(() -> WebSocketAuthUtil.extractToken(any())).thenReturn("good");
        mockWebSocketAuthUtil.when(() -> WebSocketAuthUtil.authenticate(any(), any())).thenReturn(true);

        when(mockNodeHandler.findAllByParentCodeAndStatus("parent-1", StatusEnum.SNAPSHOT.name()))
                .thenReturn(Flux.error(new RuntimeException("db error")));

        Mono<Void> result = nodeSocketHandler.handle(mockSession);

        result.subscribe(unused -> log.info("Completed with handler error"),
                error -> log.info("Propagated error: {}", error.getMessage()));

        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> verify(mockNodeHandler, atLeastOnce())
                        .findAllByParentCodeAndStatus("parent-1", StatusEnum.SNAPSHOT.name()));

        verify(mockSession, times(1)).send(any());
    }
}