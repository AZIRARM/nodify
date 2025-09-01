package com.itexpert.content.api.endpoints;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itexpert.content.api.handlers.ContentNodeHandler;
import com.itexpert.content.api.handlers.RedisHandler;
import com.itexpert.content.lib.enums.StatusEnum;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;

@Slf4j
@RestController
@RequestMapping(value = "/contents")
@AllArgsConstructor
@Tag(name = "Content Node Endpoint", description = "APIs for managing content nodes")
public class ContentNodeEndPoint {

    private final ContentNodeHandler contentNodeHandler;
    private final RedisHandler redisHandler;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Operation(summary = "Retrieve all content nodes by node code")
    @GetMapping(value = "/node/code/{code}")
    public Flux<Object> findAllByNodeCode(@PathVariable String code,
                                          @RequestParam(required = false, defaultValue = "PUBLISHED") StatusEnum status,
                                          @RequestParam(required = false) String translation,
                                          @RequestParam(required = false) boolean fillValues,
                                          @RequestParam(required = false) boolean payloadOnly) {

        String cacheKey = "node:code:" + code + ":" + status + ":" + translation + ":" + fillValues + ":" + payloadOnly;

        return redisHandler.get(cacheKey)
                .flatMapMany(bytes -> {
                    try {
                        List<Object> list = objectMapper.readValue(bytes, List.class);
                        return Flux.fromIterable(list);
                    } catch (Exception e) {
                        log.error("Erreur de désérialisation cache Redis", e);
                        return Flux.empty();
                    }
                })
                .switchIfEmpty(
                        contentNodeHandler.findAllByNodeCode(code, status, translation, fillValues)
                                .map(view -> payloadOnly ? view.getPayload() : view)
                                .collectList()
                                .flatMapMany(list -> {
                                    try {
                                        byte[] bytes = objectMapper.writeValueAsBytes(list);
                                        return redisHandler.set(cacheKey, bytes, Duration.ofMinutes(1))
                                                .thenMany(Flux.fromIterable(list));
                                    } catch (Exception e) {
                                        return Flux.fromIterable(list);
                                    }
                                })
                );
    }

    @Operation(summary = "Retrieve a content node by its unique code")
    @GetMapping(value = "/code/{code}")
    public Mono<ResponseEntity<Object>> findByCode(@PathVariable String code,
                                                   @RequestParam(required = false, defaultValue = "PUBLISHED") StatusEnum status,
                                                   @RequestParam(required = false) String translation,
                                                   @RequestParam(required = false) boolean payloadOnly) {

        String cacheKey = "content:code:" + code + ":" + status + ":" + translation + ":" + payloadOnly;

        return redisHandler.get(cacheKey)
                .flatMap(bytes -> {
                    try {
                        Object obj = objectMapper.readValue(bytes, Object.class);
                        return Mono.just(ResponseEntity.ok(obj));
                    } catch (Exception e) {
                        return Mono.empty();
                    }
                })
                .switchIfEmpty(
                        contentNodeHandler.findByCodeAndStatus(code, status, translation)
                                .map(view -> payloadOnly ? view.getPayload() : view)
                                .map(ResponseEntity::ok)
                                .flatMap(resp -> {
                                    try {
                                        byte[] bytes = objectMapper.writeValueAsBytes(resp.getBody());
                                        return redisHandler.set(cacheKey, bytes, Duration.ofMinutes(1))
                                                .thenReturn(resp);
                                    } catch (Exception e) {
                                        return Mono.just(resp);
                                    }
                                })
                                .defaultIfEmpty(new ResponseEntity<>(HttpStatus.NOT_FOUND))
                );
    }

    @Operation(summary = "Retrieve a content node by its unique slug")
    @GetMapping(value = "/{slug}")
    public Mono<ResponseEntity<Object>> findBySlug(@PathVariable String slug,
                                                   @RequestParam(required = false, defaultValue = "PUBLISHED") StatusEnum status,
                                                   @RequestParam(required = false) String translation,
                                                   @RequestParam(required = false) boolean payloadOnly) {

        String cacheKey = "content:slug:" + slug + ":" + status + ":" + translation + ":" + payloadOnly;

        return redisHandler.get(cacheKey)
                .flatMap(bytes -> {
                    try {
                        Object obj = objectMapper.readValue(bytes, Object.class);
                        return Mono.just(ResponseEntity.ok(obj));
                    } catch (Exception e) {
                        return Mono.empty();
                    }
                })
                .switchIfEmpty(
                        contentNodeHandler.findBySlugAndStatus(slug, status, translation)
                                .map(view -> payloadOnly ? view.getPayload() : view)
                                .map(ResponseEntity::ok)
                                .flatMap(resp -> {
                                    try {
                                        byte[] bytes = objectMapper.writeValueAsBytes(resp.getBody());
                                        return redisHandler.set(cacheKey, bytes, Duration.ofMinutes(1))
                                                .thenReturn(resp);
                                    } catch (Exception e) {
                                        return Mono.just(resp);
                                    }
                                })
                                .defaultIfEmpty(new ResponseEntity<>(HttpStatus.NOT_FOUND))
                );
    }

    @GetMapping(value = "/code/{code}/file")
    public Mono<ResponseEntity<byte[]>> getContentAsFileDataFromCode(
            @PathVariable String code,
            @RequestParam(required = false, defaultValue = "PUBLISHED") StatusEnum status
    ) {
        String cacheKey = "file:" + code + ":" + status;

        return redisHandler.get(cacheKey)
                .flatMap(cachedBytes ->
                        Mono.just(ResponseEntity.ok()
                                .contentLength(cachedBytes.length)
                                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                                .header("Content-Disposition", "attachment; filename=\"" + code + "\"")
                                .body(cachedBytes)))
                .switchIfEmpty(
                        contentNodeHandler.findResourceByCode(code, status)
                                .flatMap(contentFile -> {
                                    byte[] bytes;
                                    String partSeparator = ",";
                                    if (contentFile.getData().contains(partSeparator)) {
                                        String encodedImg = contentFile.getData().split(partSeparator)[1];
                                        bytes = Base64.getDecoder().decode(encodedImg.getBytes(StandardCharsets.UTF_8));
                                    } else {
                                        bytes = Base64.getDecoder().decode(contentFile.getData());
                                    }

                                    return redisHandler.set(cacheKey, bytes, Duration.ofMinutes(5))
                                            .thenReturn(ResponseEntity.ok()
                                                    .contentLength(contentFile.getSize())
                                                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                                                    .header("Content-Disposition", "attachment; filename=\"" + contentFile.getName() + "\"")
                                                    .body(bytes));
                                })
                );
    }

    @Operation(summary = "Retrieve content node file data as a downloadable file")
    @GetMapping(value = "/{slug}/file")
    public Mono<ResponseEntity<byte[]>> getContentAsFileDataFromSlug(
            @PathVariable String slug,
            @RequestParam(required = false, defaultValue = "PUBLISHED") StatusEnum status
    ) {
        String cacheKey = "file:slug:" + slug + ":" + status;

        return redisHandler.get(cacheKey)
                .flatMap(cachedBytes ->
                        Mono.just(ResponseEntity.ok()
                                .contentLength(cachedBytes.length)
                                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                                .header("Content-Disposition", "attachment; filename=\"" + slug + "\"")
                                .body(cachedBytes)))
                .switchIfEmpty(
                        contentNodeHandler.findResourceBySlug(slug, status)
                                .flatMap(contentFile -> {
                                    byte[] bytes;
                                    String partSeparator = ",";
                                    if (contentFile.getData().contains(partSeparator)) {
                                        String encodedImg = contentFile.getData().split(partSeparator)[5];
                                        bytes = Base64.getDecoder().decode(encodedImg.getBytes(StandardCharsets.UTF_8));
                                    } else {
                                        bytes = Base64.getDecoder().decode(contentFile.getData());
                                    }

                                    return redisHandler.set(cacheKey, bytes, Duration.ofMinutes(1))
                                            .thenReturn(ResponseEntity.ok()
                                                    .contentLength(contentFile.getSize())
                                                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                                                    .header("Content-Disposition", "attachment; filename=\"" + contentFile.getName() + "\"")
                                                    .body(bytes));
                                })
                );
    }

}
