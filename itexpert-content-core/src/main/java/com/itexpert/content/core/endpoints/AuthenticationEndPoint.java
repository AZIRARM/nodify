package com.itexpert.content.core.endpoints;

import com.itexpert.content.core.handlers.UserHandler;
import com.itexpert.content.core.models.auth.AuthResponse;
import com.itexpert.content.core.utils.auth.JWTUtil;
import com.itexpert.content.core.utils.auth.PBKDF2Encoder;
import com.itexpert.content.lib.models.UserLogin;
import com.itexpert.content.core.utils.auth.RefreshTokenRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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
                .map(userDetails -> ResponseEntity.ok(new AuthResponse(jwtUtil.generateToken(userDetails), jwtUtil.generateRefreshToken(userDetails))))
                .switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()));
    }

    @PostMapping("/refresh")
    public Mono<ResponseEntity<AuthResponse>> refreshToken(@RequestBody RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        // Valide le refresh token ici (exemple simplifié)
        if (refreshToken == null || !jwtUtil.validateToken(refreshToken)) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }

        String userEmail = jwtUtil.getUsernameFromToken(refreshToken);

        return userHandler.findByEmail(userEmail)
                .map(userDetails -> {
                    // Génère un nouveau accessToken
                    String newAccessToken = jwtUtil.generateToken(userDetails);
                    // Génère un nouveau refreshToken (optionnel, ici même méthode)
                    String newRefreshToken = jwtUtil.generateRefreshToken(userDetails);

                    return ResponseEntity.ok(new AuthResponse(newAccessToken, newRefreshToken));
                })
                .switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()));
    }
}
