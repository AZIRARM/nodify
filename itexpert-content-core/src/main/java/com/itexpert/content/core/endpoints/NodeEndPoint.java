package com.itexpert.content.core.endpoints;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.itexpert.content.core.handlers.NodeHandler;
import com.itexpert.content.core.handlers.UserHandler;
import com.itexpert.content.core.helpers.ProjectSecurity;
import com.itexpert.content.core.models.TreeNode;
import com.itexpert.content.core.models.auth.RoleEnum;
import com.itexpert.content.core.utils.auth.SecurityUtils;
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
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping(value = "/v0/nodes")
public class NodeEndPoint {

    private final NodeHandler nodeHandler;
    private final UserHandler userHandler;
    private final ProjectSecurity authorizationHelper;

    @GetMapping("/")
    public Flux<Node> findAll() {
        return SecurityUtils.hasAnyRole(RoleEnum.ADMIN.name(), RoleEnum.EDITOR.name(), RoleEnum.READER.name())
                .flatMapMany(canAccess -> {
                    if (!canAccess) {
                        return Flux.error(new RuntimeException("Accès refusé"));
                    }
                    return nodeHandler.findAll()
                            .flatMap(nodeHandler::setPublicationStatus)
                            .sort((node1, node2) -> Boolean.compare(node2.isFavorite(), node1.isFavorite()));
                });
    }

    @GetMapping("/origin")
    public Flux<Node> findParentOrigin() {
        return SecurityUtils.hasAnyRole(RoleEnum.ADMIN.name(), RoleEnum.EDITOR.name(), RoleEnum.READER.name())
                .flatMapMany(canAccess -> {
                    if (!canAccess) {
                        return Flux.error(new RuntimeException("Accès refusé"));
                    }
                    return nodeHandler.findParentOrigin()
                            .flatMap(nodeHandler::setPublicationStatus)
                            .sort((node1, node2) -> Boolean.compare(node2.isFavorite(), node1.isFavorite()));
                });
    }

    @GetMapping("/status/{status}")
    public Flux<Node> findAllByStatus(@PathVariable String status) {
        return SecurityUtils.hasAnyRole(RoleEnum.ADMIN.name(), RoleEnum.EDITOR.name(), RoleEnum.READER.name())
                .flatMapMany(canAccess -> {
                    if (!canAccess) {
                        return Flux.error(new RuntimeException("Accès refusé"));
                    }
                    return SecurityUtils.hasRole(RoleEnum.ADMIN.name())
                            .flatMapMany(isAdmin -> {
                                if (isAdmin) {
                                    return nodeHandler.findAllByStatus(status)
                                            .flatMap(nodeHandler::setPublicationStatus)
                                            .sort((node1, node2) -> Boolean.compare(node2.isFavorite(),
                                                    node1.isFavorite()));
                                }
                                return SecurityUtils.getUsername()
                                        .flatMapMany(username -> nodeHandler.findAllByStatusAndUser(status, username)
                                                .flatMap(nodeHandler::setPublicationStatus)
                                                .sort((node1, node2) -> Boolean.compare(node2.isFavorite(),
                                                        node1.isFavorite())));
                            });
                });
    }

    @GetMapping("/published")
    public Flux<Node> published() {
        return SecurityUtils.hasAnyRole(RoleEnum.ADMIN.name(), RoleEnum.EDITOR.name(), RoleEnum.READER.name())
                .flatMapMany(canAccess -> {
                    if (!canAccess) {
                        return Flux.error(new RuntimeException("Accès refusé"));
                    }
                    return nodeHandler.findAllByStatus(StatusEnum.PUBLISHED.name())
                            .flatMap(nodeHandler::setPublicationStatus);
                });
    }

    @GetMapping("/deleted")
    public Flux<Node> getDeleted(@RequestParam(required = false, name = "parent") String parent) {
        return SecurityUtils.hasAnyRole(RoleEnum.ADMIN.name(), RoleEnum.EDITOR.name())
                .flatMapMany(canAccess -> {
                    if (!canAccess) {
                        return Flux.error(new RuntimeException("Accès refusé"));
                    }
                    return SecurityUtils.hasRole(RoleEnum.ADMIN.name())
                            .flatMapMany(isAdmin -> {
                                if (isAdmin) {
                                    return nodeHandler.findAllByStatus(StatusEnum.DELETED.name())
                                            .flatMap(nodeHandler::setPublicationStatus)
                                            .filter(node -> (ObjectUtils.isNotEmpty(node.getParentCode())
                                                    && node.getParentCode().equals(parent))
                                                    || (ObjectUtils.isEmpty(node.getParentCode())
                                                            && (ObjectUtils.isEmpty(parent))));
                                }
                                return SecurityUtils.getUsername()
                                        .flatMapMany(username -> nodeHandler.findDeleted(username)
                                                .flatMap(nodeHandler::setPublicationStatus)
                                                .filter(node -> (ObjectUtils.isNotEmpty(node.getParentCode())
                                                        && node.getParentCode().equals(parent))
                                                        || (ObjectUtils.isEmpty(node.getParentCode())
                                                                && (ObjectUtils.isEmpty(parent)))));
                            });
                });
    }

    @GetMapping(value = "/code/{code}/status/{status}")
    public Mono<ResponseEntity<Node>> findByCodeAndStatus(@PathVariable String code, @PathVariable String status) {
        return SecurityUtils.hasAnyRole(RoleEnum.ADMIN.name(), RoleEnum.EDITOR.name(), RoleEnum.READER.name())
                .flatMap(canAccess -> {
                    if (!canAccess) {
                        return Mono.just(new ResponseEntity<>(HttpStatus.FORBIDDEN));
                    }
                    return nodeHandler.findByCodeAndStatus(code, status)
                            .flatMap(nodeHandler::setPublicationStatus)
                            .map(ResponseEntity::ok)
                            .defaultIfEmpty(new ResponseEntity<>(HttpStatus.NOT_FOUND));
                });
    }

    @GetMapping(value = "/code/{code}")
    public Flux<Node> findByCode(@PathVariable String code) {
        return SecurityUtils.hasAnyRole(RoleEnum.ADMIN.name(), RoleEnum.EDITOR.name(), RoleEnum.READER.name())
                .flatMapMany(canAccess -> {
                    if (!canAccess) {
                        return Flux.error(new RuntimeException("Accès refusé"));
                    }
                    return nodeHandler.findByCode(code)
                            .flatMap(nodeHandler::setPublicationStatus);
                });
    }

    @DeleteMapping(value = "/code/{code}")
    public Mono<ResponseEntity<Boolean>> delete(@PathVariable String code) {
        return SecurityUtils.hasAnyRole(RoleEnum.ADMIN.name(), RoleEnum.EDITOR.name())
                .flatMap(hasRole -> {
                    if (!hasRole) {
                        return Mono.just(new ResponseEntity<>(HttpStatus.FORBIDDEN));
                    }
                    return SecurityUtils.hasRole(RoleEnum.EDITOR.name())
                            .flatMap(isEditor -> {
                                if (isEditor && !authorizationHelper.hasProjectAccess(code)) {
                                    return Mono.just(new ResponseEntity<>(HttpStatus.FORBIDDEN));
                                }
                                return SecurityUtils.getUsername()
                                        .flatMap(user -> nodeHandler.delete(code, user))
                                        .map(ResponseEntity::ok);
                            });
                });
    }

    @DeleteMapping(value = "/code/{code}/deleteDefinitively")
    public Mono<ResponseEntity<Boolean>> deleteDefinitively(@PathVariable String code) {
        return SecurityUtils.hasAnyRole(RoleEnum.ADMIN.name(), RoleEnum.EDITOR.name())
                .flatMap(hasRole -> {
                    if (!hasRole) {
                        return Mono.just(new ResponseEntity<>(HttpStatus.FORBIDDEN));
                    }
                    return SecurityUtils.hasRole(RoleEnum.EDITOR.name())
                            .flatMap(isEditor -> {
                                if (isEditor && !authorizationHelper.hasProjectAccess(code)) {
                                    return Mono.just(new ResponseEntity<>(HttpStatus.FORBIDDEN));
                                }
                                return nodeHandler.deleteDefinitively(code)
                                        .map(ResponseEntity::ok);
                            });
                });
    }

    @DeleteMapping(value = "/code/{code}/version/{version}/deleteDefinitively")
    public Mono<ResponseEntity<Boolean>> deleteDefinitivelyVersion(@PathVariable String code,
            @PathVariable String version) {
        return SecurityUtils.hasAnyRole(RoleEnum.ADMIN.name(), RoleEnum.EDITOR.name())
                .flatMap(hasRole -> {
                    if (!hasRole) {
                        return Mono.just(new ResponseEntity<>(HttpStatus.FORBIDDEN));
                    }
                    return SecurityUtils.hasRole(RoleEnum.EDITOR.name())
                            .flatMap(isEditor -> {
                                if (isEditor && !authorizationHelper.hasProjectAccess(code)) {
                                    return Mono.just(new ResponseEntity<>(HttpStatus.FORBIDDEN));
                                }
                                return nodeHandler.deleteDefinitivelyVersion(code, version)
                                        .map(ResponseEntity::ok);
                            });
                });
    }

    @PostMapping(value = "/code/{code}/activate")
    public Mono<ResponseEntity<Boolean>> activate(@PathVariable String code) {
        return SecurityUtils.hasAnyRole(RoleEnum.ADMIN.name(), RoleEnum.EDITOR.name())
                .flatMap(hasRole -> {
                    if (!hasRole) {
                        return Mono.just(new ResponseEntity<>(HttpStatus.FORBIDDEN));
                    }
                    return SecurityUtils.hasRole(RoleEnum.EDITOR.name())
                            .flatMap(isEditor -> {
                                if (isEditor && !authorizationHelper.hasProjectAccess(code)) {
                                    return Mono.just(new ResponseEntity<>(HttpStatus.FORBIDDEN));
                                }
                                return SecurityUtils.getUsername()
                                        .flatMap(user -> nodeHandler.activate(code, user))
                                        .map(ResponseEntity::ok);
                            });
                });
    }

    @PostMapping(value = "/code/{code}/publish")
    public Mono<ResponseEntity<Node>> publish(@PathVariable String code) {
        return SecurityUtils.hasAnyRole(RoleEnum.ADMIN.name(), RoleEnum.EDITOR.name())
                .flatMap(hasRole -> {
                    if (!hasRole) {
                        return Mono.just(new ResponseEntity<>(HttpStatus.FORBIDDEN));
                    }
                    return SecurityUtils.hasRole(RoleEnum.EDITOR.name())
                            .flatMap(isEditor -> {
                                if (isEditor && !authorizationHelper.hasProjectAccess(code)) {
                                    return Mono.just(new ResponseEntity<>(HttpStatus.FORBIDDEN));
                                }
                                return SecurityUtils.getUsername()
                                        .flatMap(user -> nodeHandler.publish(code, user)
                                                .flatMap(nodeHandler::setPublicationStatus))
                                        .map(ResponseEntity::ok);
                            });
                });
    }

    @GetMapping(value = "/parent/status/{status}")
    public Flux<Node> findParentsNodesByStatus(@PathVariable String status) {
        return SecurityUtils.hasAnyRole(RoleEnum.ADMIN.name(), RoleEnum.EDITOR.name(), RoleEnum.READER.name())
                .flatMapMany(canAccess -> {
                    if (!canAccess) {
                        return Flux.error(new RuntimeException("Accès refusé"));
                    }
                    return SecurityUtils.hasRole(RoleEnum.ADMIN.name())
                            .flatMapMany(isAdmin -> {
                                if (isAdmin) {
                                    return nodeHandler.findParentsNodesByStatus(status)
                                            .flatMap(nodeHandler::setPublicationStatus)
                                            .sort((node1, node2) -> Boolean.compare(node2.isFavorite(),
                                                    node1.isFavorite()));
                                }
                                return SecurityUtils.getUsername()
                                        .flatMapMany(username -> nodeHandler.findParentsNodesByStatus(status, username)
                                                .flatMap(nodeHandler::setPublicationStatus)
                                                .map(node -> {
                                                    node.setParentCode(null);
                                                    return node;
                                                })
                                                .sort((node1, node2) -> Boolean.compare(node2.isFavorite(),
                                                        node1.isFavorite())));
                            });
                });
    }

    @GetMapping(value = "/parent/code/{code}/descendants")
    public Flux<Node> findAllDescendants(@PathVariable String code) {
        return SecurityUtils.hasAnyRole(RoleEnum.ADMIN.name(), RoleEnum.EDITOR.name(), RoleEnum.READER.name())
                .flatMapMany(canAccess -> {
                    if (!canAccess) {
                        return Flux.error(new RuntimeException("Accès refusé"));
                    }
                    return nodeHandler.findAllChildren(code)
                            .flatMap(nodeHandler::setPublicationStatus);
                });
    }

    @GetMapping(value = "/parent/code/{code}")
    public Flux<Node> findByCodeParent(@PathVariable String code) {
        return SecurityUtils.hasAnyRole(RoleEnum.ADMIN.name(), RoleEnum.EDITOR.name(), RoleEnum.READER.name())
                .flatMapMany(canAccess -> {
                    if (!canAccess) {
                        return Flux.error(new RuntimeException("Accès refusé"));
                    }
                    return nodeHandler.findByCodeParent(code)
                            .flatMap(nodeHandler::setPublicationStatus);
                });
    }

    @GetMapping(value = "/parent/code/{code}/status/{status}")
    public Flux<Node> findChildrenByCodeAndStatus(@PathVariable String code, @PathVariable String status) {
        return SecurityUtils.hasAnyRole(RoleEnum.ADMIN.name(), RoleEnum.EDITOR.name(), RoleEnum.READER.name())
                .flatMapMany(canAccess -> {
                    if (!canAccess) {
                        return Flux.error(new RuntimeException("Accès refusé"));
                    }
                    return nodeHandler.findChildrenByCodeAndStatus(code, status)
                            .flatMap(nodeHandler::setPublicationStatus);
                });
    }

    @PostMapping(value = "/code/{code}/version/{version}/revert")
    public Mono<ResponseEntity<Node>> revert(@PathVariable String code, @PathVariable String version) {
        return SecurityUtils.hasAnyRole(RoleEnum.ADMIN.name(), RoleEnum.EDITOR.name())
                .flatMap(hasRole -> {
                    if (!hasRole) {
                        return Mono.just(new ResponseEntity<>(HttpStatus.FORBIDDEN));
                    }
                    return SecurityUtils.hasRole(RoleEnum.EDITOR.name())
                            .flatMap(isEditor -> {
                                if (isEditor && !authorizationHelper.hasProjectAccess(code)) {
                                    return Mono.just(new ResponseEntity<>(HttpStatus.FORBIDDEN));
                                }
                                return SecurityUtils.getUsername()
                                        .flatMap(user -> nodeHandler.revert(code, version, user)
                                                .flatMap(nodeHandler::setPublicationStatus))
                                        .map(ResponseEntity::ok);
                            });
                });
    }

    @PostMapping("/")
    public Mono<ResponseEntity<Node>> save(@RequestBody Node node) {
        return SecurityUtils.hasAnyRole(RoleEnum.ADMIN.name(), RoleEnum.EDITOR.name())
                .flatMap(hasRole -> {
                    if (!hasRole) {
                        return Mono.just(new ResponseEntity<>(HttpStatus.FORBIDDEN));
                    }
                    return SecurityUtils.hasRole(RoleEnum.EDITOR.name())
                            .flatMap(isEditor -> {
                                if (isEditor && !authorizationHelper.hasProjectAccess(node.getParentCodeOrigin())) {
                                    return Mono.just(new ResponseEntity<>(HttpStatus.FORBIDDEN));
                                }
                                return SecurityUtils.getUsername()
                                        .flatMap(user -> {
                                            node.setModifiedBy(user);
                                            return nodeHandler.save(node)
                                                    .flatMap(nodeHandler::setPublicationStatus)
                                                    .map(ResponseEntity::ok);
                                        });
                            });
                });
    }

    @GetMapping(value = "/code/{code}/export")
    public Mono<ResponseEntity<byte[]>> exportAll(
            @PathVariable String code,
            @RequestParam(required = false, name = "environment") String environment) {
        return SecurityUtils.hasAnyRole(RoleEnum.ADMIN.name(), RoleEnum.EDITOR.name())
                .flatMap(hasRole -> {
                    if (!hasRole) {
                        return Mono.just(new ResponseEntity<>(HttpStatus.FORBIDDEN));
                    }
                    return SecurityUtils.hasRole(RoleEnum.EDITOR.name())
                            .flatMap(isEditor -> {
                                if (isEditor && !authorizationHelper.hasProjectAccess(code)) {
                                    return Mono.just(new ResponseEntity<>(HttpStatus.FORBIDDEN));
                                }
                                return nodeHandler.exportAll(code, environment)
                                        .map(jsonBytes -> ResponseEntity.ok()
                                                .contentLength(jsonBytes.length)
                                                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                                                .header("Content-Disposition",
                                                        "attachment; filename=\"" + code + ".json\"")
                                                .body(jsonBytes));
                            });
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
        return nodeHandler.importNodes(nodes, nodeParentCode, fromFile)
                .flatMap(nodeHandler::setPublicationStatus);
    }

    @GetMapping(value = "/code/{code}/haveChilds")
    public Mono<ResponseEntity<Boolean>> haveChilds(@PathVariable String code) {
        return SecurityUtils.hasAnyRole(RoleEnum.ADMIN.name(), RoleEnum.EDITOR.name(), RoleEnum.READER.name())
                .flatMap(canAccess -> {
                    if (!canAccess) {
                        return Mono.just(new ResponseEntity<>(HttpStatus.FORBIDDEN));
                    }
                    return nodeHandler.haveChilds(code)
                            .map(ResponseEntity::ok);
                });
    }

    @GetMapping(value = "/code/{code}/haveContents")
    public Mono<ResponseEntity<Boolean>> haveContents(@PathVariable String code) {
        return SecurityUtils.hasAnyRole(RoleEnum.ADMIN.name(), RoleEnum.EDITOR.name(), RoleEnum.READER.name())
                .flatMap(canAccess -> {
                    if (!canAccess) {
                        return Mono.just(new ResponseEntity<>(HttpStatus.FORBIDDEN));
                    }
                    return nodeHandler.haveContents(code)
                            .map(ResponseEntity::ok);
                });
    }

    @GetMapping(value = "/code/{code}/deploy")
    public Flux<Node> deploy(@PathVariable String code,
            @RequestParam(name = "environment", required = false) String environmentCode) {
        return SecurityUtils.hasAnyRole(RoleEnum.ADMIN.name(), RoleEnum.EDITOR.name())
                .flatMapMany(canAccess -> {
                    if (!canAccess) {
                        return Flux.error(new RuntimeException("Accès refusé"));
                    }
                    return SecurityUtils.getUsername()
                            .flatMapMany(username -> this.exportAll(code, environmentCode)
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
                                        nodes.forEach(node -> {
                                            log.debug("[EXPORT] Node to import: code={}, status={}", node.getCode(),
                                                    node.getStatus());
                                            node.setModifiedBy(username);
                                            Optional.ofNullable(node.getContents()).orElse(List.of())
                                                    .forEach(contentNode -> {
                                                        log.debug("[EXPORT]   ContentNode: code={}, status={}",
                                                                contentNode.getCode(), contentNode.getStatus());
                                                        contentNode.setModifiedBy(username);
                                                    });
                                        });
                                        return this.importNodes(nodes, environmentCode, false);
                                    })
                                    .flatMapMany(nodes -> nodes)
                                    .flatMap(this::removeStatusSnaphotFromContents)
                                    .flatMap(nodeHandler::setPublicationStatus)
                                    .flatMap(node -> this.nodeHandler.notify(node, NotificationEnum.IMPORT))
                                    .filter(node -> node.getParentCode().equals(environmentCode))
                                    .flatMap(node -> this.nodeHandler
                                            .findByCodeAndStatus(node.getCode(), StatusEnum.SNAPSHOT.name())
                                            .flatMap(nodeToPublish -> this.nodeHandler.publish(nodeToPublish.getCode(),
                                                    username))));
                });
    }

    @PostMapping(value = "/code/{code}/version/{version}/deploy")
    public Mono<ResponseEntity<Boolean>> deployVersion(
            @PathVariable String code,
            @PathVariable String version,
            @RequestParam(name = "environment", required = false) String environmentCode) {
        return SecurityUtils.hasAnyRole(RoleEnum.ADMIN.name(), RoleEnum.EDITOR.name())
                .flatMap(canAccess -> {
                    if (!canAccess) {
                        return Mono.just(new ResponseEntity<>(HttpStatus.FORBIDDEN));
                    }
                    return SecurityUtils.hasRole(RoleEnum.EDITOR.name())
                            .flatMap(isEditor -> {
                                if (isEditor && !authorizationHelper.hasProjectAccess(code)) {
                                    return Mono.just(new ResponseEntity<>(HttpStatus.FORBIDDEN));
                                }
                                return SecurityUtils.getUsername()
                                        .flatMap(user -> nodeHandler.publishVersion(code, version, user))
                                        .map(ResponseEntity::ok);
                            });
                });
    }

    @GetMapping(value = "/code/{code}/slug/{slug}/exists")
    public Mono<ResponseEntity<Boolean>> slugExists(@PathVariable String code, @PathVariable String slug) {
        return SecurityUtils.hasAnyRole(RoleEnum.ADMIN.name(), RoleEnum.EDITOR.name())
                .flatMap(canAccess -> {
                    if (!canAccess) {
                        return Mono.just(new ResponseEntity<>(HttpStatus.FORBIDDEN));
                    }
                    return nodeHandler.slugAlreadyExists(code, slug)
                            .map(ResponseEntity::ok);
                });
    }

    @GetMapping(value = "/code/{code}/tree-view")
    public Mono<ResponseEntity<TreeNode>> generateTreeView(@PathVariable String code) {
        return SecurityUtils.hasAnyRole(RoleEnum.ADMIN.name(), RoleEnum.EDITOR.name(), RoleEnum.READER.name())
                .flatMap(canAccess -> {
                    if (!canAccess) {
                        return Mono.just(new ResponseEntity<>(HttpStatus.FORBIDDEN));
                    }
                    return SecurityUtils.hasRole(RoleEnum.ADMIN.name())
                            .flatMap(isAdmin -> {
                                if (isAdmin) {
                                    return nodeHandler.generateTreeView(code, List.of())
                                            .map(ResponseEntity::ok);
                                }
                                return SecurityUtils.getUsername()
                                        .flatMap(username -> this.userHandler.findByEmail(username)
                                                .map(UserPost::getProjects)
                                                .flatMap(userProjects -> nodeHandler.generateTreeView(code,
                                                        userProjects))
                                                .map(ResponseEntity::ok));
                            });
                });
    }

    private String extratUser(UserPost user) {
        return ObjectUtils.isEmpty(user) ? "" : (user.getFirstname() + " " + user.getLastname());
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
        if (input == null)
            return null;

        int qIndex = input.indexOf('?');
        if (qIndex < 0) {
            return input;
        }

        String base = input.substring(0, qIndex);
        String query = input.substring(qIndex + 1);

        String[] params = query.split("&");
        List<String> kept = new ArrayList<>();

        for (String p : params) {
            if (!"status=SNAPSHOT".equals(p)) {
                kept.add(p);
            }
        }

        if (kept.isEmpty()) {
            return base;
        }

        return base + "?" + String.join("&", kept);
    }

    @PostMapping(value = "/propagateMaxHistoryToKeep/{nodeCodePatent}")
    public Mono<ResponseEntity<Boolean>> propagateMaxHistoryToKeep(@PathVariable String nodeCodePatent) {
        return nodeHandler.propagateMaxHistoryToKeep(nodeCodePatent)
                .map(ResponseEntity::ok);
    }

    @GetMapping(value = "/actifs")
    public Flux<Node> findAllActifs() {
        return SecurityUtils.hasRole(RoleEnum.ADMIN.name())
                .flatMapMany(isAdmin -> {
                    if (isAdmin) {
                        return this.nodeHandler.findAllActifs();
                    }
                    return Flux.empty();
                });
    }
}