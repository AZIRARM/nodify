package com.itexpert.content.core.endpoints;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.itexpert.content.core.handlers.ContentNodeHandler;
import com.itexpert.content.core.handlers.NodeHandler;
import com.itexpert.content.core.handlers.RedisHandler;
import com.itexpert.content.core.handlers.UserHandler;
import com.itexpert.content.core.helpers.RenameContentNodeCodesHelper;
import com.itexpert.content.core.models.ContentNodePayload;
import com.itexpert.content.core.models.auth.RoleEnum;
import com.itexpert.content.lib.enums.NotificationEnum;
import com.itexpert.content.lib.enums.StatusEnum;
import com.itexpert.content.lib.models.ContentNode;
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

import java.time.Duration;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping(value = "/v0/content-node")
@AllArgsConstructor
public class ContentNodeEndPoint {

    private final ContentNodeHandler contentNodeHandler;
    private final NodeHandler nodeHandler;
    private final UserHandler userHandler;
    private final RedisHandler redisHandler;
    private final RenameContentNodeCodesHelper renameContentNodeCodesHelper;

    @GetMapping("/")
    public Flux<ContentNode> findAll() {
        return contentNodeHandler.findAll()
                .flatMap(contentNodeHandler::setPublicationStatus)
                .sort((content1, content2) -> Boolean.compare(content2.isFavorite(), content1.isFavorite()));
    }

    @GetMapping("/status/{status}")
    public Flux<ContentNode> findAllByStatus(@PathVariable String status) {
        return contentNodeHandler.findAllByStatus(status)
                .flatMap(contentNodeHandler::setPublicationStatus)
                .sort((content1, content2) -> Boolean.compare(content2.isFavorite(), content1.isFavorite()));
    }

    @GetMapping(value = "/code/{code}")
    public Flux<ContentNode> findAllByCode(@PathVariable String code) {
        return contentNodeHandler.findAllByCode(code)
                .flatMap(contentNodeHandler::setPublicationStatus)
                .sort((content1, content2) -> Boolean.compare(content2.isFavorite(), content1.isFavorite()));
    }

    @GetMapping(value = "/node/code/{code}/status/{status}")
    public Flux<ContentNode> findByNodeCodeAndStatus(@PathVariable String code, @PathVariable String status) {
        return contentNodeHandler.findByNodeCodeAndStatus(code, status)
                .flatMap(contentNodeHandler::setPublicationStatus)
                .sort((content1, content2) -> Boolean.compare(content2.isFavorite(), content1.isFavorite()));
    }

    @GetMapping(value = "/node/code/{code}")
    public Flux<ContentNode> findAllByNodeCode(@PathVariable String code) {
        return contentNodeHandler.findAllByNodeCode(code)
                .flatMap(contentNodeHandler::setPublicationStatus)
                .sort((content1, content2) -> Boolean.compare(content2.isFavorite(), content1.isFavorite()));
    }

    @DeleteMapping(value = "/code/{code}")
    public Mono<ResponseEntity<Boolean>> delete(@PathVariable String code, Authentication authentication) {
        String user = authentication.getPrincipal().toString();
        Duration ttl = Duration.ofMinutes(30);

        return redisHandler.canModify(code, user, ttl)
                .flatMap(canModify -> {
                    if (!canModify) {
                        return Mono.error(new IllegalStateException("ContentNode locked by another user"));
                    }

                    return contentNodeHandler.delete(code, user)
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
                        return Mono.error(new IllegalStateException("ContentNode locked by another user"));
                    }

                    return contentNodeHandler.deleteDefinitively(code)
                            .flatMap(result -> redisHandler.releaseLock(code, user).thenReturn(result))
                            .onErrorResume(ex -> redisHandler.releaseLock(code, user).then(Mono.error(ex)))
                            .map(ResponseEntity::ok);
                });
    }

    @DeleteMapping(value = "/code/{code}/version/{version}/deleteDefinitively")
    public Mono<ResponseEntity<Boolean>> deleteDefinitivelyVersion(@PathVariable String code, @PathVariable String version, Authentication authentication) {
        String user = authentication.getPrincipal().toString();
        Duration ttl = Duration.ofMinutes(30);

        return redisHandler.canModify(code, user, ttl)
                .flatMap(canModify -> {
                    if (!canModify) {
                        return Mono.error(new IllegalStateException("ContentNode locked by another user"));
                    }

                    return contentNodeHandler.deleteDefinitivelyVersion(code, version)
                            .flatMap(result -> redisHandler.releaseLock(code, user).thenReturn(result))
                            .onErrorResume(ex -> redisHandler.releaseLock(code, user).then(Mono.error(ex)))
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
                        return Mono.error(new IllegalStateException("ContentNode locked by another user"));
                    }

                    return contentNodeHandler.activate(code, user)
                            .flatMap(result -> redisHandler.releaseLock(code, user).thenReturn(result))
                            .onErrorResume(ex -> redisHandler.releaseLock(code, user).then(Mono.error(ex)))
                            .map(ResponseEntity::ok);
                });
    }


    @PostMapping(value = "/code/{code}/publish/{publish}")
    public Mono<ResponseEntity<ContentNode>> publish(@PathVariable String code,
                                                     @PathVariable Boolean publish,
                                                     Authentication authentication) {
        String user = authentication.getPrincipal().toString();
        Duration ttl = Duration.ofMinutes(30);

        return redisHandler.canModify(code, user, ttl)
                .flatMap(canModify -> {
                    if (!canModify) {
                        return Mono.error(new IllegalStateException("ContentNode locked by another user"));
                    }

                    return contentNodeHandler.publish(code, publish, user)
                            .flatMap(contentNodeHandler::setPublicationStatus)
                            .flatMap(saved -> redisHandler.releaseLock(code, user).thenReturn(saved))
                            .onErrorResume(ex -> redisHandler.releaseLock(code, user).then(Mono.error(ex)))
                            .map(ResponseEntity::ok);
                });
    }

    @PostMapping(value = "/code/{code}/version/{version}/deploy")
    public Mono<ResponseEntity<Boolean>> deployVersion(
            @PathVariable String code,
            @PathVariable String version,
            @RequestParam(name = "environment", required = false) String environmentCode,
            Authentication authentication) {
        String user = authentication.getPrincipal().toString();
        Duration ttl = Duration.ofMinutes(30);

        return redisHandler.canModify(code, user, ttl)
                .flatMap(canModify -> {
                    if (!canModify) {
                        return Mono.error(new IllegalStateException("Resource locked by another user"));
                    }

                    return contentNodeHandler.publishVersion(code, version, user)
                            .flatMap(saved -> redisHandler.releaseLock(code, user).thenReturn(saved))
                            .onErrorResume(ex -> redisHandler.releaseLock(code, user).then(Mono.error(ex)))
                            .map(ResponseEntity::ok);
                });

    }

    @PostMapping(value = "/code/{code}/version/{version}/revert")
    public Mono<ContentNode> revert(@PathVariable String code,
                                    @PathVariable String version,
                                    Authentication authentication) {
        String user = authentication.getPrincipal().toString();
        Duration ttl = Duration.ofMinutes(30);

        return redisHandler.canModify(code, user, ttl)
                .flatMap(canModify -> {
                    if (!canModify) {
                        return Mono.error(new IllegalStateException("ContentNode locked by another user"));
                    }

                    return contentNodeHandler.revert(code, version, user)
                            .flatMap(contentNodeHandler::setPublicationStatus)
                            .flatMap(saved -> redisHandler.releaseLock(code, user).thenReturn(saved))
                            .onErrorResume(ex -> redisHandler.releaseLock(code, user).then(Mono.error(ex)));
                });
    }


    @PostMapping(value = "/code/{code}/status/{status}/fill")
    public Mono<ContentNode> fillContent(@PathVariable String code,
                                         @PathVariable StatusEnum status,
                                         @RequestBody ContentNodePayload contentNode) {
        return contentNodeHandler.fillContent(code, status, contentNode);
    }


    @PostMapping("/")
    public Mono<ResponseEntity<ContentNode>> save(@RequestBody ContentNode contentNode,
                                                  Authentication authentication) {
        String user = authentication.getPrincipal().toString();
        Duration ttl = Duration.ofMinutes(30);

        // On utilise code comme clé de ressource (ou id si tu préfères)
        String resourceKey = ObjectUtils.isNotEmpty(contentNode.getCode())
                ? contentNode.getCode()
                : UUID.randomUUID().toString();

        return redisHandler.canModify(resourceKey, user, ttl)
                .flatMap(canModify -> {
                    if (!canModify) {
                        return Mono.error(new IllegalStateException("ContentNode locked by another user"));
                    }

                    contentNode.setModifiedBy(user);

                    return Mono.defer(() -> {
                                try {
                                    return contentNodeHandler.save(contentNode);
                                } catch (CloneNotSupportedException ex) {
                                    return Mono.error(new RuntimeException("Erreur lors du clone du contentNode", ex));
                                }
                            })
                            .flatMap(contentNodeHandler::setPublicationStatus)
                            .flatMap(saved -> redisHandler.releaseLock(resourceKey, user).thenReturn(saved))
                            .onErrorResume(ex -> redisHandler.releaseLock(resourceKey, user).then(Mono.error(ex)))
                            .map(ResponseEntity::ok);
                });
    }


    @GetMapping("/deleted")
    public Flux<ContentNode> getDeleted(Authentication authentication, @RequestParam(required = false, name = "parent") String parent) {
        var grantedAuthority = authentication.getAuthorities().stream().findFirst().get();

        if (grantedAuthority.getAuthority().equals(RoleEnum.ADMIN.name())) {
            return contentNodeHandler.findAllByStatus(StatusEnum.DELETED.name())
                    .flatMap(contentNodeHandler::setPublicationStatus)
                    .filter(contentNode -> {
                                return (
                                        (ObjectUtils.isNotEmpty(contentNode.getParentCode()) && contentNode.getParentCode().equals(parent))
                                                || (ObjectUtils.isEmpty(contentNode.getParentCode()) && (ObjectUtils.isEmpty(parent)))
                                );
                            }
                    );
        }

        return contentNodeHandler.findDeleted(authentication.getPrincipal().toString())
                .flatMap(contentNodeHandler::setPublicationStatus)
                .filter(contentNode -> {
                            return (
                                    (ObjectUtils.isNotEmpty(contentNode.getParentCode()) && contentNode.getParentCode().equals(parent))
                                            || (ObjectUtils.isEmpty(contentNode.getParentCode()) && (ObjectUtils.isEmpty(parent)))
                            );
                        }
                );
    }

    @GetMapping(value = "/code/{code}/status/{status}")
    public Mono<ResponseEntity<ContentNode>> findByCodeAndStatus(@PathVariable String code, @PathVariable String status) {
        return contentNodeHandler.findByCodeAndStatus(code, status)
                .flatMap(contentNodeHandler::setPublicationStatus)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PostMapping(value = "/import")
    public Mono<ResponseEntity<ContentNode>> importContentNode(@RequestBody ContentNode contentNode,
                                                               @RequestParam(name = "nodeParentCode", required = false) String nodeParentCode,
                                                               @RequestParam(name = "fromFile", required = false, defaultValue = "true") Boolean fromFile) {
        return this.nodeHandler.findByCodeAndStatus(nodeParentCode, StatusEnum.SNAPSHOT.name())
                .map(node -> ObjectUtils.isEmpty(node.getParentCodeOrigin()) ? node.getCode() : node.getParentCodeOrigin())
                .flatMap(codeParentOrigin -> this.nodeHandler.findByCodeAndStatus(codeParentOrigin, StatusEnum.SNAPSHOT.name()))
                .doOnNext(node -> {
                    log.debug(node.toString());
                })
                .flatMap(environment -> this.renameContentNodeCodesHelper.changeCodesAndReturnJson(contentNode, environment.getCode(), fromFile))
                .map(content -> {
                    content.setParentCode(nodeParentCode);
                    return content;
                })
                .flatMap(contentNodeHandler::importContentNode)
                .flatMap(contentNodeHandler::setPublicationStatus)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(new ResponseEntity<>(HttpStatus.NOT_FOUND));

    }

    @GetMapping(value = "/code/{code}/export")
    public Mono<ResponseEntity<byte[]>> export(@PathVariable String code,
                                               @RequestParam(name = "environment", required = false) String environment) {
        return this.contentNodeHandler.findByCodeAndStatus(code, StatusEnum.SNAPSHOT.name())
                .map(contentNode -> {
                    contentNode.setId(UUID.randomUUID());
                    contentNode.setParentCodeOrigin(null);
                    return contentNode;
                })
                .flatMap(model -> this.contentNodeHandler.notify(model, NotificationEnum.EXPORT))
                .map(contentNode -> {
                    Gson gson = new GsonBuilder().create();
                    return gson.toJson(contentNode);
                })
                .map(json -> {
                    byte[] bytes = json.getBytes();
                    return ResponseEntity.ok()
                            .contentLength(bytes.length)
                            .contentType(MediaType.APPLICATION_OCTET_STREAM)
                            .header("Content-Disposition", "attachment; filename=\"content-" + code + (ObjectUtils.isNotEmpty(environment) ? "-" + environment : "") + ".json\"")
                            .body(bytes);
                });
    }

    @GetMapping(value = "/code/{code}/deploy")
    public Mono<Boolean> deploy(@PathVariable String code,
                                @RequestParam(name = "environment", required = false) String environmentCode) {
        return this.contentNodeHandler.deployContent(code, environmentCode).hasElement();
    }

    @GetMapping(value = "/code/{code}/slug/{slug}/exists")
    public Mono<Boolean> slugExists(@PathVariable String code, @PathVariable String slug) {
        return contentNodeHandler.slugAlreadyExists(code, slug);
    }


    private String extratUser(UserPost user) {
        return ObjectUtils.isEmpty(user) ? "" :
                (user.getFirstname() + " " + user.getLastname());
    }
}
