package com.itexpert.content.core.endpoints;

import com.itexpert.content.core.handlers.PluginFileHandler;
import com.itexpert.content.lib.models.PluginFile;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping(value = "/v0/plugin-files")
public class PluginFileEndPoint {

    private final PluginFileHandler pluginFileHandler;

    @GetMapping("/")
    public Flux<PluginFile> findAll(@RequestParam(name = "enabled", required = false) Boolean enabled) {
        return pluginFileHandler.findAll();
    }

    @GetMapping(value = "/id/{id}")
    public Mono<ResponseEntity<PluginFile>> findById(@PathVariable String id) {
        return pluginFileHandler.findById(UUID.fromString(id))
                .map(ResponseEntity::ok)
                .defaultIfEmpty(new ResponseEntity<>(HttpStatus.NOT_FOUND));

    }

    @GetMapping(value = "/plugin/{id}")
    public Flux<PluginFile> findByPluginId(@PathVariable String id) {
        return pluginFileHandler.findByPluginId(UUID.fromString(id));

    }

    @GetMapping(value = "/plugin/name/{name}")
    public Flux<PluginFile> findByPluginName(@PathVariable String name) {
        return pluginFileHandler.findByPluginName(name);

    }

    @PostMapping("/")
    public Mono<ResponseEntity<PluginFile>> save(@RequestBody(required = true) PluginFile pluginFile) {
        return pluginFileHandler.save(pluginFile)
                .map(ResponseEntity::ok);
    }

    @DeleteMapping(value = "/id/{id}")
    public Mono<ResponseEntity<Boolean>> delete(@PathVariable String id) {
        return pluginFileHandler.delete(UUID.fromString(id))
                .map(ResponseEntity::ok);
    }
}
