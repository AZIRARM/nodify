package com.itexpert.content.core.endpoints;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.itexpert.content.core.handlers.NodeHandler;
import com.itexpert.content.core.handlers.RedisHandler;
import com.itexpert.content.core.handlers.UserHandler;
import com.itexpert.content.core.models.TreeNode;
import com.itexpert.content.core.models.auth.RoleEnum;
import com.itexpert.content.lib.enums.ContentTypeEnum;
import com.itexpert.content.lib.enums.NotificationEnum;
import com.itexpert.content.lib.enums.StatusEnum;
import com.itexpert.content.lib.models.ContentNode;
import com.itexpert.content.lib.models.Node;
import com.itexpert.content.lib.models.UserPost;
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
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping(value = "/v0/nodes")
public class NodeEndPoint {

    private final NodeHandler nodeHandler;

    private final UserHandler userHandler;
    private final RedisHandler redisHandler;

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
    public Flux<Node> getDeleted(Authentication authentication, @RequestParam(required = false, name = "parent") String parent) {
        var grantedAuthority = authentication.getAuthorities().stream().findFirst().get();

        if (grantedAuthority.getAuthority().equals(RoleEnum.ADMIN.name())) {
            return nodeHandler.findAllByStatus(StatusEnum.DELETED.name())
                    .flatMap(nodeHandler::setPublicationStatus)
                    .filter(node -> {
                                return (
                                        (ObjectUtils.isNotEmpty(node.getParentCode()) && node.getParentCode().equals(parent))
                                                || (ObjectUtils.isEmpty(node.getParentCode()) && (ObjectUtils.isEmpty(parent)))
                                );
                            }
                    );
        }

        return nodeHandler.findDeleted(authentication.getPrincipal().toString())
                .flatMap(nodeHandler::setPublicationStatus).filter(node -> {
                            return (
                                    (ObjectUtils.isNotEmpty(node.getParentCode()) && node.getParentCode().equals(parent))
                                            || (ObjectUtils.isEmpty(node.getParentCode()) && (ObjectUtils.isEmpty(parent)))
                            );
                        }
                );
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

    @DeleteMapping(value = "/code/{code}")
    public Mono<ResponseEntity<Boolean>> delete(@PathVariable String code, Authentication authentication) {
        String user = authentication.getPrincipal().toString();
        Duration ttl = Duration.ofMinutes(30);

        return redisHandler.canModify(code, user, ttl)
                .flatMap(canModify -> {
                    if (!canModify) {
                        return Mono.error(new IllegalStateException("Resource locked by another user"));
                    }

                    return nodeHandler.delete(code, user)
                            .flatMap(result -> redisHandler.releaseLock(code, user).thenReturn(result))
                            .onErrorResume(ex -> redisHandler.releaseLock(code, user).then(Mono.error(ex)))
                            .map(ResponseEntity::ok);
                });
    }


    @DeleteMapping(value = "/code/{code}/deleteDefinitively")
    public Mono<ResponseEntity<Boolean>> deleteDefinitively(@PathVariable String code, Authentication authentication) {
        String user = authentication.getPrincipal().toString();
        Duration ttl = Duration.ofMinutes(30);

        return redisHandler.canModify(code, user, ttl)
                .flatMap(canModify -> {
                    if (!canModify) {
                        return Mono.error(new IllegalStateException("Resource locked by another user"));
                    }

                    return nodeHandler.deleteDefinitively(code)
                            .flatMap(result -> redisHandler.releaseLock(code, user).thenReturn(result))
                            .onErrorResume(ex -> redisHandler.releaseLock(code, user).then(Mono.error(ex)))
                            .map(ResponseEntity::ok);
                });
    }

    @DeleteMapping(value = "/{id}")
    public Mono<ResponseEntity<Boolean>> deleteById(@PathVariable UUID id, Authentication authentication) {
        String user = authentication.getPrincipal().toString();
        Duration ttl = Duration.ofMinutes(30);

        return redisHandler.canModify(id.toString(), user, ttl)
                .flatMap(canModify -> {
                    if (!canModify) {
                        return Mono.error(new IllegalStateException("Resource locked by another user"));
                    }

                    return nodeHandler.deleteById(id)
                            .flatMap(result -> redisHandler.releaseLock(id.toString(), user).thenReturn(result))
                            .onErrorResume(ex -> redisHandler.releaseLock(id.toString(), user).then(Mono.error(ex)))
                            .map(ResponseEntity::ok);
                });
    }


    @PostMapping(value = "/code/{code}/activate")
    public Mono<ResponseEntity<Boolean>> activate(@PathVariable String code, Authentication authentication) {
        String user = authentication.getPrincipal().toString();
        Duration ttl = Duration.ofMinutes(30);

        return redisHandler.canModify(code, user, ttl)
                .flatMap(canModify -> {
                    if (!canModify) {
                        return Mono.error(new IllegalStateException("Resource locked by another user"));
                    }

                    return nodeHandler.activate(code, user)
                            .flatMap(result -> redisHandler.releaseLock(code, user).thenReturn(result))
                            .onErrorResume(ex -> redisHandler.releaseLock(code, user).then(Mono.error(ex)))
                            .map(ResponseEntity::ok);
                });
    }


    @PostMapping(value = "/id/{id}/publish")
    public Mono<ResponseEntity<Node>> publish(@PathVariable UUID id, Authentication authentication) {
        String user = authentication.getPrincipal().toString();
        Duration ttl = Duration.ofMinutes(30);

        return redisHandler.canModify(id.toString(), user, ttl)
                .flatMap(canModify -> {
                    if (!canModify) {
                        return Mono.error(new IllegalStateException("Resource locked by another user"));
                    }

                    return nodeHandler.publish(id, user)
                            .flatMap(nodeHandler::setPublicationStatus)
                            .flatMap(saved -> redisHandler.releaseLock(id.toString(), user).thenReturn(saved))
                            .onErrorResume(ex -> redisHandler.releaseLock(id.toString(), user).then(Mono.error(ex)))
                            .map(ResponseEntity::ok);
                });
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

    @PostMapping(value = "/code/{code}/version/{version}/revert")
    public Mono<Node> revert(@PathVariable String code, @PathVariable String version, Authentication authentication) {
        String user = authentication.getPrincipal().toString();
        Duration ttl = Duration.ofMinutes(30);

        return redisHandler.canModify(code, user, ttl)
                .flatMap(canModify -> {
                    if (!canModify) {
                        return Mono.error(new IllegalStateException("Resource locked by another user"));
                    }

                    return nodeHandler.revert(code, version, user)
                            .flatMap(nodeHandler::setPublicationStatus)
                            .flatMap(saved -> redisHandler.releaseLock(code, user).thenReturn(saved))
                            .onErrorResume(ex -> redisHandler.releaseLock(code, user).then(Mono.error(ex)));
                });
    }


    @PostMapping("/")
    public Mono<Node> save(@RequestBody Node node, Authentication authentication) {
        String user = authentication.getPrincipal().toString();
        Duration ttl = Duration.ofMinutes(30);

        return redisHandler.canModify(node.getCode(), user, ttl)
                .flatMap(canModify -> {
                    if (!canModify) {
                        return Mono.error(new IllegalStateException("Resource locked by another user"));
                    }

                    node.setModifiedBy(user);

                    return nodeHandler.save(node)
                            .flatMap(nodeHandler::setPublicationStatus)
                            .flatMap(savedNode ->
                                    redisHandler.releaseLock(node.getCode(), user)
                                            .thenReturn(savedNode)
                            )
                            .onErrorResume(ex ->
                                    redisHandler.releaseLock(node.getCode(), user)
                                            .then(Mono.error(ex))
                            );
                });
    }


    @GetMapping(value = "/code/{code}/export")
    public Mono<ResponseEntity<byte[]>> exportAll(
            @PathVariable String code,
            @RequestParam(required = false, name = "environment") String environment) {

        return nodeHandler.exportAll(code, environment)
                .map(jsonBytes -> {
                    return ResponseEntity.ok()
                            .contentLength(jsonBytes.length)
                            .contentType(MediaType.APPLICATION_OCTET_STREAM) // pour téléchargement
                            .header("Content-Disposition", "attachment; filename=\"" + code + ".json\"")
                            .body(jsonBytes);
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
                             @RequestParam(name = "environment", required = false) String environmentCode,
                             Authentication authentication) {
        return this.exportAll(code, environmentCode)
                .map(responseEntity -> {
                    Gson gson = new GsonBuilder().create();

                    String json = new String(responseEntity.getBody(), StandardCharsets.UTF_8);

                    List<Node> nodes = gson.fromJson(json, new TypeToken<List<Node>>() {
                    }.getType());

                    return nodes;
                })
                .map(nodes -> {
                    log.debug("[EXPORT] About to importNodes, environmentCode={}, nodes count={}",
                            environmentCode, nodes.size());

                    // Log chaque Node avec code et status
                    nodes.forEach(node -> {
                        log.debug("[EXPORT] Node to import: code={}, status={}",
                                node.getCode(), node.getStatus());

                        // Log chaque ContentNode à l'intérieur de Node
                        Optional.ofNullable(node.getContents()).orElse(List.of())
                                .forEach(contentNode -> log.debug(
                                        "[EXPORT]   ContentNode: code={}, status={}",
                                        contentNode.getCode(), contentNode.getStatus()));
                    });

                    return this.importNodes(nodes, environmentCode, false);
                })
                .flatMapMany(nodes -> nodes)
                .flatMap(this::removeStatusSnaphotFromContents)
                .flatMap(nodeHandler::setPublicationStatus)
                .flatMap(node -> this.nodeHandler.notify(node, NotificationEnum.IMPORT))
                .filter(node -> node.getParentCode().equals(environmentCode))
                .flatMap(node -> this.nodeHandler.findByCodeAndStatus(node.getCode(), StatusEnum.SNAPSHOT.name())
                        .flatMap(nodeToPublish -> this.nodeHandler.publish(nodeToPublish.getId(), authentication.getPrincipal().toString()))
                )
                ;
    }

    @GetMapping(value = "/code/{code}/slug/{slug}/exists")
    public Mono<Boolean> slugExists(@PathVariable String code, @PathVariable String slug) {
        return nodeHandler.slugAlreadyExists(code, slug);
    }

    @GetMapping(value = "/code/{code}/tree-view")
    public Mono<TreeNode> generateTreeView(@PathVariable String code, Authentication authentication) {
        var grantedAuthority = authentication.getAuthorities().stream().findFirst().get();

        if (grantedAuthority.getAuthority().equals(RoleEnum.ADMIN.name())) {

            return nodeHandler.generateTreeView(code, List.of());
        }
        return this.userHandler.findByEmail(authentication.getPrincipal().toString())
                .map(UserPost::getProjects)
                .flatMap(userProjects -> nodeHandler.generateTreeView(code, userProjects));
    }


    private String extratUser(UserPost user) {
        return ObjectUtils.isEmpty(user) ? "" :
                (user.getFirstname() + " " + user.getLastname());
    }

    private Mono<Node> removeStatusSnaphotFromContents(Node node) {
        if (ObjectUtils.isEmpty(node) || ObjectUtils.isEmpty(node.getContents())) {
            return Mono.just(node);
        }

        List<ContentNode> cleanedContents = node.getContents().stream()
                .map(contentNode -> {
                    if (ObjectUtils.isNotEmpty(contentNode.getContent())
                            && !contentNode.getType().equals(ContentTypeEnum.FILE)
                            && !contentNode.getType().equals(ContentTypeEnum.PICTURE)) {
                        String cleaned = cleanStatusSnapshot(contentNode.getContent());
                        contentNode.setContent(cleaned);
                    }
                    return contentNode;
                })
                .toList();

        node.setContents(cleanedContents);
        return Mono.just(node);
    }

    private String cleanStatusSnapshot(String input) {
        if (input == null) return null;

        int qIndex = input.indexOf('?');
        if (qIndex < 0) {
            return input; // pas de paramètres
        }

        String base = input.substring(0, qIndex);
        String query = input.substring(qIndex + 1);

        // découpe en paramètres
        String[] params = query.split("&");
        List<String> kept = new ArrayList<>();

        for (String p : params) {
            if (!"status=SNAPSHOT".equals(p)) { // supprime exactement status=SNAPSHOT
                kept.add(p);
            }
        }

        if (kept.isEmpty()) {
            return base; // aucun param restant -> pas de "?"
        }

        return base + "?" + String.join("&", kept);
    }
}
