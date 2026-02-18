package com.itexpert.content.core.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
@Order(-2)
@Slf4j
public class GlobalExceptionHandler implements WebExceptionHandler {

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpRequest request = exchange.getRequest();

        // Log essential info to identify the culprit
        log.error("❌ Error on {} {} - {}: {}",
                request.getMethod(),
                request.getURI(),
                ex.getClass().getSimpleName(),
                ex.getMessage() != null ? ex.getMessage() : "No message"
        );

        // For 404 errors, lighter log
        if (ex instanceof ResponseStatusException &&
                ((ResponseStatusException) ex).getStatusCode().value() == 404) {
            log.warn("🔍 404 - Resource not found: {}", request.getURI());
        }

        // Log the cause if it exists (often the real culprit)
        if (ex.getCause() != null) {
            log.error("Caused by: {} - {}",
                    ex.getCause().getClass().getSimpleName(),
                    ex.getCause().getMessage()
            );
        }

        // Simple response
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);

        DataBufferFactory bufferFactory = response.bufferFactory();
        DataBuffer dataBuffer = bufferFactory.wrap("Internal Server Error".getBytes(StandardCharsets.UTF_8));

        return response.writeWith(Mono.just(dataBuffer));
    }
}