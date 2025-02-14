package com.itexpert.content.api.endpoints;

import com.itexpert.content.api.handlers.ContentNodeHandler;
import com.itexpert.content.api.utils.ContentNodeView;
import com.itexpert.content.lib.enums.StatusEnum;
import com.itexpert.content.lib.models.Value;
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
@RequestMapping(value = "/v0/contents")
@AllArgsConstructor
public class ContentNodeEndPoint {

    private final ContentNodeHandler contentNodeHandler;

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

    @GetMapping(value = "/code/{code}")
    public Mono<ResponseEntity<Object>> findByCode(@PathVariable String code,
                                                   @RequestParam(required = false, defaultValue = "PUBLISHED") StatusEnum status,
                                                   @RequestParam(required = false) String translation,
                                                   @RequestParam(required = false) boolean payloadOnly) {

        Mono<ContentNodeView> contentView = contentNodeHandler.findByCodeAndStatus(code, status, translation);
        if (payloadOnly) {
            return contentView.map(ContentNodeView::getPayload).map(ResponseEntity::ok)
                    .defaultIfEmpty(new ResponseEntity<>(HttpStatus.NOT_FOUND));
        }
        return contentView
                .map(content -> (Object) content)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }


    @GetMapping(value = "/code/{code}/file")
    public Mono<ResponseEntity<byte[]>> getContentAsFileData(
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

    @PatchMapping(value = "/code/{code}/data")
    public Mono<Value> saveData(@PathVariable String code, @RequestBody Value value) {
        return contentNodeHandler.saveData(code, value);
    }

    @GetMapping(value = "/code/{code}/key/{key}/data")
    public Mono<Value> findDataByKey(@PathVariable String code,
                                     @PathVariable String key,
                                     @RequestParam(required = false, defaultValue = "PUBLISHED") StatusEnum status) {
        return contentNodeHandler.getValueByContentNodeCodeAndKey(code, key, status);
    }

    @GetMapping(value = "/code/{code}/data")
    public Flux<Value> findDataByCode(@PathVariable String code,
                                      @RequestParam(required = false, defaultValue = "PUBLISHED") StatusEnum status) {
        return contentNodeHandler.getValueByContentNodeCode(code, status);
    }
}
