package com.itexpert.content.core.endpoints;

import com.itexpert.content.core.handlers.DataHandler;
import com.itexpert.content.core.handlers.SlugHandler;
import com.itexpert.content.lib.models.Data;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;


@RestController
@RequestMapping("/v0/slugs")
@AllArgsConstructor
@Tag(name = "Slug Controller", description = "API for managing Slugs objects")
public class SlugEndPoint {

    private final SlugHandler slugHandler;

    /**
     * Check if slug exists on contents or nodes.
     *
     * @param slug The slug to check if exists.
     * @return A Mono containing the ResponseEntity with a boolean indicating success.
     */
    @Operation(summary = "Check if slug exists", description = "Check if slug exists for contents nodes and nodes")
    @GetMapping(value = "/exists/{slug}")
    public Mono<List<String>> exists(@Parameter(description = "The slug to check") @PathVariable String slug) {
        return slugHandler.existsBySlug(slug).collectList();
    }


}

