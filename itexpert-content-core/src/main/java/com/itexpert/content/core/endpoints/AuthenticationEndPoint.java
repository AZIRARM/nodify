package com.itexpert.content.core.endpoints;

import com.itexpert.content.core.handlers.UserHandler;
import com.itexpert.content.core.models.auth.AuthResponse;
import com.itexpert.content.core.utils.auth.JWTUtil;
import com.itexpert.content.core.utils.auth.PBKDF2Encoder;
import com.itexpert.content.lib.models.UserLogin;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping(value = "/authentication")
public class AuthenticationEndPoint {
    private final JWTUtil jwtUtil;
    private final PBKDF2Encoder passwordEncoder;
    private final UserHandler userHandler;

    public AuthenticationEndPoint(JWTUtil jwtUtil, PBKDF2Encoder passwordEncoder, UserHandler userHandler) {
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
        this.userHandler = userHandler;
    }

    @PostMapping("/login")
    public Mono<ResponseEntity<AuthResponse>> login(@RequestBody UserLogin userLogin) {
        return userHandler.findByEmail(userLogin.getEmail())
                .doOnNext(userPost -> {
                    passwordEncoder.matches(passwordEncoder.encode(userLogin.getPassword()), userPost.getPassword());
                })
                .filter(userDetails -> passwordEncoder.encode(userLogin.getPassword()).equals(userDetails.getPassword()))
                .map(userDetails -> ResponseEntity.ok(new AuthResponse(jwtUtil.generateToken(userDetails))))
                .switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()));
    }



}
