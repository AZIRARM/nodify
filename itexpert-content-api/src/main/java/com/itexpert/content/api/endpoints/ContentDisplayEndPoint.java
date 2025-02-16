package com.itexpert.content.api.endpoints;

import com.itexpert.content.api.handlers.ContentDisplayHandler;
import com.itexpert.content.lib.models.ContentDisplay;
import com.itexpert.content.lib.models.ContentDisplayCharts;
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
 * REST controller for managing content display.
 * Provides endpoints to retrieve, update, and delete content display entries.
 */
@Slf4j
@RestController
@RequestMapping(value = "/v0/content-displays")
@AllArgsConstructor
@Tag(name = "Content Display Endpoint", description = "APIs for managing content display")
public class ContentDisplayEndPoint {

    private final ContentDisplayHandler contentDisplayHandler;

    /**
     * Retrieves all content display entries.
     *
     * @return a Flux of content display entries
     */
    @Operation(summary = "Retrieve all content display entries")
    @GetMapping
    public Flux<ContentDisplay> findAll() {
        return contentDisplayHandler.findAll();
    }

    /**
     * Retrieves a content display entry by its unique ID.
     *
     * @param id the unique identifier of the content display entry
     * @return a Mono containing the content display entry if found, or a NOT FOUND response
     */
    @Operation(summary = "Retrieve a content display entry by ID")
    @GetMapping(value = "/id/{id}")
    public Mono<ResponseEntity<ContentDisplay>> findById(@PathVariable UUID id) {
        return contentDisplayHandler.findById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * Retrieves a content display entry by content code.
     *
     * @param code the unique content code
     * @return a Mono containing the content display entry if found
     */
    @Operation(summary = "Retrieve a content display entry by content code")
    @GetMapping(value = "/contentCode/{code}")
    public Mono<ContentDisplay> findByContentCode(@PathVariable String code) {
        return contentDisplayHandler.findByContentCode(code);
    }

    /**
     * Increments the display count for a given content code.
     *
     * @param code the unique content code
     * @return a Mono containing a response entity indicating success or failure
     */
    @Operation(summary = "Increment the display count for a content code")
    @PatchMapping(value = "/contentCode/{code}")
    public Mono<ResponseEntity<Boolean>> addDisplay(@PathVariable(required = true) String code) {
        return contentDisplayHandler.addDisplay(code)
                .map(ResponseEntity::ok);
    }

    /**
     * Deletes a content display entry by its unique ID.
     *
     * @param id the unique identifier of the content display entry
     * @return a Mono containing a response entity indicating success or failure
     */
    @Operation(summary = "Delete a content display entry by ID", security = @SecurityRequirement(name = "ADMIN"))
    @DeleteMapping(value = "/id/{id}")
    public Mono<ResponseEntity<Boolean>> delete(@PathVariable String id) {
        return contentDisplayHandler.delete(UUID.fromString(id))
                .map(ResponseEntity::ok);
    }

    /**
     * Retrieves content display statistics in the form of charts.
     *
     * @return a Flux of content display chart data
     */
    @Operation(summary = "Retrieve content display statistics")
    @GetMapping(value = "/charts")
    public Flux<ContentDisplayCharts> getCharts() {
        return contentDisplayHandler.getCharts();
    }
}