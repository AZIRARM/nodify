package com.itexpert.content.core.endpoints;

import com.itexpert.content.core.handlers.AccessRoleHandler;
import com.itexpert.content.core.models.AccessRole;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping(value = "/v0/access-roles")
public class AccessRoleEndPoint {

    private final AccessRoleHandler roleRoleHandler;

    public AccessRoleEndPoint(AccessRoleHandler roleRoleHandler) {
        this.roleRoleHandler = roleRoleHandler;
    }

    @GetMapping
    public Flux<AccessRole> findAll() {
        return roleRoleHandler.findAll();
    }

    @GetMapping(value = "/id/{id}")
    public Mono<ResponseEntity<AccessRole>> findById(@PathVariable String id) {
        return roleRoleHandler.findById(UUID.fromString(id))
                .map(ResponseEntity::ok)
                .defaultIfEmpty(new ResponseEntity<>(HttpStatus.NOT_FOUND));

    }

    // @RolesAllowed("ADMIN")
    @PostMapping
    public Mono<ResponseEntity<AccessRole>> save(@RequestBody(required = true) AccessRole role) {
        return roleRoleHandler.save(role)
                .map(ResponseEntity::ok);
    }

    //@RolesAllowed("ADMIN")
    @DeleteMapping(value = "/id/{id}")
    public Mono<ResponseEntity<Boolean>> delete(@PathVariable String id) {
        return roleRoleHandler.delete(UUID.fromString(id))
                .map(ResponseEntity::ok);
    }

}
