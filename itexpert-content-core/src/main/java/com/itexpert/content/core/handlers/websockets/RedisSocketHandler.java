package com.itexpert.content.core.handlers.websockets;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itexpert.content.core.handlers.RedisHandler;
import com.itexpert.content.core.utils.auth.JWTUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
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
public class RedisSocketHandler implements WebSocketHandler {

    private final RedisHandler redisHandler;
    private final JWTUtil jwtUtil;

    @Override
    public Mono<Void> handle(WebSocketSession session) {

        URI uri = session.getHandshakeInfo().getUri();
        MultiValueMap<String, String> queryParams =
                UriComponentsBuilder.fromUri(uri).build().getQueryParams();

        // 1️⃣ Paramètres
        String token = queryParams.getFirst("token");
        String resourceCode = queryParams.getFirst("code");

        if (token == null || resourceCode == null) {
            return session.close();
        }

        String authentication = jwtUtil.getUsernameFromToken(token);

        Flux<String> lockInfoFlux =
                Flux.interval(Duration.ofSeconds(2))
                        .flatMap(tick ->
                                redisHandler.getLockInfo(resourceCode, authentication)
                        )
                        .map(lockInfo -> {
                            try {
                                return new ObjectMapper().writeValueAsString(lockInfo);
                            } catch (JsonProcessingException e) {
                                throw new RuntimeException(e);
                            }
                        });

        return session.send(
                lockInfoFlux.map(session::textMessage)
        );
    }


}
