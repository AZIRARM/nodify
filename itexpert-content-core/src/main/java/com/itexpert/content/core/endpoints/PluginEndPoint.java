package com.itexpert.content.core.endpoints;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.itexpert.content.core.handlers.PluginHandler;
import com.itexpert.content.core.handlers.UserHandler;
import com.itexpert.content.lib.models.Plugin;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping(value = "/v0/plugins")
public class PluginEndPoint {

    private final PluginHandler pluginHandler;
    private final UserHandler userHandler;


    @GetMapping("/")
    public Flux<Plugin> findNotDeleted() {
        return pluginHandler.findNotDeleted();
    }

    @GetMapping("/deleteds")
    public Flux<Plugin> deleteds() {
        return pluginHandler.deleteds();
    }

    @GetMapping(value = "/id/{id}")
    public Mono<ResponseEntity<Plugin>> findById(@PathVariable UUID id) {
        return pluginHandler.findById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(new ResponseEntity<>(HttpStatus.NOT_FOUND));

    }

    @PostMapping("/")
    public Mono<ResponseEntity<Plugin>> save(@RequestBody(required = true) Plugin plugin) {
        return pluginHandler.save(plugin)
                .map(ResponseEntity::ok);
    }

    @DeleteMapping(value = "/id/{id}")
    public Mono<ResponseEntity<Boolean>> delete(@PathVariable UUID id, Authentication authentication) {
        return
                this.extractUser(authentication.getPrincipal().toString())
                        .flatMap(user ->
                                pluginHandler.delete(id, user))
                        .map(ResponseEntity::ok);
    }

    @PutMapping(value = "/id/{id}/enable")
    public Mono<ResponseEntity<Plugin>> enable(@PathVariable UUID id, Authentication authentication) {
        return
                this.extractUser(authentication.getPrincipal().toString())
                        .flatMap(user -> pluginHandler.enable(id, user)
                                .map(ResponseEntity::ok)
                        );
    }

    @PutMapping(value = "/id/{id}/disable")
    public Mono<ResponseEntity<Plugin>> disable(@PathVariable UUID id, Authentication authentication) {
        return
                this.extractUser(authentication.getPrincipal().toString())
                        .flatMap(user -> pluginHandler.disable(id, user)
                                .map(ResponseEntity::ok));
    }

    @PutMapping(value = "/id/{id}/activate")
    public Mono<ResponseEntity<Plugin>> activate(@PathVariable UUID id, Authentication authentication) {
        return
                this.extractUser(authentication.getPrincipal().toString())
                        .flatMap(user -> pluginHandler.activate(id, user)
                                .map(ResponseEntity::ok));
    }

    @DeleteMapping(value = "/id/{id}/deleteDefinitively")
    public Mono<ResponseEntity<Boolean>> deleteDefinitively(@PathVariable UUID id, Authentication authentication) {
        return
                this.extractUser(authentication.getPrincipal().toString())
                        .flatMap(user -> pluginHandler.deleteDefinitively(id, user)
                                .map(ResponseEntity::ok)
                        );
    }

    @GetMapping(value = "/id/{id}/export")
    public Mono<ResponseEntity<byte[]>> export(@PathVariable UUID id) {
        return this.pluginHandler.export(id)
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

    private Mono<String> extractUser(String email) {
        if (ObjectUtils.isEmpty(email)) {
            return Mono.just("");
        }

        return this.userHandler.findByEmail(email)
                .map(userPost -> userPost.getFirstname() + " " + userPost.getLastname())
                .defaultIfEmpty("");
    }
}
