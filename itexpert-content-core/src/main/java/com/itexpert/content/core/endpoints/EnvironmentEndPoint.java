package com.itexpert.content.core.endpoints;

import com.itexpert.content.core.handlers.EnvironmentHandler;
import com.itexpert.content.lib.models.Environment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping(value = "/v0/environments")
public class EnvironmentEndPoint {

    private final EnvironmentHandler environmentHandler;

    public EnvironmentEndPoint(EnvironmentHandler environmentHandler) {
        this.environmentHandler = environmentHandler;
    }

    @GetMapping
    public Flux<Environment> findAll() {
        return environmentHandler.findAll();
    }

    @GetMapping(value = "/id/{id}")
    public Mono<ResponseEntity<Environment>> findById(@PathVariable String id) {
        return environmentHandler.findById(UUID.fromString(id))
                .map(ResponseEntity::ok)
                .defaultIfEmpty(new ResponseEntity<>(HttpStatus.NOT_FOUND));

    }

    @PostMapping
    public Mono<ResponseEntity<Environment>> save(@RequestBody Environment environment) {
        return environmentHandler.save(environment)
                .map(ResponseEntity::ok);
    }
    @PostMapping("/saveAll")
    public Flux<Environment> saveAll(@RequestBody List<Environment> environments) {
        return environmentHandler.saveAll(environments);
    }

    @DeleteMapping(value = "/code/{code}")
    public Mono<ResponseEntity<Boolean>> delete(@PathVariable String code) {
        return environmentHandler.delete(code)
                .map(ResponseEntity::ok);
    }

}
