package com.itexpert.content.api.endpoints;

import com.itexpert.content.api.handlers.ContentClickHandler;
import com.itexpert.content.lib.models.ContentClick;
import com.itexpert.content.lib.models.ContentClickCharts;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * REST controller for managing content clicks.
 * Provides endpoints to retrieve, record, and delete content click data.
 */
@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping(value = "/v0/content-clicks")
@Tag(name = "Content Click Endpoint", description = "APIs for managing content clicks")
public class ContentClickEndPoint {

    private final ContentClickHandler contentClickHandler;

    /**
     * Retrieves all content clicks.
     *
     * @return a Flux of content clicks
     */
    @Operation(summary = "Retrieve all content clicks")
    @GetMapping
    public Flux<ContentClick> findAll() {
        return contentClickHandler.findAll();
    }

    /**
     * Retrieves a content click by its unique ID.
     *
     * @param id the unique identifier of the content click
     * @return a Mono containing the content click if found, or a NOT FOUND response
     */
    @Operation(summary = "Retrieve a content click by ID")
    @GetMapping(value = "/id/{id}")
    public Mono<ResponseEntity<ContentClick>> findById(@PathVariable UUID id) {
        return contentClickHandler.findById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * Retrieves a content click by content code.
     *
     * @param code the unique content code
     * @return a Mono containing the content click associated with the content code
     */
    @Operation(summary = "Retrieve a content click by content code")
    @GetMapping(value = "/contentCode/{code}")
    public Mono<ContentClick> findByContentCode(@PathVariable String code) {
        return contentClickHandler.findByContentCode(code);
    }

    /**
     * Records a content click for a given content code.
     *
     * @param code the unique content code
     * @return a Mono containing the response entity indicating success or failure
     */
    @Operation(summary = "Record a content click")
    @PatchMapping("/contentCode/{code}")
    public Mono<ResponseEntity<Boolean>> save(@PathVariable String code) {
        return contentClickHandler.addClick(code)
                .map(ResponseEntity::ok);
    }

    /**
     * Deletes a content click entry by its unique ID.
     *
     * @param id the unique identifier of the content click to delete
     * @return a Mono containing the response entity indicating success or failure
     */
    @Operation(summary = "Delete a content click by ID", security = @SecurityRequirement(name = "ADMIN"))
    @DeleteMapping(value = "/id/{id}")
    public Mono<ResponseEntity<Boolean>> delete(@PathVariable String id) {
        return contentClickHandler.delete(UUID.fromString(id))
                .map(ResponseEntity::ok);
    }

    /**
     * Retrieves content click statistics in the form of charts.
     *
     * @return a Flux of content click chart data
     */
    @Operation(summary = "Retrieve content click statistics")
    @GetMapping(value = "/charts")
    public Flux<ContentClickCharts> getCharts() {
        return contentClickHandler.getCharts();
    }
}
