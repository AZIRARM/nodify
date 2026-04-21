package com.itexpert.content.core.utils.auth;

import com.itexpert.content.core.config.SecurityProperties;
import com.itexpert.content.core.handlers.UserHandler;
import com.itexpert.content.core.handlers.oauth2.OAuth2Service;
import com.itexpert.content.core.handlers.openid.OpenIDService;
import com.itexpert.content.lib.models.UserPost;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = {
        AuthenticationManager.class,
        JWTUtil.class,
        UserHandler.class,
        SecurityProperties.class
})
class AuthenticationManagerTest {

    @MockBean
    private JWTUtil jwtUtil;

    @MockBean
    private UserHandler userHandler;

    @MockBean
    private OAuth2Service oauth2Service;

    @MockBean
    private OpenIDService openIDService;

    @MockBean
    private SecurityProperties securityProperties;

    @Autowired
    private AuthenticationManager authenticationManager;

    private Authentication authentication;
    private final String validInternalToken = "valid.internal.jwt.token";
    private final String username = "test@example.com";

    @BeforeEach
    void setUp() {
        authentication = new UsernamePasswordAuthenticationToken(username, validInternalToken);
    }

    // ==================== TESTS MODE INTERNE ====================

    @Test
    void authenticateWithInternalJWTSuccess() {
        when(securityProperties.getMode()).thenReturn("internal");
        when(jwtUtil.validateToken(validInternalToken)).thenReturn(true);
        when(jwtUtil.getUsernameFromToken(validInternalToken)).thenReturn(username);

        Claims claims = mock(Claims.class);
        when(jwtUtil.getAllClaimsFromToken(validInternalToken)).thenReturn(claims);
        List<String> roles = List.of("ADMIN", "EDITOR");
        when(claims.get("role", List.class)).thenReturn(roles);

        Mono<Authentication> result = authenticationManager.authenticate(authentication);

        StepVerifier.create(result)
                .assertNext(auth -> {
                    assertEquals(username, auth.getPrincipal());
                    assertTrue(auth.getAuthorities().contains(new SimpleGrantedAuthority("ADMIN")));
                    assertTrue(auth.getAuthorities().contains(new SimpleGrantedAuthority("EDITOR")));
                })
                .verifyComplete();

        verify(jwtUtil, times(1)).validateToken(validInternalToken);
        verify(jwtUtil, times(1)).getUsernameFromToken(validInternalToken);
    }

    @Test
    void authenticateWithInternalJWTInvalidToken() {
        when(securityProperties.getMode()).thenReturn("internal");
        when(jwtUtil.validateToken(validInternalToken)).thenReturn(false);

        Mono<Authentication> result = authenticationManager.authenticate(authentication);

        StepVerifier.create(result).verifyComplete();
        verify(jwtUtil, times(1)).validateToken(validInternalToken);
        verify(jwtUtil, never()).getUsernameFromToken(anyString());
    }

    @Test
    void authenticateWithInternalJWTNoRoles() {
        when(securityProperties.getMode()).thenReturn("internal");
        when(jwtUtil.validateToken(validInternalToken)).thenReturn(true);
        when(jwtUtil.getUsernameFromToken(validInternalToken)).thenReturn(username);

        Claims claims = mock(Claims.class);
        when(jwtUtil.getAllClaimsFromToken(validInternalToken)).thenReturn(claims);
        when(claims.get("role", List.class)).thenReturn(null);

        Mono<Authentication> result = authenticationManager.authenticate(authentication);

        StepVerifier.create(result).verifyComplete();
        verify(jwtUtil, times(1)).validateToken(validInternalToken);
    }

    @Test
    void authenticateWithInternalJWTInvalidRoles() {
        when(securityProperties.getMode()).thenReturn("internal");
        when(jwtUtil.validateToken(validInternalToken)).thenReturn(true);
        when(jwtUtil.getUsernameFromToken(validInternalToken)).thenReturn(username);

        Claims claims = mock(Claims.class);
        when(jwtUtil.getAllClaimsFromToken(validInternalToken)).thenReturn(claims);
        List<String> roles = List.of("INVALID_ROLE", "WRONG_ROLE");
        when(claims.get("role", List.class)).thenReturn(roles);

        Mono<Authentication> result = authenticationManager.authenticate(authentication);

        StepVerifier.create(result).verifyComplete();
    }

    // ==================== TESTS MODE OAUTH2 ====================

    private String buildTokenWithClaim(String claimName, String claimValue) {
        String payload = String.format("{\"%s\":\"%s\"}", claimName, claimValue);
        String encodedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes());
        return "header." + encodedPayload + ".signature";
    }

    @Test
    void authenticateWithOAuth2Success_EmailClaim() {
        when(securityProperties.getMode()).thenReturn("oauth2");
        String token = buildTokenWithClaim("email", username);
        Authentication oauth2Auth = new UsernamePasswordAuthenticationToken(token, token);

        UserPost user = new UserPost();
        user.setEmail(username);
        user.setRoles(List.of("ADMIN", "READER"));

        when(userHandler.findByEmail(username)).thenReturn(Mono.just(user));

        Mono<Authentication> result = authenticationManager.authenticate(oauth2Auth);

        StepVerifier.create(result)
                .assertNext(auth -> {
                    assertEquals(username, auth.getPrincipal());
                    assertTrue(auth.getAuthorities().contains(new SimpleGrantedAuthority("ADMIN")));
                    assertTrue(auth.getAuthorities().contains(new SimpleGrantedAuthority("READER")));
                })
                .verifyComplete();

        verify(userHandler, times(1)).findByEmail(username);
    }

    @Test
    void authenticateWithOAuth2Success_PreferredUsernameClaim() {
        when(securityProperties.getMode()).thenReturn("oauth2");
        String token = buildTokenWithClaim("preferred_username", username);
        Authentication oauth2Auth = new UsernamePasswordAuthenticationToken(token, token);

        UserPost user = new UserPost();
        user.setEmail(username);
        user.setRoles(List.of("EDITOR"));

        when(userHandler.findByEmail(username)).thenReturn(Mono.just(user));

        Mono<Authentication> result = authenticationManager.authenticate(oauth2Auth);

        StepVerifier.create(result)
                .expectNextCount(1)
                .verifyComplete();

        verify(userHandler, times(1)).findByEmail(username);
    }

    @Test
    void authenticateWithOAuth2Success_SubClaim() {
        when(securityProperties.getMode()).thenReturn("oauth2");
        String token = buildTokenWithClaim("sub", username);
        Authentication oauth2Auth = new UsernamePasswordAuthenticationToken(token, token);

        UserPost user = new UserPost();
        user.setEmail(username);
        user.setRoles(List.of("READER"));

        when(userHandler.findByEmail(username)).thenReturn(Mono.just(user));

        Mono<Authentication> result = authenticationManager.authenticate(oauth2Auth);

        StepVerifier.create(result)
                .expectNextCount(1)
                .verifyComplete();

        verify(userHandler, times(1)).findByEmail(username);
    }

    @Test
    void authenticateWithOAuth2UserNotFound() {
        when(securityProperties.getMode()).thenReturn("oauth2");
        String token = buildTokenWithClaim("email", username);
        Authentication oauth2Auth = new UsernamePasswordAuthenticationToken(token, token);

        when(userHandler.findByEmail(username)).thenReturn(Mono.empty());

        Mono<Authentication> result = authenticationManager.authenticate(oauth2Auth);

        StepVerifier.create(result).verifyComplete();
        verify(userHandler, times(1)).findByEmail(username);
    }

    @Test
    void authenticateWithOAuth2InvalidTokenFormat() {
        when(securityProperties.getMode()).thenReturn("oauth2");
        String invalidToken = "invalid.token.format";
        Authentication oauth2Auth = new UsernamePasswordAuthenticationToken(invalidToken, invalidToken);

        Mono<Authentication> result = authenticationManager.authenticate(oauth2Auth);

        StepVerifier.create(result).verifyComplete();
        verify(userHandler, never()).findByEmail(anyString());
    }

    @Test
    void authenticateWithOAuth2NoEmailClaim() {
        when(securityProperties.getMode()).thenReturn("oauth2");
        String token = buildTokenWithClaim("other", "value");
        Authentication oauth2Auth = new UsernamePasswordAuthenticationToken(token, token);

        Mono<Authentication> result = authenticationManager.authenticate(oauth2Auth);

        StepVerifier.create(result).verifyComplete();
        verify(userHandler, never()).findByEmail(anyString());
    }

    @Test
    void authenticateWithOAuth2RolesNotAuthorized() {
        when(securityProperties.getMode()).thenReturn("oauth2");
        String token = buildTokenWithClaim("email", username);
        Authentication oauth2Auth = new UsernamePasswordAuthenticationToken(token, token);

        UserPost user = new UserPost();
        user.setEmail(username);
        user.setRoles(List.of("UNKNOWN_ROLE")); // aucun rôle autorisé

        when(userHandler.findByEmail(username)).thenReturn(Mono.just(user));

        Mono<Authentication> result = authenticationManager.authenticate(oauth2Auth);

        StepVerifier.create(result)
                .assertNext(auth -> {
                    assertNotNull(auth);
                    assertEquals(username, auth.getPrincipal());
                    assertTrue(auth.getAuthorities().isEmpty());
                })
                .verifyComplete();
        verify(userHandler, times(1)).findByEmail(username);
    }

    // ==================== TESTS MODE OPENID ====================

    @Test
    void authenticateWithOpenIDSuccess_EmailClaim() {
        when(securityProperties.getMode()).thenReturn("openid");
        String token = buildTokenWithClaim("email", username);
        Authentication openidAuth = new UsernamePasswordAuthenticationToken(token, token);

        UserPost user = new UserPost();
        user.setEmail(username);
        user.setRoles(List.of("EDITOR"));

        when(userHandler.findByEmail(username)).thenReturn(Mono.just(user));

        Mono<Authentication> result = authenticationManager.authenticate(openidAuth);

        StepVerifier.create(result)
                .expectNextCount(1)
                .verifyComplete();

        verify(userHandler, times(1)).findByEmail(username);
    }

    @Test
    void authenticateWithOpenIDSuccess_PreferredUsernameClaim() {
        when(securityProperties.getMode()).thenReturn("openid");
        String token = buildTokenWithClaim("preferred_username", username);
        Authentication openidAuth = new UsernamePasswordAuthenticationToken(token, token);

        UserPost user = new UserPost();
        user.setEmail(username);
        user.setRoles(List.of("ADMIN"));

        when(userHandler.findByEmail(username)).thenReturn(Mono.just(user));

        Mono<Authentication> result = authenticationManager.authenticate(openidAuth);

        StepVerifier.create(result)
                .expectNextCount(1)
                .verifyComplete();

        verify(userHandler, times(1)).findByEmail(username);
    }

    @Test
    void authenticateWithOpenIDUserNotFound() {
        when(securityProperties.getMode()).thenReturn("openid");
        String token = buildTokenWithClaim("email", username);
        Authentication openidAuth = new UsernamePasswordAuthenticationToken(token, token);

        when(userHandler.findByEmail(username)).thenReturn(Mono.empty());

        Mono<Authentication> result = authenticationManager.authenticate(openidAuth);

        StepVerifier.create(result).verifyComplete();
        verify(userHandler, times(1)).findByEmail(username);
    }

    @Test
    void authenticateWithOpenIDInvalidTokenFormat() {
        when(securityProperties.getMode()).thenReturn("openid");
        String invalidToken = "invalid.token.format";
        Authentication openidAuth = new UsernamePasswordAuthenticationToken(invalidToken, invalidToken);

        Mono<Authentication> result = authenticationManager.authenticate(openidAuth);

        StepVerifier.create(result).verifyComplete();
        verify(userHandler, never()).findByEmail(anyString());
    }

    @Test
    void authenticateWithOpenIDNoEmailClaim() {
        when(securityProperties.getMode()).thenReturn("openid");
        String token = buildTokenWithClaim("other", "value");
        Authentication openidAuth = new UsernamePasswordAuthenticationToken(token, token);

        Mono<Authentication> result = authenticationManager.authenticate(openidAuth);

        StepVerifier.create(result).verifyComplete();
        verify(userHandler, never()).findByEmail(anyString());
    }

    // ==================== AUTRES TESTS ====================

    @Test
    void authenticateWithUnknownModeFallsBackToInternal() {
        when(securityProperties.getMode()).thenReturn("unknown");
        when(jwtUtil.validateToken(validInternalToken)).thenReturn(true);
        when(jwtUtil.getUsernameFromToken(validInternalToken)).thenReturn(username);

        Claims claims = mock(Claims.class);
        when(jwtUtil.getAllClaimsFromToken(validInternalToken)).thenReturn(claims);
        List<String> roles = List.of("ADMIN");
        when(claims.get("role", List.class)).thenReturn(roles);

        Mono<Authentication> result = authenticationManager.authenticate(authentication);

        StepVerifier.create(result)
                .assertNext(auth -> assertEquals(username, auth.getPrincipal()))
                .verifyComplete();
    }

    @Test
    void authenticateWithNullOAuth2Service() {
        when(securityProperties.getMode()).thenReturn("oauth2");
        AuthenticationManager managerWithoutOAuth2 = new AuthenticationManager(
                jwtUtil, userHandler, null, openIDService, securityProperties);

        String token = buildTokenWithClaim("email", username);
        Authentication oauth2Auth = new UsernamePasswordAuthenticationToken(token, token);

        Mono<Authentication> result = managerWithoutOAuth2.authenticate(oauth2Auth);

        StepVerifier.create(result).verifyComplete();
        verify(userHandler, never()).findByEmail(anyString());
    }

    @Test
    void authenticateWithNullOpenIDService() {
        when(securityProperties.getMode()).thenReturn("openid");
        AuthenticationManager managerWithoutOpenID = new AuthenticationManager(
                jwtUtil, userHandler, oauth2Service, null, securityProperties);

        String token = buildTokenWithClaim("email", username);
        Authentication openidAuth = new UsernamePasswordAuthenticationToken(token, token);

        Mono<Authentication> result = managerWithoutOpenID.authenticate(openidAuth);

        StepVerifier.create(result).verifyComplete();
        verify(userHandler, never()).findByEmail(anyString());
    }
}