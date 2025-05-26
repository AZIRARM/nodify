package com.itexpert.content.core.endpoints;

import com.itexpert.content.core.handlers.UserRoleHandler;
import com.itexpert.content.lib.models.UserRole;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping(value = "/v0/users-roles")
public class UserRoleEndPoint {

    private final UserRoleHandler roleRoleHandler;

    public UserRoleEndPoint(UserRoleHandler roleRoleHandler) {
        this.roleRoleHandler = roleRoleHandler;
    }

    @GetMapping("/")
    public Flux<UserRole> findAll() {
        return roleRoleHandler.findAll();
    }

    @GetMapping(value = "/id/{id}")
    public Mono<ResponseEntity<UserRole>> findById(@PathVariable String id) {
        return roleRoleHandler.findById(UUID.fromString(id))
                .map(ResponseEntity::ok)
                .defaultIfEmpty(new ResponseEntity<>(HttpStatus.NOT_FOUND));

    }

   // @RolesAllowed("ADMIN")
    @PostMapping("/")
    public Mono<ResponseEntity<UserRole>> save(@RequestBody(required = true) UserRole role) {
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
