
package com.itexpert.content.api.endpoints;

import com.itexpert.content.api.handlers.DataHandler;
import com.itexpert.content.lib.models.Data;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/datas")
@AllArgsConstructor
@Tag(name = "Data Controller", description = "API for managing Data objects")
public class DataEndPoint {

    private final DataHandler dataHandler;

    /**
     * Retrieves Data objects by content code.
     * Supports filtering by key or name, as well as pagination.
     */
    @Operation(
        summary = "Find Data by content code",
        description = "Retrieves Data objects associated with a content code, optionally filtered by key or name."
    )
    @GetMapping(value = "/contents/{code}")
    public Flux<Data> findByContentCode(
            @Parameter(description = "The content code")
            @PathVariable String code,

            @Parameter(description = "The key of the Data object")
            @RequestParam(name = "key", required = false) String key,

            @Parameter(description = "The name of the Data object")
            @RequestParam(name = "name", required = false) String name,

            @Parameter(description = "Current page index", example = "0")
            @RequestParam(name = "currentPage", required = false, defaultValue = "0") Integer currentPage,

            @Parameter(description = "Limit per page", example = "50")
            @RequestParam(name = "limit", required = false, defaultValue = "50") Integer limit) {

        if (key != null) {
            return dataHandler.findByContentNodeCodeAndKey(code, key)
                    .flux();
        }

        if (name != null) {
            return dataHandler.findByContentNodeCodeAndName(code, name);
        }

        return dataHandler.findByContentCode(code, currentPage, limit);
    }

    /**
     * Counts data objects by content code.
     */
    @Operation(
        summary = "Count data by content code",
        description = "Returns the number of data entries associated with the given content code."
    )
    @GetMapping("/content-code/{code}/count")
    public Mono<Long> countByContentCode(
            @Parameter(description = "The content code")
            @PathVariable String code) {

        return dataHandler.countByContentCode(code);
    }

    @Operation(
        summary = "Count all Data",
        description = "Returns the total number of data entries."
    )
    @GetMapping("/count")
    public Mono<Long> countAll() {
        return dataHandler.countAll();
    }

    @Operation(
        summary = "Count Data by content code and type",
        description = "Returns the number of data entries matching the given content code and data type."
    )
    @GetMapping("/content-code/{code}/type/{dataType}/count")
    public Mono<Long> countByContentCodeAndDataType(
            @Parameter(description = "The content code")
            @PathVariable String code,

            @Parameter(description = "The data type")
            @PathVariable String dataType) {

        return dataHandler.countByContentNodeCodeAndDataType(code, dataType);
    }

    @Operation(
        summary = "Count Data by content code and user",
        description = "Returns the number of data entries matching the given content code and user."
    )
    @GetMapping("/content-code/{code}/user/{user}/count")
    public Mono<Long> countByContentCodeAndUser(
            @Parameter(description = "The content code")
            @PathVariable String code,

            @Parameter(description = "The user identifier")
            @PathVariable String user) {

        return dataHandler.countByContentNodeCodeAndUser(code, user);
    }

    @Operation(
        summary = "Count Data by type",
        description = "Returns the number of data entries matching the given data type."
    )
    @GetMapping("/type/{dataType}/count")
    public Mono<Long> countByDataType(
            @Parameter(description = "The data type")
            @PathVariable String dataType) {

        return dataHandler.countByDataType(dataType);
    }

    @Operation(
        summary = "Count Data by user",
        description = "Returns the number of data entries matching the given user."
    )
    @GetMapping("/user/{user}/count")
    public Mono<Long> countByUser(
            @Parameter(description = "The user identifier")
            @PathVariable String user) {

        return dataHandler.countByUser(user);
    }

    /**
     * Retrieves a Data object by its UUID.
     */
    @Operation(
        summary = "Find Data by ID",
        description = "Retrieves a Data object based on its unique UUID."
    )
    @GetMapping(value = "/id/{id}")
    public Mono<Data> findById(
            @Parameter(description = "The UUID of the Data object")
            @PathVariable String id) {

        return dataHandler.findById(UUID.fromString(id));
    }

    /**
     * Retrieves all Data objects with pagination.
     */
    @Operation(
        summary = "Find all Data",
        description = "Retrieves a paginated list of all Data objects."
    )
    @GetMapping(value = "/")
    public Flux<Data> findAll(
            @Parameter(description = "Current page index", example = "0")
            @RequestParam(name = "currentPage", required = false, defaultValue = "0") Integer currentPage,

            @Parameter(description = "Limit per page", example = "50")
            @RequestParam(name = "limit", required = false, defaultValue = "50") Integer limit) {

        return dataHandler.findAll(currentPage, limit);
    }

    /**
     * Retrieves a list of Data objects by data type.
     */
    @Operation(
        summary = "Find Data by type",
        description = "Retrieves a paginated list of Data objects associated with a given dataType."
    )
    @GetMapping(value = "/type/{dataType}")
    public Flux<Data> findByDataType(
            @Parameter(description = "The data type")
            @PathVariable String dataType,

            @Parameter(description = "Current page index", example = "0")
            @RequestParam(name = "currentPage", required = false, defaultValue = "0") Integer currentPage,

            @Parameter(description = "Limit per page", example = "50")
            @RequestParam(name = "limit", required = false, defaultValue = "50") Integer limit) {

        return dataHandler.findByDataType(dataType, currentPage, limit);
    }

    /**
     * Retrieves a list of Data objects by user.
     */
    @Operation(
        summary = "Find Data by user",
        description = "Retrieves a paginated list of Data objects associated with a given user."
    )
    @GetMapping(value = "/user/{user}")
    public Flux<Data> findByUser(
            @Parameter(description = "The user identifier")
            @PathVariable String user,

            @Parameter(description = "Current page index", example = "0")
            @RequestParam(name = "currentPage", required = false, defaultValue = "0") Integer currentPage,

            @Parameter(description = "Limit per page", example = "50")
            @RequestParam(name = "limit", required = false, defaultValue = "50") Integer limit) {

        return dataHandler.findByUser(user, currentPage, limit);
    }

    /**
     * Searches for Data objects by content code and keyword.
     */
    @Operation(
        summary = "Search Data by keyword",
        description = "Searches for Data objects where the name, value, or key match the given keyword."
    )
    @GetMapping(value = "/contentCode/{code}/search")
    public Flux<Data> searchByKeyword(
            @Parameter(description = "The content code")
            @PathVariable String code,

            @Parameter(description = "The search keyword")
            @RequestParam(name = "keyword") String keyword,

            @Parameter(description = "Current page index", example = "0")
            @RequestParam(name = "currentPage", required = false, defaultValue = "0") Integer currentPage,

            @Parameter(description = "Limit per page", example = "50")
            @RequestParam(name = "limit", required = false, defaultValue = "50") Integer limit) {

        return dataHandler.searchByKeyword(code, keyword, currentPage, limit);
    }

    /**
     * Updates an existing Data object.
     */
    @Operation(
        summary = "Update Data",
        description = "Updates an existing Data object by its UUID."
    )
    @PutMapping(value = "/id/{id}")
    public Mono<ResponseEntity<Data>> update(
            @Parameter(description = "The UUID of the Data object")
            @PathVariable String id,

            @RequestBody Data data) {

        return dataHandler.update(UUID.fromString(id), data)
                .map(ResponseEntity::ok);
    }

    /**
     * Saves a new Data object.
     */
    @Operation(
        summary = "Save Data",
        description = "Saves a new Data object in the system."
    )
    @PostMapping("/")
    public Mono<ResponseEntity<Data>> save(@RequestBody Data data) {

        return dataHandler.save(data)
                .map(ResponseEntity::ok);
    }

    /**
     * Deletes all Data objects by content code.
     */
    @Operation(
        summary = "Delete Data by content code",
        description = "Deletes all Data objects associated with the given content code."
    )
    @DeleteMapping(value = "/contentCode/{code}")
    public Mono<ResponseEntity<Boolean>> deleteAllByContentNodeCode(
            @Parameter(description = "The content code")
            @PathVariable String code) {

        return dataHandler.deleteAllByContentNodeCode(code)
                .map(ResponseEntity::ok);
    }

    /**
     * Deletes a Data object by its content code and key.
     */
    @Operation(
        summary = "Delete Data by content code and key",
        description = "Deletes a Data object based on its unique key and content code."
    )
    @DeleteMapping(value = "/contents/{code}/key/{key}")
    public Mono<ResponseEntity<Boolean>> deleteByContentCodeAndKey(
            @Parameter(description = "The content code")
            @PathVariable String code,

            @Parameter(description = "The key of the Data object")
            @PathVariable String key) {

        return dataHandler.deleteByContentNodeCodeAndKey(code, key)
                .map(ResponseEntity::ok);
    }

    /**
     * Deletes a Data object by its UUID.
     */
    @Operation(
        summary = "Delete Data by ID",
        description = "Deletes a Data object based on its unique UUID."
    )
    @DeleteMapping(value = "/id/{id}")
    public Mono<ResponseEntity<Boolean>> delete(
            @Parameter(description = "The UUID of the Data object")
            @PathVariable String id) {

        return dataHandler.delete(UUID.fromString(id))
                .map(ResponseEntity::ok);
    }
}
