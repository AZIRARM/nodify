package com.itexpert.content.core.endpoints;

import com.itexpert.content.core.handlers.UserHandler;
import com.itexpert.content.core.models.auth.RoleEnum;
import com.itexpert.content.lib.models.UserPassword;
import com.itexpert.content.lib.models.UserPost;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping(value = "/v0/users")
public class UserEndPoint {

    private final UserHandler userHandler;

    public UserEndPoint(UserHandler userHandler) {
        this.userHandler = userHandler;
    }

    @GetMapping("/")
    public Flux<UserPost> findAll(Authentication authentication) {
        var grantedAuthority = authentication.getAuthorities().stream().findFirst().get();

        if (grantedAuthority.getAuthority().equals(RoleEnum.ADMIN.name())) {
            return userHandler.findAll();
        }
        return Flux.empty();
    }

    @GetMapping(value = "/id/{id}")
    public Mono<ResponseEntity<UserPost>> findById(@PathVariable UUID id) {
        return userHandler.findById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @GetMapping(value = "/email/{email}")
    public Mono<ResponseEntity<UserPost>> findByEmail(@PathVariable String email) {
        return userHandler.findByEmail(email)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(new ResponseEntity<>(HttpStatus.NOT_FOUND));

    }

    //@RolesAllowed("ADMIN")
    @PostMapping("/")
    public Mono<ResponseEntity<UserPost>> save(@RequestBody(required = true) UserPost user) {
        return userHandler.save(user)
                .map(ResponseEntity::ok);
    }

    @PostMapping(value = "/password")
    public Mono<ResponseEntity<Boolean>> changePassword(@RequestBody(required = true)
                                                        UserPassword userPassword) {
        return userHandler.changePassword(userPassword)
                .map(ResponseEntity::ok);
    }

    //@RolesAllowed("ADMIN")
    @DeleteMapping(value = "/id/{id}")
    public Mono<ResponseEntity<Boolean>> delete(@PathVariable String id) {
        return userHandler.delete(UUID.fromString(id))
                .map(ResponseEntity::ok);
    }

}
