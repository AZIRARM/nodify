package com.itexpert.content.core.endpoints;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.itexpert.content.core.handlers.NodeHandler;
import com.itexpert.content.core.models.auth.RoleEnum;
import com.itexpert.content.lib.enums.NotificationEnum;
import com.itexpert.content.lib.enums.StatusEnum;
import com.itexpert.content.lib.models.Node;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping(value = "/v0/nodes")
public class NodeEndPoint {

    private final NodeHandler nodeHandler;

    @GetMapping("/")
    public Flux<Node> findAll() {
        return nodeHandler.findAll()
                .flatMap(nodeHandler::setPublicationStatus)
                .sort((node1, node2) -> Boolean.compare(node2.isFavorite(), node1.isFavorite()));
    }

    @GetMapping("/origin")
    public Flux<Node> findParentOrigin() {
        return nodeHandler.findParentOrigin()
                .flatMap(nodeHandler::setPublicationStatus)
                .sort((node1, node2) -> Boolean.compare(node2.isFavorite(), node1.isFavorite()));
    }


    @GetMapping("/status/{status}")
    public Flux<Node> findAllByStatus(@PathVariable String status, Authentication authentication) {
        var grantedAuthority = authentication.getAuthorities().stream().findFirst().get();

        if (grantedAuthority.getAuthority().equals(RoleEnum.ADMIN.name())) {
            return nodeHandler.findAllByStatus(status)
                    .flatMap(nodeHandler::setPublicationStatus)
                    .sort((node1, node2) -> Boolean.compare(node2.isFavorite(), node1.isFavorite()));
        }

        return nodeHandler.findAllByStatusAndUser(status, authentication.getPrincipal().toString())
                .flatMap(nodeHandler::setPublicationStatus)
                .sort((node1, node2) -> Boolean.compare(node2.isFavorite(), node1.isFavorite()));
    }

    @GetMapping("/published")
    public Flux<Node> published() {
        return nodeHandler.findAllByStatus(StatusEnum.PUBLISHED.name())
                .flatMap(nodeHandler::setPublicationStatus);
    }

    @GetMapping("/deleted")
    public Flux<Node> getDeleted(Authentication authentication) {
        var grantedAuthority = authentication.getAuthorities().stream().findFirst().get();

        if (grantedAuthority.getAuthority().equals(RoleEnum.ADMIN.name())) {
            return nodeHandler.findAllByStatus(StatusEnum.DELETED.name())
                    .flatMap(nodeHandler::setPublicationStatus);
        }

        return nodeHandler.findDeleted(authentication.getPrincipal().toString())
                .flatMap(nodeHandler::setPublicationStatus);
    }

    @GetMapping(value = "/id/{id}")
    public Mono<ResponseEntity<Node>> findById(@PathVariable String id) {
        return nodeHandler.findById(UUID.fromString(id))
                .flatMap(nodeHandler::setPublicationStatus)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(new ResponseEntity<>(HttpStatus.NOT_FOUND));

    }

    @GetMapping(value = "/code/{code}/status/{status}")
    public Mono<ResponseEntity<Node>> findByCodeAndStatus(@PathVariable String code, @PathVariable String status) {
        return nodeHandler.findByCodeAndStatus(code, status)
                .flatMap(nodeHandler::setPublicationStatus)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @GetMapping(value = "/code/{code}")
    public Flux<Node> findByCode(@PathVariable String code) {
        return nodeHandler.findByCode(code)
                .flatMap(nodeHandler::setPublicationStatus);

    }

    @DeleteMapping(value = "/code/{code}/user/{userId}")
    public Mono<ResponseEntity<Boolean>> delete(@PathVariable String code, @PathVariable UUID userId) {
        return nodeHandler.delete(code, userId)
                .map(ResponseEntity::ok);
    }

    @DeleteMapping(value = "/code/{code}/deleteDefinitively")
    public Mono<ResponseEntity<Boolean>> deleteDefinitively(@PathVariable String code) {
        return nodeHandler.deleteDefinitively(code)
                .map(ResponseEntity::ok);
    }

    @PostMapping(value = "/code/{code}/user/{userId}/activate")
    public Mono<ResponseEntity<Boolean>> activate(@PathVariable String code, @PathVariable UUID userId) {
        return nodeHandler.activate(code, userId)
                .map(ResponseEntity::ok);
    }

    @PostMapping(value = "/id/{id}/user/{userId}/publish")
    public Mono<ResponseEntity<Node>> publish(@PathVariable UUID id, @PathVariable UUID userId) {
        return nodeHandler.publish(id, userId)
                .flatMap(nodeHandler::setPublicationStatus)
                .map(ResponseEntity::ok);
    }

    @GetMapping(value = "/parent/status/{status}")
    public Flux<Node> findParentsNodesByStatus(@PathVariable String status, Authentication authentication) {
        var grantedAuthority = authentication.getAuthorities().stream().findFirst().get();

        if (grantedAuthority.getAuthority().equals(RoleEnum.ADMIN.name())) {
            return nodeHandler.findParentsNodesByStatus(status)
                    .flatMap(nodeHandler::setPublicationStatus)
                    .sort((node1, node2) -> Boolean.compare(node2.isFavorite(), node1.isFavorite()));
        }

        return nodeHandler.findParentsNodesByStatus(status, authentication.getPrincipal().toString())
                .flatMap(nodeHandler::setPublicationStatus)
                .sort((node1, node2) -> Boolean.compare(node2.isFavorite(), node1.isFavorite()));

    }

    @GetMapping(value = "/parent/code/{code}/descendants")
    public Flux<Node> findAllDescendants(@PathVariable String code) {
        return nodeHandler.findAllChildren(code)
                .flatMap(nodeHandler::setPublicationStatus);
    }


    @GetMapping(value = "/parent/code/{code}")
    public Flux<Node> findByCodeParent(@PathVariable String code) {
        return nodeHandler.findByCodeParent(code)
                .flatMap(nodeHandler::setPublicationStatus);
    }

    @GetMapping(value = "/parent/code/{code}/status/{status}")
    public Flux<Node> findChildrenByCodeAndStatus(@PathVariable String code, @PathVariable String status) {
        return nodeHandler.findChildrenByCodeAndStatus(code, status)
                .flatMap(nodeHandler::setPublicationStatus);
    }

    @PostMapping(value = "/code/{code}/version/{version}/user/{userId}/revert")
    public Mono<Node> revert(@PathVariable String code, @PathVariable String version, @PathVariable UUID userId) {
        return nodeHandler.revert(code, version, userId)
                .flatMap(nodeHandler::setPublicationStatus);
    }

    @PostMapping("/")
    public Mono<Node> save(@RequestBody(required = true) Node node) {
        return nodeHandler.save(node)
                .flatMap(nodeHandler::setPublicationStatus);
    }

    @GetMapping(value = "/code/{code}/export")
    public Mono<ResponseEntity<byte[]>> exportAll(@PathVariable String code, @RequestParam(required = false, name = "environment") String environment) {
        return nodeHandler.exportAll(code, environment)
                .map(bytes -> {
                    return ResponseEntity.ok()
                            .contentLength(bytes.length)
                            .contentType(MediaType.APPLICATION_OCTET_STREAM)
                            .header("Content-Disposition", "attachment; filename=\"" + code + ".json\"")
                            .body(bytes);
                });
    }

    @PostMapping(value = "/import")
    public Mono<ResponseEntity<Node>> importNode(@RequestBody Node node) {
        return nodeHandler.importNode(node)
                .flatMap(nodeHandler::setPublicationStatus)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PostMapping(value = "/importAll")
    public Flux<Node> importNodes(@RequestBody List<Node> nodes,
                                  @RequestParam(name = "nodeParentCode", required = false) String nodeParentCode,
                                  @RequestParam(name = "fromFile", required = false, defaultValue = "true") Boolean fromFile) {
        return nodeHandler.importNodes(
                        nodes,
                        nodeParentCode,
                        fromFile)
                .flatMap(nodeHandler::setPublicationStatus);
    }

    @GetMapping(value = "/code/{code}/haveChilds")
    public Mono<Boolean> haveChilds(@PathVariable String code) {
        return nodeHandler.haveChilds(code);
    }

    @GetMapping(value = "/code/{code}/haveContents")
    public Mono<Boolean> haveContents(@PathVariable String code) {
        return nodeHandler.haveContents(code);
    }


    @GetMapping(value = "/code/{code}/deploy")
    public Flux<Node> deploy(@PathVariable String code,
                             @RequestParam(name = "environment", required = false) String environmentCode) {
        return this.exportAll(code, environmentCode)
                .map(responseEntity -> {
                    Gson gson = new GsonBuilder().create();

                    ObjectMapper objectMapper = new ObjectMapper();

                    List<String> array = null;
                    try {
                        array = objectMapper.readValue(responseEntity.getBody(), new TypeReference<List<String>>() {
                        });
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    List<Node> nodes = gson.fromJson(array.toString(), new TypeToken<List<Node>>() {
                    }.getType());

                    return nodes;
                })
                .map(nodes -> this.importNodes(nodes, environmentCode, false))
                .flatMapMany(nodes -> nodes)
                .flatMap(nodeHandler::setPublicationStatus)
                .flatMap(node -> this.nodeHandler.notify(node, NotificationEnum.IMPORT));
    }
}
