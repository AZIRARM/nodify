package com.itexpert.content.core.endpoints;

import com.itexpert.content.core.handlers.ContentClickHandler;
import com.itexpert.content.lib.models.ContentClick;
import com.itexpert.content.lib.models.ContentClickCharts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping(value = "/v0/content-clicks")
public class ContentClickEndPoint {

    private ContentClickHandler contentClickHandler;

    public ContentClickEndPoint(ContentClickHandler contentClickHandler) {
        this.contentClickHandler = contentClickHandler;
    }

    @GetMapping("/")
    public Flux<ContentClick> findAll() {
        return contentClickHandler.findAll();
    }

    @GetMapping(value = "/id/{id}")
    public Mono<ResponseEntity<ContentClick>> findById(@PathVariable UUID id) {
        return contentClickHandler.findById(id).map(ResponseEntity::ok).defaultIfEmpty(new ResponseEntity<>(HttpStatus.NOT_FOUND));

    }

    @GetMapping(value = "/contentCode/{code}")
    public Flux<ContentClick> findByContentCode(@PathVariable String code) {
        return contentClickHandler.findByContentCode(code);
    }

    //@RolesAllowed("ADMIN")
    @PostMapping("/")
    public Mono<ResponseEntity<ContentClick>> save(@RequestBody(required = true) ContentClick contentClick) {
        return contentClickHandler.save(contentClick).map(ResponseEntity::ok);
    }

    //@RolesAllowed("ADMIN")
    @DeleteMapping(value = "/id/{id}")
    public Mono<ResponseEntity<Boolean>> delete(@PathVariable String id) {
        return contentClickHandler.delete(UUID.fromString(id)).map(ResponseEntity::ok);
    }

    @GetMapping(value = "/charts")
    public Flux<ContentClickCharts> getCharts() {
        return contentClickHandler.getCharts();
    }

    @GetMapping(value = "/charts/node/{code}")
    public Flux<ContentClickCharts> getChartsByNodeCode(@PathVariable String code) {
        return contentClickHandler.getChartsByNodeCode(code);
    }

    @GetMapping(value = "/charts/content/{code}")
    public Flux<ContentClickCharts> getChartsByContentCode(@PathVariable String code) {
        return contentClickHandler.getChartsByContentCode(code);
    }
}
