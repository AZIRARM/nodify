package com.itexpert.content.core.endpoints;

import com.itexpert.content.core.handlers.UserParametersHandler;
import com.itexpert.content.lib.models.UserParameters;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping(value = "/v0/user-parameters")
public class UserParametersEndPoint {

    private final UserParametersHandler userParametersHandler;

    public UserParametersEndPoint(UserParametersHandler userParametersHandler) {
        this.userParametersHandler = userParametersHandler;
    }


    @GetMapping("/")
    public Flux<UserParameters> findAll() {
        return userParametersHandler.findAll();
    }

    @GetMapping(value = "/id/{id}")
    public Mono<ResponseEntity<UserParameters>> findById(@PathVariable String id) {
        return userParametersHandler.findById(UUID.fromString(id))
                .map(ResponseEntity::ok)
                .defaultIfEmpty(new ResponseEntity<>(HttpStatus.NOT_FOUND));

    }
    @GetMapping(value = "/user/{userId}")
    public Mono<ResponseEntity<UserParameters>> findByUserId(@PathVariable String userId) {
        return userParametersHandler.findByUserId(UUID.fromString(userId))
                .map(ResponseEntity::ok)
                .defaultIfEmpty(new ResponseEntity<>(HttpStatus.NOT_FOUND));

    }
    // @RolesAllowed("ADMIN")
    @PostMapping("/")
    public Mono<ResponseEntity<UserParameters>> save(@RequestBody(required = true) UserParameters userParameters) {
        return userParametersHandler.save(userParameters)
                .map(ResponseEntity::ok);
    }

    //@RolesAllowed("ADMIN")
    @DeleteMapping(value = "/id/{id}")
    public Mono<ResponseEntity<Boolean>> delete(@PathVariable String id) {
        return userParametersHandler.delete(UUID.fromString(id))
                .map(ResponseEntity::ok);
    }

}
