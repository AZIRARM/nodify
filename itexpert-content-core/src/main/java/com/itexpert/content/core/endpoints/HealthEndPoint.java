package com.itexpert.content.core.endpoints;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@Slf4j
@RestController
@RequestMapping(value = "/health")
public class HealthEndPoint {
    @GetMapping("")
    public String health() {
        return "OK";
    }
}
