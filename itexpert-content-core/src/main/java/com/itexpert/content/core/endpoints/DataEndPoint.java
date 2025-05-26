package com.itexpert.content.core.endpoints;

import com.itexpert.content.core.handlers.DataHandler;
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

import java.util.UUID;


@RestController
@RequestMapping("/v0/datas")
@AllArgsConstructor
@Tag(name = "Data Controller", description = "API for managing Data objects")
public class DataEndPoint {

    private final DataHandler dataHandler;

    /**
     * Retrieves a Data object by its key.
     *
     * @param key The key of the Data object.
     * @return A Mono containing the ResponseEntity with the Data object if found, or NOT_FOUND if not found.
     */
    @Operation(summary = "Find Data by key", description = "Retrieves a Data object based on its unique key.")
    @GetMapping(value = "/key/{key}")
    public Mono<ResponseEntity<Data>> findByKey(@Parameter(description = "The key of the Data object") @PathVariable String key) {
        return dataHandler.findByKey(key)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * Retrieves a list of Data objects by content code.
     *
     * @param code        The content code.
     * @param currentPage The current page index (optional, default: 0).
     * @param limit       The number of items per page (optional, default: 50).
     * @return A Flux containing a list of Data objects.
     */
    @Operation(summary = "Find Data by content code", description = "Retrieves a list of Data objects associated with a given content code.")
    @GetMapping(value = "/contentCode/{code}")
    public Flux<Data> findByContentCode(
            @Parameter(description = "The content code") @PathVariable String code,
            @Parameter(description = "Current page index", example = "0") @RequestParam(name = "currentPage", required = false, defaultValue = "0") Integer currentPage,
            @Parameter(description = "Limit per page", example = "50") @RequestParam(name = "limit", required = false, defaultValue = "50") Integer limit) {
        return dataHandler.findByContentCode(code, currentPage, limit);
    }

    /**
     * Saves a new Data object.
     *
     * @param data The Data object to be saved.
     * @return A Mono containing the ResponseEntity with the saved Data object.
     */
    @Operation(summary = "Save Data", description = "Saves a new Data object in the system.")
    @PostMapping("/")
    public Mono<ResponseEntity<Data>> save(@RequestBody Data data) {
        return dataHandler.save(data)
                .map(ResponseEntity::ok);
    }

    /**
     * Deletes all Data objects by content code.
     *
     * @param code The content code.
     * @return A Mono containing the ResponseEntity with a boolean indicating success.
     */
    @Operation(summary = "Delete Data by content code", description = "Deletes all Data objects associated with the given content code.")
    @DeleteMapping(value = "/contentCode/{code}")
    public Mono<ResponseEntity<Boolean>> deleteAllByContentNodeCode(@Parameter(description = "The content code") @PathVariable String code) {
        return dataHandler.deleteAllByContentNodeCode(code)
                .map(ResponseEntity::ok);
    }

    /**
     * Deletes a Data object by its key.
     *
     * @param uuid The id of the Data object.
     * @return A Mono containing the ResponseEntity with a boolean indicating success.
     */
    @Operation(summary = "Delete Data by key", description = "Deletes a Data object based on its unique key.")
    @DeleteMapping(value = "/id/{uuid}")
    public Mono<ResponseEntity<Boolean>> delete(@Parameter(description = "The uuid of the Data object") @PathVariable String uuid) {
        return dataHandler.delete(UUID.fromString(uuid))
                .map(ResponseEntity::ok);
    }

    /**
     * Count all Datas by content node code.
     *
     * @param code The id of the ContentNode.
     * @return A Mono containing the ResponseEntity with a Long indicating the total off datas objects to the database.
     */
    @Operation(summary = "Count all Data by Content Code", description = "Count all Data by Content.")
    @GetMapping(value = "/contentCode/{code}/count")
    public Mono<ResponseEntity<Long>> countByContentNodeCode(@PathVariable String code) {
        return dataHandler.countByContentNodeCode(code)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(new ResponseEntity<>(HttpStatus.NOT_FOUND));

    }
}

