package com.itexpert.content.api.endpoints;

import com.itexpert.content.api.handlers.NodeHandler;
import com.itexpert.content.lib.enums.StatusEnum;
import com.itexpert.content.lib.models.Node;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping(value = "/nodes")
@Tag(name = "Node Endpoint", description = "APIs for managing nodes")
/**
 * REST controller for managing nodes.
 * Provides endpoints to retrieve nodes based on different criteria.
 */
public class NodeEndPoint {

    private final NodeHandler nodeHandler;

    /**
     * Retrieves all nodes with an optional status filter.
     *
     * @param status the status of the nodes to retrieve (default is PUBLISHED)
     * @return a Flux of nodes matching the specified status
     */
    @Operation(summary = "Retrieve all nodes", description = "Fetch all nodes, optionally filtered by status")
    @GetMapping("/")
    public Flux<Node> findAll(@RequestParam(required = false, defaultValue = "PUBLISHED") StatusEnum status) {
        return nodeHandler.findAll(status);
    }

    /**
     * Retrieves a node by its unique code and status.
     *
     * @param code the unique identifier of the node
     * @param status the status of the node to retrieve (default is PUBLISHED)
     * @return a Mono containing the node if found, or a NOT FOUND response
     */
    @Operation(summary = "Retrieve a node by code", description = "Fetch a node by its unique code and status")
    @GetMapping(value = "/code/{code}")
    public Mono<ResponseEntity<Node>> findByCode(@PathVariable String code,
                                                 @RequestParam(required = false, defaultValue = "PUBLISHED") StatusEnum status) {
        return nodeHandler.findByCodeAndStatus(code, status)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * Retrieves a node by its unique slug and status.
     *
     * @param slug the unique identifier of the node
     * @param status the status of the node to retrieve (default is PUBLISHED)
     * @return a Mono containing the node if found, or a NOT FOUND response
     */
    @Operation(summary = "Retrieve a node by slug", description = "Fetch a node by its unique slug and status")
    @GetMapping(value = "/{slug}")
    public Mono<ResponseEntity<Node>> findBySlug(@PathVariable String slug,
                                                 @RequestParam(required = false, defaultValue = "PUBLISHED") StatusEnum status) {
        return nodeHandler.findBySlugAndStatus(slug, status)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
    /**
     * Retrieves all parent nodes with an optional status filter.
     *
     * @param status the status of the parent nodes to retrieve (default is PUBLISHED)
     * @return a Flux of parent nodes
     */
    @Operation(summary = "Retrieve all parent nodes", description = "Fetch all parent nodes optionally filtered by status")
    @GetMapping(value = "/parents")
    public Flux<Node> findParentsNodes(@RequestParam(required = false, defaultValue = "PUBLISHED") StatusEnum status) {
        return nodeHandler.findParentsNodes(status);
    }

    /**
     * Retrieves all child nodes of a specified parent node by its code.
     *
     * @param code the unique identifier of the parent node
     * @param status the status of the child nodes to retrieve (default is PUBLISHED)
     * @return a Flux of child nodes
     */
    @Operation(summary = "Retrieve child nodes by parent code", description = "Fetch all child nodes for a given parent node code, optionally filtered by status")
    @GetMapping(value = "/parent/{code}")
    public Flux<Node> findAllByParentCode(@PathVariable String code,
                                          @RequestParam(required = false, defaultValue = "PUBLISHED") StatusEnum status) {
        return nodeHandler.findAllByParentCode(code, status);
    }
}
