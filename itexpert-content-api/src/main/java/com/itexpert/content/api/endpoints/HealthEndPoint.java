package com.itexpert.content.api.endpoints;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping(value = "/health")
@Tag(name = "Health Endpoint", description = "API for checking service health status")
public class HealthEndPoint {

    /**
     * Health check endpoint.
     *
     * @return a simple OK response indicating the service is running
     */
    @Operation(summary = "Check service health status")
    @GetMapping("/")
    public String health() {
        return "OK";
    }
}
