package com.itexpert.content.core.endpoints;

import com.itexpert.content.core.config.OAuth2Properties;
import com.itexpert.content.core.config.OpenIDProperties;
import com.itexpert.content.core.config.SecurityProperties;
import com.itexpert.content.core.handlers.UserHandler;
import com.itexpert.content.core.handlers.oauth2.OAuth2Service;
import com.itexpert.content.core.handlers.openid.OpenIDService;
import com.itexpert.content.core.models.auth.AuthResponse;
import com.itexpert.content.core.models.auth.RoleEnum;
import com.itexpert.content.core.models.oauth.AuthUserInfo;
import com.itexpert.content.core.utils.auth.JWTUtil;
import com.itexpert.content.core.utils.auth.PBKDF2Encoder;
import com.itexpert.content.lib.models.UserLogin;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/authentication")
public class AuthenticationEndPoint {
    private final JWTUtil jwtUtil;
    private final PBKDF2Encoder passwordEncoder;
    private final UserHandler userHandler;
    private final SecurityProperties securityProperties;
    private final OAuth2Properties oauth2Properties;
    private final OpenIDProperties openidProperties;
    private final OAuth2Service oauth2Service;
    private final OpenIDService openidService;

    public AuthenticationEndPoint(JWTUtil jwtUtil,
            PBKDF2Encoder passwordEncoder,
            UserHandler userHandler,
            SecurityProperties securityProperties,
            OAuth2Properties oauth2Properties,
            OpenIDProperties openidProperties,
            OAuth2Service oauth2Service,
            OpenIDService openidService) {
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
        this.userHandler = userHandler;
        this.securityProperties = securityProperties;
        this.oauth2Properties = oauth2Properties;
        this.openidProperties = openidProperties;
        this.oauth2Service = oauth2Service;
        this.openidService = openidService;
    }

    @PostMapping("/login")
    public Mono<ResponseEntity<AuthResponse>> login(@RequestBody UserLogin userLogin) {
        return userHandler.findByEmail(userLogin.getEmail())
                .filter(userDetails -> passwordEncoder.encode(userLogin.getPassword()).equals(userDetails.getPassword())
                        && (userDetails.getValidated() || userDetails.getRoles().contains(RoleEnum.ADMIN.name())))
                .map(userDetails -> ResponseEntity.ok(new AuthResponse(jwtUtil.generateToken(userDetails))))
                .switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()));
    }

    @GetMapping("/mode")
    public Mono<Map<String, String>> getAuthMode() {
        String mode = securityProperties.getMode();
        return Mono.just(Map.of(
                "mode", mode,
                "internalEnabled", String.valueOf("internal".equals(mode)),
                "oauth2Enabled", String.valueOf(oauth2Properties.isEnabled() && "oauth2".equals(mode)),
                "openidEnabled", String.valueOf(openidProperties.isEnabled() && "openid".equals(mode))));
    }

    @PostMapping("/oauth2/token")
    public Mono<ResponseEntity<AuthResponse>> oauth2Token(@RequestBody UserLogin userLogin) {
        log.info("OAuth2 token request for user: {}", userLogin.getEmail());

        return oauth2Service.validateAndGetUserInfo(userLogin.getPassword())
                .flatMap(authUserInfo -> {
                    String email = authUserInfo.getEmail() != null ? authUserInfo.getEmail()
                            : authUserInfo.getUsername();
                    return userHandler.findByEmail(email)
                            .flatMap(user -> {
                                String token = jwtUtil.generateToken(user);
                                AuthResponse response = new AuthResponse(token);
                                return Mono.just(ResponseEntity.ok(response));
                            })
                            .switchIfEmpty(Mono.defer(() -> {
                                log.warn("User not found in database: {}", email);
                                return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
                            }));
                })
                .onErrorResume(error -> {
                    log.error("OAuth2 authentication failed: {}", error.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
                });
    }

    @PostMapping("/openid/token")
    public Mono<ResponseEntity<AuthResponse>> openidToken(@RequestBody UserLogin userLogin) {
        log.info("OpenID token request for user: {}", userLogin.getEmail());

        return openidService.validateAndGetUserInfo(userLogin.getPassword())
                .flatMap(authUserInfo -> {
                    String email = authUserInfo.getEmail() != null ? authUserInfo.getEmail()
                            : authUserInfo.getUsername();
                    return userHandler.findByEmail(email)
                            .flatMap(user -> {
                                String token = jwtUtil.generateToken(user);
                                AuthResponse response = new AuthResponse(token);
                                return Mono.just(ResponseEntity.ok(response));
                            })
                            .switchIfEmpty(Mono.defer(() -> {
                                log.warn("User not found in database: {}", email);
                                return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
                            }));
                })
                .onErrorResume(error -> {
                    log.error("OpenID authentication failed: {}", error.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
                });
    }

    @PostMapping("/oauth2/logout")
    public Mono<ResponseEntity<Void>> oauth2Logout() {
        log.info("OAuth2 logout request");
        // La déconnexion côté serveur est gérée par le token invalidation
        // Le client doit supprimer son token localement
        return Mono.just(ResponseEntity.ok().build());
    }

    @PostMapping("/openid/logout")
    public Mono<ResponseEntity<Void>> openidLogout() {
        log.info("OpenID logout request");
        // La déconnexion côté serveur est gérée par le token invalidation
        // Le client doit supprimer son token localement
        return Mono.just(ResponseEntity.ok().build());
    }

    @GetMapping("/oauth2/authorize")
    public Mono<Void> oauth2Authorize(ServerWebExchange exchange) {
        String scope = oauth2Properties.getConfig().getScope().replace(",", " ");
        String encodedScope = URLEncoder.encode(scope, StandardCharsets.UTF_8);

        String redirectUri = oauth2Properties.getConfig().getAuthorizationUri() +
                "?client_id=" + oauth2Properties.getConfig().getClientId() +
                "&redirect_uri=" + oauth2Properties.getConfig().getRedirectUri() +
                "&response_type=code" +
                "&scope=" + encodedScope;

        log.info("Redirect URL: {}", redirectUri);

        exchange.getResponse().setStatusCode(HttpStatus.FOUND);
        exchange.getResponse().getHeaders().setLocation(URI.create(redirectUri));
        return exchange.getResponse().setComplete();
    }

    @GetMapping("/openid/authorize")
    public Mono<Void> openidAuthorize(ServerWebExchange exchange) {
        String scope = "openid profile email";
        String encodedScope = URLEncoder.encode(scope, StandardCharsets.UTF_8);

        String redirectUri = openidProperties.getConfig().getAuthorizationUri() +
                "?client_id=" + openidProperties.getConfig().getClientId() +
                "&redirect_uri=" + openidProperties.getConfig().getRedirectUri() +
                "&response_type=code" +
                "&scope=" + encodedScope;

        log.info("Redirect URL: {}", redirectUri);

        exchange.getResponse().setStatusCode(HttpStatus.FOUND);
        exchange.getResponse().getHeaders().setLocation(URI.create(redirectUri));
        return exchange.getResponse().setComplete();
    }

    @GetMapping("/oauth2/callback")
    public Mono<Void> oauth2Callback(@RequestParam String code, ServerWebExchange exchange) {
        log.info("OAuth2 callback received with code: {}", code);

        return oauth2Service.exchangeCodeForToken(code)
                .flatMap(tokenResponse -> {
                    String accessToken = tokenResponse.getAccess_token();
                    String redirectUri = oauth2Properties.getConfig().getStudioUri() + "?token=" + accessToken;
                    exchange.getResponse().setStatusCode(HttpStatus.FOUND);
                    exchange.getResponse().getHeaders().setLocation(URI.create(redirectUri));
                    return exchange.getResponse().setComplete();
                })
                .onErrorResume(error -> {
                    log.error("OAuth2 callback error: {}", error.getMessage());
                    String redirectUri = oauth2Properties.getConfig().getRedirectUri() + "?error=true";
                    exchange.getResponse().setStatusCode(HttpStatus.FOUND);
                    exchange.getResponse().getHeaders().setLocation(URI.create(redirectUri));
                    return exchange.getResponse().setComplete();
                });
    }

    @GetMapping("/openid/callback")
    public Mono<Void> openidCallback(@RequestParam String code, ServerWebExchange exchange) {
        log.info("OpenID callback received with code: {}", code);

        return openidService.exchangeCodeForToken(code)
                .flatMap(tokenResponse -> {
                    String accessToken = tokenResponse.getAccess_token();
                    String redirectUri = openidProperties.getConfig().getStudioUri() + "?token=" + accessToken;
                    exchange.getResponse().setStatusCode(HttpStatus.FOUND);
                    exchange.getResponse().getHeaders().setLocation(URI.create(redirectUri));
                    return exchange.getResponse().setComplete();
                })
                .onErrorResume(error -> {
                    log.error("OpenID callback error: {}", error.getMessage());
                    String redirectUri = openidProperties.getConfig().getRedirectUri() + "?error=true";
                    exchange.getResponse().setStatusCode(HttpStatus.FOUND);
                    exchange.getResponse().getHeaders().setLocation(URI.create(redirectUri));
                    return exchange.getResponse().setComplete();
                });
    }

}