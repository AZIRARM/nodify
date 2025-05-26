package com.itexpert.content.core.endpoints;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.itexpert.content.core.handlers.PluginHandler;
import com.itexpert.content.lib.models.Plugin;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping(value = "/v0/plugins")
public class PluginEndPoint {

    private final PluginHandler pluginHandler;


    @GetMapping("/")
    public Flux<Plugin> findNotDeleted() {
        return pluginHandler.findNotDeleted();
    }

    @GetMapping("/deleteds")
    public Flux<Plugin> deleteds() {
        return pluginHandler.deleteds();
    }

    @GetMapping(value = "/id/{id}")
    public Mono<ResponseEntity<Plugin>> findById(@PathVariable String id) {
        return pluginHandler.findById(UUID.fromString(id))
                .map(ResponseEntity::ok)
                .defaultIfEmpty(new ResponseEntity<>(HttpStatus.NOT_FOUND));

    }

    @GetMapping(value = "/name/{name}")
    public Mono<ResponseEntity<Plugin>> findByName(@PathVariable String name) {
        return pluginHandler.findByName(name)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(new ResponseEntity<>(HttpStatus.NOT_FOUND));

    }

    @PostMapping("/")
    public Mono<ResponseEntity<Plugin>> save(@RequestBody(required = true) Plugin plugin) {
        return pluginHandler.save(plugin)
                .map(ResponseEntity::ok);
    }

    @DeleteMapping(value = "/id/{id}/user/{userId}")
    public Mono<ResponseEntity<Boolean>> delete(@PathVariable String id, @PathVariable String userId) {
        return pluginHandler.delete(UUID.fromString(id), UUID.fromString(userId))
                .map(ResponseEntity::ok);
    }

    @PutMapping(value = "/name/{name}/user/{userId}/enable")
    public Mono<ResponseEntity<Plugin>> enable(@PathVariable String name, @PathVariable String userId) {
        return pluginHandler.enable(name, UUID.fromString(userId))
                .map(ResponseEntity::ok);
    }

    @PutMapping(value = "/name/{name}/user/{userId}/disable")
    public Mono<ResponseEntity<Plugin>> disable(@PathVariable String name, @PathVariable String userId) {
        return pluginHandler.disable(name, UUID.fromString(userId))
                .map(ResponseEntity::ok);
    }

    @PutMapping(value = "/name/{name}/user/{userId}/activate")
    public Mono<ResponseEntity<Plugin>> activate(@PathVariable String name, @PathVariable String userId) {
        return pluginHandler.activate(name, UUID.fromString(userId))
                .map(ResponseEntity::ok);
    }

    @DeleteMapping(value = "/id/{id}/user/{userId}/deleteDefinitively")
    public Mono<ResponseEntity<Boolean>> deleteDefinitively(@PathVariable String id, @PathVariable String userId) {
        return pluginHandler.deleteDefinitively(UUID.fromString(id), UUID.fromString(userId))
                .map(ResponseEntity::ok);
    }

    @GetMapping(value = "/name/{name}/export")
    public Mono<ResponseEntity<byte[]>> export(@PathVariable String name) {
        return this.pluginHandler.export(name)
                .map(plugin -> {
                    Gson gson = new GsonBuilder()
                            .setPrettyPrinting() // optionnel, pour lisibilité
                            .serializeNulls()    // pour ne pas ignorer les champs nulls
                            .create();
                    String json = gson.toJson(plugin); // ICI : plugin est un objet, pas une liste
                    byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
                    return ResponseEntity.ok()
                            .contentLength(bytes.length)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Content-Disposition", "attachment; filename=\"plugin-" + plugin.getName() + ".json\"")
                            .body(bytes);
                });
    }


    @PostMapping(value = "/import")
    public Mono<ResponseEntity<Plugin>> importContentNode(@RequestBody Plugin plugin) {
        return this.pluginHandler.importPlugin(plugin)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(new ResponseEntity<>(HttpStatus.NOT_FOUND));

    }


}
