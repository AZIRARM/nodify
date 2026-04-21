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
    private final String validToken = "valid.jwt.token";
    private final String username = "test@example.com";

    @BeforeEach
    void setUp() {
        authentication = new UsernamePasswordAuthenticationToken(username, validToken);
    }

    @Test
    void authenticateWithInternalJWTSuccess() {
        // Given
        when(securityProperties.getMode()).thenReturn("internal");
        when(jwtUtil.validateToken(validToken)).thenReturn(true);
        when(jwtUtil.getUsernameFromToken(validToken)).thenReturn(username);

        Claims claims = mock(Claims.class);
        when(jwtUtil.getAllClaimsFromToken(validToken)).thenReturn(claims);

        List<String> roles = List.of("ADMIN", "EDITOR");
        when(claims.get("role", List.class)).thenReturn(roles);

        // When
        Mono<Authentication> result = authenticationManager.authenticate(authentication);

        // Then
        StepVerifier.create(result)
                .assertNext(auth -> {
                    assertNotNull(auth);
                    assertEquals(username, auth.getPrincipal());
                    assertTrue(auth.getAuthorities().contains(new SimpleGrantedAuthority("ADMIN")));
                    assertTrue(auth.getAuthorities().contains(new SimpleGrantedAuthority("EDITOR")));
                })
                .verifyComplete();

        verify(jwtUtil, times(1)).validateToken(validToken);
        verify(jwtUtil, times(1)).getUsernameFromToken(validToken);
        verify(jwtUtil, times(1)).getAllClaimsFromToken(validToken);
    }

    @Test
    void authenticateWithInternalJWTInvalidToken() {
        // Given
        when(securityProperties.getMode()).thenReturn("internal");
        when(jwtUtil.validateToken(validToken)).thenReturn(false);

        // When
        Mono<Authentication> result = authenticationManager.authenticate(authentication);

        // Then
        StepVerifier.create(result)
                .verifyComplete(); // Mono.empty() returns empty

        verify(jwtUtil, times(1)).validateToken(validToken);
        verify(jwtUtil, never()).getUsernameFromToken(anyString());
        verify(jwtUtil, never()).getAllClaimsFromToken(anyString());
    }

    @Test
    void authenticateWithInternalJWTNoRoles() {
        // Given
        when(securityProperties.getMode()).thenReturn("internal");
        when(jwtUtil.validateToken(validToken)).thenReturn(true);
        when(jwtUtil.getUsernameFromToken(validToken)).thenReturn(username);

        Claims claims = mock(Claims.class);
        when(jwtUtil.getAllClaimsFromToken(validToken)).thenReturn(claims);
        when(claims.get("role", List.class)).thenReturn(null);

        // When
        Mono<Authentication> result = authenticationManager.authenticate(authentication);

        // Then
        StepVerifier.create(result)
                .verifyComplete(); // Mono.empty() because no authorities

        verify(jwtUtil, times(1)).validateToken(validToken);
        verify(jwtUtil, times(1)).getUsernameFromToken(validToken);
        verify(jwtUtil, times(1)).getAllClaimsFromToken(validToken);
    }

    @Test
    void authenticateWithInternalJWTInvalidRoles() {
        // Given
        when(securityProperties.getMode()).thenReturn("internal");
        when(jwtUtil.validateToken(validToken)).thenReturn(true);
        when(jwtUtil.getUsernameFromToken(validToken)).thenReturn(username);

        Claims claims = mock(Claims.class);
        when(jwtUtil.getAllClaimsFromToken(validToken)).thenReturn(claims);

        List<String> roles = List.of("INVALID_ROLE", "WRONG_ROLE");
        when(claims.get("role", List.class)).thenReturn(roles);

        // When
        Mono<Authentication> result = authenticationManager.authenticate(authentication);

        // Then
        StepVerifier.create(result)
                .verifyComplete(); // Mono.empty() because no valid authorities

        verify(jwtUtil, times(1)).validateToken(validToken);
        verify(jwtUtil, times(1)).getUsernameFromToken(validToken);
    }

    @Test
    void authenticateWithOAuth2Success() {
        // Given
        when(securityProperties.getMode()).thenReturn("oauth2");

        String oauth2Token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJlbWFpbCI6InRlc3RAZXhhbXBsZS5jb20ifQ.signature";
        Authentication oauth2Auth = new UsernamePasswordAuthenticationToken(username, oauth2Token);

        UserPost user = new UserPost();
        user.setEmail(username);
        user.setRoles(List.of("ADMIN", "READER"));

        when(userHandler.findByEmail(username)).thenReturn(Mono.just(user));

        // When
        Mono<Authentication> result = authenticationManager.authenticate(oauth2Auth);

        // Then
        StepVerifier.create(result)
                .assertNext(auth -> {
                    assertNotNull(auth);
                    assertEquals(username, auth.getPrincipal());
                    assertTrue(auth.getAuthorities().contains(new SimpleGrantedAuthority("ADMIN")));
                    assertTrue(auth.getAuthorities().contains(new SimpleGrantedAuthority("READER")));
                })
                .verifyComplete();

        verify(userHandler, times(1)).findByEmail(username);
    }

    @Test
    void authenticateWithOAuth2UserNotFound() {
        // Given
        when(securityProperties.getMode()).thenReturn("oauth2");

        String oauth2Token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJlbWFpbCI6InRlc3RAZXhhbXBsZS5jb20ifQ.signature";
        Authentication oauth2Auth = new UsernamePasswordAuthenticationToken(username, oauth2Token);

        when(userHandler.findByEmail(username)).thenReturn(Mono.empty());

        // When
        Mono<Authentication> result = authenticationManager.authenticate(oauth2Auth);

        // Then
        StepVerifier.create(result)
                .verifyComplete(); // Mono.empty()

        verify(userHandler, times(1)).findByEmail(username);
    }

    @Test
    void authenticateWithOpenIDSuccess() {
        // Given
        when(securityProperties.getMode()).thenReturn("openid");

        String openidToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJlbWFpbCI6InRlc3RAZXhhbXBsZS5jb20ifQ.signature";
        Authentication openidAuth = new UsernamePasswordAuthenticationToken(username, openidToken);

        UserPost user = new UserPost();
        user.setEmail(username);
        user.setRoles(List.of("EDITOR"));

        when(userHandler.findByEmail(username)).thenReturn(Mono.just(user));

        // When
        Mono<Authentication> result = authenticationManager.authenticate(openidAuth);

        // Then
        StepVerifier.create(result)
                .assertNext(auth -> {
                    assertNotNull(auth);
                    assertEquals(username, auth.getPrincipal());
                    assertTrue(auth.getAuthorities().contains(new SimpleGrantedAuthority("EDITOR")));
                })
                .verifyComplete();

        verify(userHandler, times(1)).findByEmail(username);
    }

    @Test
    void authenticateWithOpenIDUserNotFound() {
        // Given
        when(securityProperties.getMode()).thenReturn("openid");

        String openidToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJlbWFpbCI6InRlc3RAZXhhbXBsZS5jb20ifQ.signature";
        Authentication openidAuth = new UsernamePasswordAuthenticationToken(username, openidToken);

        when(userHandler.findByEmail(username)).thenReturn(Mono.empty());

        // When
        Mono<Authentication> result = authenticationManager.authenticate(openidAuth);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(userHandler, times(1)).findByEmail(username);
    }

    @Test
    void authenticateWithOAuth2InvalidToken() {
        // Given
        when(securityProperties.getMode()).thenReturn("oauth2");

        String invalidToken = "invalid.token.format";
        Authentication oauth2Auth = new UsernamePasswordAuthenticationToken(username, invalidToken);

        // When
        Mono<Authentication> result = authenticationManager.authenticate(oauth2Auth);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(userHandler, never()).findByEmail(anyString());
    }

    @Test
    void authenticateWithOpenIDInvalidToken() {
        // Given
        when(securityProperties.getMode()).thenReturn("openid");

        String invalidToken = "invalid.token.format";
        Authentication openidAuth = new UsernamePasswordAuthenticationToken(username, invalidToken);

        // When
        Mono<Authentication> result = authenticationManager.authenticate(openidAuth);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(userHandler, never()).findByEmail(anyString());
    }

    @Test
    void authenticateWithUnknownMode() {
        // Given
        when(securityProperties.getMode()).thenReturn("unknown");
        when(jwtUtil.validateToken(validToken)).thenReturn(true);
        when(jwtUtil.getUsernameFromToken(validToken)).thenReturn(username);

        Claims claims = mock(Claims.class);
        when(jwtUtil.getAllClaimsFromToken(validToken)).thenReturn(claims);

        List<String> roles = List.of("ADMIN");
        when(claims.get("role", List.class)).thenReturn(roles);

        // When
        Mono<Authentication> result = authenticationManager.authenticate(authentication);

        // Then
        StepVerifier.create(result)
                .assertNext(auth -> {
                    assertNotNull(auth);
                    assertEquals(username, auth.getPrincipal());
                })
                .verifyComplete();
    }

    @Test
    void extractEmailFromTokenWithEmailField() {
        // Given
        when(securityProperties.getMode()).thenReturn("oauth2");

        String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJlbWFpbCI6InRlc3RAZXhhbXBsZS5jb20iLCJzdWIiOiIxMjM0NTYifQ.signature";
        Authentication oauth2Auth = new UsernamePasswordAuthenticationToken(username, token);

        UserPost user = new UserPost();
        user.setEmail("test@example.com");
        user.setRoles(List.of("READER"));

        when(userHandler.findByEmail("test@example.com")).thenReturn(Mono.just(user));

        // When
        Mono<Authentication> result = authenticationManager.authenticate(oauth2Auth);

        // Then
        StepVerifier.create(result)
                .expectNextCount(1)
                .verifyComplete();

        verify(userHandler, times(1)).findByEmail("test@example.com");
    }

    @Test
    void extractEmailFromTokenWithPreferredUsername() {
        // Given
        when(securityProperties.getMode()).thenReturn("oauth2");

        String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJwcmVmZXJyZWRfdXNlcm5hbWUiOiJ0ZXN0QGV4YW1wbGUuY29tIn0.signature";
        Authentication oauth2Auth = new UsernamePasswordAuthenticationToken(username, token);

        UserPost user = new UserPost();
        user.setEmail("test@example.com");
        user.setRoles(List.of("READER"));

        when(userHandler.findByEmail("test@example.com")).thenReturn(Mono.just(user));

        // When
        Mono<Authentication> result = authenticationManager.authenticate(oauth2Auth);

        // Then
        StepVerifier.create(result)
                .expectNextCount(1)
                .verifyComplete();

        verify(userHandler, times(1)).findByEmail("test@example.com");
    }

    @Test
    void authenticateWithNullOAuth2Service() {
        // Given
        when(securityProperties.getMode()).thenReturn("oauth2");

        AuthenticationManager managerWithoutOAuth2 = new AuthenticationManager(
                jwtUtil, userHandler, null, openIDService, securityProperties);

        String oauth2Token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJlbWFpbCI6InRlc3RAZXhhbXBsZS5jb20ifQ.signature";
        Authentication oauth2Auth = new UsernamePasswordAuthenticationToken(username, oauth2Token);

        // When
        Mono<Authentication> result = managerWithoutOAuth2.authenticate(oauth2Auth);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(userHandler, never()).findByEmail(anyString());
    }

    @Test
    void authenticateWithNullOpenIDService() {
        // Given
        when(securityProperties.getMode()).thenReturn("openid");

        AuthenticationManager managerWithoutOpenID = new AuthenticationManager(
                jwtUtil, userHandler, oauth2Service, null, securityProperties);

        String openidToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJlbWFpbCI6InRlc3RAZXhhbXBsZS5jb20ifQ.signature";
        Authentication openidAuth = new UsernamePasswordAuthenticationToken(username, openidToken);

        // When
        Mono<Authentication> result = managerWithoutOpenID.authenticate(openidAuth);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(userHandler, never()).findByEmail(anyString());
    }
}