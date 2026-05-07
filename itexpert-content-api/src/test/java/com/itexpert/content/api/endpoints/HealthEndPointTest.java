package com.itexpert.content.api.endpoints;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HealthEndPointTest {

    private final HealthEndPoint healthEndPoint = new HealthEndPoint();

    @Test
    void healthShouldReturnOk() {
        String result = healthEndPoint.health();
        assertThat(result).isEqualTo("OK");
    }
}