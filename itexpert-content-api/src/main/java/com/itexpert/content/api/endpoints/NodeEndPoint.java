package com.itexpert.content.api.endpoints;

import com.itexpert.content.api.handlers.NodeHandler;
import com.itexpert.content.lib.enums.StatusEnum;
import com.itexpert.content.lib.models.Node;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping(value = "/v0/nodes")
public class NodeEndPoint {

    private final NodeHandler nodeHandler;

    @GetMapping
    public Flux<Node> findAll(@RequestParam(required = false, defaultValue = "PUBLISHED") StatusEnum status) {
        return nodeHandler.findAll(status);
    }

    @GetMapping(value = "/code/{code}")
    public Mono<ResponseEntity<Node>> findByCode(@PathVariable String code,
                                                 @RequestParam(required = false, defaultValue = "PUBLISHED") StatusEnum status) {
        return nodeHandler.findByCodeAndStatus(code, status)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(new ResponseEntity<>(HttpStatus.NOT_FOUND));

    }

    @GetMapping(value = "/parents")
    public Flux<Node> findParentsNodes(@RequestParam(required = false, defaultValue = "PUBLISHED") StatusEnum status) {
        return nodeHandler.findParentsNodes(status);
    }

    @GetMapping(value = "/childreens/parent/{code}")
    public Flux<Node> findChildreensByCodeParent(@PathVariable String code,
                                                 @RequestParam(required = false, defaultValue = "PUBLISHED") StatusEnum status) {
        return nodeHandler.findChildreensByCodeParent(code, status);
    }

}
