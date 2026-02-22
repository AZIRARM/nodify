package com.itexpert.content.api.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public Mono<ResponseEntity<String>> handleResponseStatusException(
            ResponseStatusException ex, ServerWebExchange exchange) {

        HttpStatus status = (HttpStatus) ex.getStatusCode();

        if (status == HttpStatus.NOT_FOUND) {
            log.warn("🔍 404 - Resource not found: {}", exchange.getRequest().getURI());
            return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Resource not found"));
        }

        log.error("❌ Error on {} {} - Status: {} - {}",
                exchange.getRequest().getMethod(),
                exchange.getRequest().getURI(),
                status.value(),
                ex.getMessage()
        );

        return Mono.just(ResponseEntity.status(status)
                .body(status.getReasonPhrase()));
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<String>> handleGenericException(
            Exception ex, ServerWebExchange exchange) {

        log.error("❌ Unexpected error on {} {}",
                exchange.getRequest().getMethod(),
                exchange.getRequest().getURI(),
                ex
        );

        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Internal Server Error"));
    }
}