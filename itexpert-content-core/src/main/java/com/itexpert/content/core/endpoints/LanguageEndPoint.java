package com.itexpert.content.core.endpoints;

import com.itexpert.content.core.handlers.LanguageHandler;
import com.itexpert.content.lib.models.Language;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * REST controller for managing languages.
 */
@Slf4j
@RestController
@RequestMapping(value = "/v0/languages")
@Tag(name = "Language API", description = "Operations related to languages")
public class LanguageEndPoint {

    private final LanguageHandler languageHandler;

    /**
     * Constructor for LanguageEndPoint.
     *
     * @param languageHandler the language handler
     */
    public LanguageEndPoint(LanguageHandler languageHandler) {
        this.languageHandler = languageHandler;
    }

    /**
     * Retrieves all available languages.
     *
     * @return a Flux stream of Language objects
     */
    @GetMapping("/")
    @Operation(summary = "Get all languages", description = "Fetch all languages from the database")
    public Flux<Language> findAll() {
        return languageHandler.findAll();
    }

    /**
     * Finds a language by its ID.
     *
     * @param id the UUID of the language
     * @return a Mono containing the found language or a NOT FOUND response
     */
    @GetMapping(value = "/id/{id}")
    @Operation(summary = "Find language by ID", description = "Retrieve a language by its unique identifier")
    public Mono<ResponseEntity<Language>> findById(@PathVariable String id) {
        return languageHandler.findById(UUID.fromString(id))
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * Saves a new language.
     *
     * @param language the language to save
     * @return a Mono containing the saved language
     */
    @PostMapping("/")
    @Operation(summary = "Save a new language", description = "Create and store a new language entry")
    public Mono<ResponseEntity<Language>> save(@RequestBody Language language) {
        return languageHandler.save(language)
                .map(ResponseEntity::ok);
    }

    /**
     * Deletes a language by its ID.
     *
     * @param id the UUID of the language to delete
     * @return a Mono containing the deletion result
     */
    @DeleteMapping(value = "/id/{id}")
    @Operation(summary = "Delete language by ID", description = "Remove a language entry by its unique identifier")
    public Mono<ResponseEntity<Boolean>> delete(@PathVariable String id) {
        return languageHandler.delete(UUID.fromString(id))
                .map(ResponseEntity::ok);
    }
}