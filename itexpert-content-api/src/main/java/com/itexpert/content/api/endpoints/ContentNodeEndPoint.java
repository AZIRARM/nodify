package com.itexpert.content.api.endpoints;

import com.itexpert.content.api.handlers.ContentNodeHandler;
import com.itexpert.content.api.utils.ContentNodeView;
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
import java.util.Base64;

@Slf4j
@RestController
@RequestMapping(value = "/contents")
@AllArgsConstructor
@Tag(name = "Content Node Endpoint", description = "APIs for managing content nodes")
/**
 * REST controller for managing content nodes.
 * Provides endpoints to retrieve and manipulate content nodes based on different criteria.
 */
public class ContentNodeEndPoint {

    private final ContentNodeHandler contentNodeHandler;

    @Operation(summary = "Retrieve all content nodes by node code")
    @GetMapping(value = "/node/code/{code}")
    public Flux<Object> findAllByNodeCode(@PathVariable String code,
                                          @RequestParam(required = false, defaultValue = "PUBLISHED") StatusEnum status,
                                          @RequestParam(required = false) String translation,
                                          @RequestParam(required = false) boolean fillValues,
                                          @RequestParam(required = false) boolean payloadOnly) {
        Flux<ContentNodeView> contentViews = contentNodeHandler.findAllByNodeCode(code, status, translation, fillValues);
        if (payloadOnly)
            return contentViews.map(ContentNodeView::getPayload);
        return contentViews.map(contentNodeView -> (Object) contentNodeView);
    }

    @Operation(summary = "Retrieve a content node by its unique code")
    @GetMapping(value = "/code/{code}")
    public Mono<ResponseEntity<Object>> findByCode(@PathVariable String code,
                                                   @RequestParam(required = false, defaultValue = "PUBLISHED") StatusEnum status,
                                                   @RequestParam(required = false) String translation,
                                                   @RequestParam(required = false) boolean payloadOnly) {

        Mono<ContentNodeView> contentView = contentNodeHandler.findByCodeAndStatus(code, status, translation);
        if (payloadOnly) {
            return contentView
                    .doOnNext(contentNodeView -> {
                        log.debug("Found content node with code {}", contentNodeView.getCode());
                    })
                    .map(ContentNodeView::getPayload).map(ResponseEntity::ok)
                    .defaultIfEmpty(new ResponseEntity<>(HttpStatus.NOT_FOUND));
        }
        return contentView
                .map(content -> (Object) content)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @Operation(summary = "Retrieve a content node by its unique slug")
    @GetMapping(value = "/{slug}")
    public Mono<ResponseEntity<Object>> findBySlug(@PathVariable String slug,
                                                   @RequestParam(required = false, defaultValue = "PUBLISHED") StatusEnum status,
                                                   @RequestParam(required = false) String translation,
                                                   @RequestParam(required = false) boolean payloadOnly) {


        Mono<ContentNodeView> contentView = contentNodeHandler.findBySlugAndStatus(slug, status, translation);

        if (payloadOnly) {
            return contentView
                    .doOnNext(contentNodeView -> {
                        log.debug("Found content node with code {}", contentNodeView.getCode());
                    })
                    .map(ContentNodeView::getPayload).map(ResponseEntity::ok)
                    .defaultIfEmpty(new ResponseEntity<>(HttpStatus.NOT_FOUND));
        }
        return contentView
                .map(content -> (Object) content)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @Operation(summary = "Retrieve content node file data as a downloadable file")
    @GetMapping(value = "/code/{code}/file")
    public Mono<ResponseEntity<byte[]>> getContentAsFileDataFromCode(
            @PathVariable String code,
            @RequestParam(required = false, defaultValue = "PUBLISHED") StatusEnum status
    ) {
        return contentNodeHandler.findResourceByCode(code, status).map(contentFile -> {

            byte[] bytes = null;
            String partSeparator = ",";
            if (contentFile.getData().contains(partSeparator)) {
                String encodedImg = contentFile.getData().split(partSeparator)[1];
                bytes = Base64.getDecoder().decode(encodedImg.getBytes(StandardCharsets.UTF_8));
            } else {
                bytes = Base64.getDecoder().decode(contentFile.getData());
            }

            return ResponseEntity.ok()
                    .contentLength(contentFile.getSize())
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header("Content-Disposition", "attachment; filename=\"" + contentFile.getName() + "\"")
                    .body(bytes);
        });
    }

    @Operation(summary = "Retrieve content node file data as a downloadable file")
    @GetMapping(value = "/{slug}/file")
    public Mono<ResponseEntity<byte[]>> getContentAsFileDataFromSlug(
            @PathVariable String slug,
            @RequestParam(required = false, defaultValue = "PUBLISHED") StatusEnum status
    ) {

        return contentNodeHandler.findResourceBySlug(slug, status).map(contentFile -> {

            byte[] bytes = null;
            String partSeparator = ",";
            if (contentFile.getData().contains(partSeparator)) {
                String encodedImg = contentFile.getData().split(partSeparator)[1];
                bytes = Base64.getDecoder().decode(encodedImg.getBytes(StandardCharsets.UTF_8));
            } else {
                bytes = Base64.getDecoder().decode(contentFile.getData());
            }

            return ResponseEntity.ok()
                    .contentLength(contentFile.getSize())
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header("Content-Disposition", "attachment; filename=\"" + contentFile.getName() + "\"")
                    .body(bytes);
        });
    }
}
