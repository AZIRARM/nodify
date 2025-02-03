package com.itexpert.content.core.endpoints;

import com.itexpert.content.core.handlers.LanguageHandler;
import com.itexpert.content.lib.models.Language;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping(value = "/v0/languages")
public class LanguageEndPoint {

    private final LanguageHandler languageHandler;

    public LanguageEndPoint(LanguageHandler languageHandler) {
        this.languageHandler = languageHandler;
    }

    @GetMapping
    public Flux<Language> findAll() {
        return languageHandler.findAll();
    }

    @GetMapping(value = "/id/{id}")
    public Mono<ResponseEntity<Language>> findById(@PathVariable String id) {
        return languageHandler.findById(UUID.fromString(id))
                .map(ResponseEntity::ok)
                .defaultIfEmpty(new ResponseEntity<>(HttpStatus.NOT_FOUND));

    }

    //@RolesAllowed("ADMIN")
    @PostMapping
    public Mono<ResponseEntity<Language>> save(@RequestBody(required = true) Language language) {
        return languageHandler.save(language)
                .map(ResponseEntity::ok);
    }

    //@RolesAllowed("ADMIN")
    @DeleteMapping(value = "/id/{id}")
    public Mono<ResponseEntity<Boolean>> delete(@PathVariable String id) {
        return languageHandler.delete(UUID.fromString(id))
                .map(ResponseEntity::ok);
    }

}
