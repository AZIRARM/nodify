package com.itexpert.content.core.endpoints;

import com.itexpert.content.core.handlers.ContentDisplayHandler;
import com.itexpert.content.lib.models.ContentDisplay;
import com.itexpert.content.lib.models.ContentDisplayCharts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping(value = "/v0/content-displays")
public class ContentDisplayEndPoint {

    private ContentDisplayHandler contentDisplayHandler;

    public ContentDisplayEndPoint(ContentDisplayHandler contentDisplayHandler) {
        this.contentDisplayHandler = contentDisplayHandler;
    }

    @GetMapping
    public Flux<ContentDisplay> findAll() {
        return contentDisplayHandler.findAll();
    }

    @GetMapping(value = "/id/{id}")
    public Mono<ResponseEntity<ContentDisplay>> findById(@PathVariable UUID id) {
        return contentDisplayHandler.findById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(new ResponseEntity<>(HttpStatus.NOT_FOUND));

    }

    @GetMapping(value = "/contentCode/{code}")
    public Mono<ContentDisplay> findByContentCode(@PathVariable String code) {
        return contentDisplayHandler.findByContentCode(code);
    }

    //@RolesAllowed("ADMIN")
    @PostMapping
    public Mono<ResponseEntity<ContentDisplay>> save(@RequestBody(required = true) ContentDisplay contentClick) {
        return contentDisplayHandler.save(contentClick)
                .map(ResponseEntity::ok);
    }

    //@RolesAllowed("ADMIN")
    @DeleteMapping(value = "/id/{id}")
    public Mono<ResponseEntity<Boolean>> delete(@PathVariable String id) {
        return contentDisplayHandler.delete(UUID.fromString(id))
                .map(ResponseEntity::ok);
    }

    @GetMapping(value = "/charts")
    public Flux<ContentDisplayCharts> getCharts() {
        return contentDisplayHandler.getCharts();
    }

    @GetMapping(value = "/charts/node/{code}")
    public Flux<ContentDisplayCharts> getChartsByNodeCode(@PathVariable String code) {
        return contentDisplayHandler.getChartsByNodeCode(code);
    }

    @GetMapping(value = "/charts/content/{code}")
    public Flux<ContentDisplayCharts> getChartsByContentCode(@PathVariable String code) {
        return contentDisplayHandler.getChartsByContentCode(code);
    }
}
