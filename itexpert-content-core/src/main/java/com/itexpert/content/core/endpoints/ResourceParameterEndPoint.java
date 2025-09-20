package com.itexpert.content.core.endpoints;

import com.itexpert.content.core.handlers.ResourceParameterHandler;
import com.itexpert.content.core.handlers.UserHandler;
import com.itexpert.content.lib.enums.ResourceActionEnum;
import com.itexpert.content.lib.enums.ResourceTypeEnum;
import com.itexpert.content.lib.models.ResourceParameter;
import com.itexpert.content.lib.models.UserPost;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;


@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping(value = "/v0/resource-parameters")
public class ResourceParameterEndPoint {

    private final ResourceParameterHandler resourceParameterHandler;
    private final UserHandler userHandler;

    @PostMapping("/cleanup-archived-resources")
    public Mono<Boolean> cleanupArchivedResources() {
        return resourceParameterHandler.cleanupArchivedChildren();
    }

    @PostMapping("/")
    public Mono<ResourceParameter> save(@RequestBody ResourceParameter resourceParameter, Authentication authentication) {
        return this.extractUser(authentication.getPrincipal().toString())
                .filter(roles -> roles.contains("ADMIN"))
                .flatMap(roles -> resourceParameterHandler.save(resourceParameter)); // flatMap au lieu de then
    }

    @GetMapping("/")
    public Flux<ResourceParameter> findAll(Authentication authentication) {
        return this.extractUser(authentication.getPrincipal().toString())
                .filter(roles -> roles.contains("ADMIN"))
                .flatMapMany(roles -> resourceParameterHandler.findAll());
    }

    @DeleteMapping("/{id}")
    public Mono<Boolean> delete(@PathVariable UUID id, Authentication authentication) {
        return this.extractUser(authentication.getPrincipal().toString())
                .filter(roles -> roles.contains("ADMIN"))
                .flatMap(roles -> resourceParameterHandler.deleteById(id)); // flatMap au lieu de then
    }


    @GetMapping("/type/{type}/action/{action}")
    public Flux<ResourceParameter> findByTypeAndAction(
            @PathVariable ResourceTypeEnum type,
            @PathVariable ResourceActionEnum action,
            Authentication authentication) {

        return this.extractUser(authentication.getPrincipal().toString())
                .filter(roles -> roles.contains("ADMIN"))
                .flatMapMany(roles -> resourceParameterHandler.findByTypeAndAction(type, action));
    }


    private Mono<List<String>> extractUser(String email) {
        if (ObjectUtils.isEmpty(email)) {
            return Mono.just(List.of());
        }

        return this.userHandler.findByEmail(email)
                .map(UserPost::getRoles)
                .defaultIfEmpty(List.of());
    }
}
