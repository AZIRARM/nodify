package com.itexpert.content.core.endpoints;

import com.itexpert.content.core.handlers.UserHandler;
import com.itexpert.content.core.models.auth.AuthResponse;
import com.itexpert.content.core.utils.auth.JWTUtil;
import com.itexpert.content.core.utils.auth.PBKDF2Encoder;
import com.itexpert.content.lib.models.UserLogin;
import com.itexpert.content.lib.models.UserPost;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthenticationEndPointTest {

    @Mock
    private JWTUtil jwtUtil;

    @Mock
    private PBKDF2Encoder passwordEncoder;

    @Mock
    private UserHandler userHandler;

    @InjectMocks
    private AuthenticationEndPoint authenticationEndPoint;

    private WebTestClient webTestClient;

    @BeforeEach
    public void setUp() {
        webTestClient = WebTestClient.bindToController(authenticationEndPoint).build();
    }

    @Test
    public void testLoginSuccess() {
        String email = "test@example.com";
        String password = "password123";
        String encodedPassword = "encodedPassword123";
        String token = "jwt-token-123";

        UserLogin userLogin = new UserLogin();
        userLogin.setEmail(email);
        userLogin.setPassword(password);

        UserPost userPost = new UserPost();
        userPost.setEmail(email);
        userPost.setPassword(encodedPassword);
        userPost.setValidated(Boolean.TRUE);

        when(userHandler.findByEmail(email)).thenReturn(Mono.just(userPost));
        when(passwordEncoder.encode(password)).thenReturn(encodedPassword);
        when(jwtUtil.generateToken(userPost)).thenReturn(token);

        webTestClient.post()
                .uri("/authentication/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(userLogin)
                .exchange()
                .expectStatus().isOk()
                .expectBody(AuthResponse.class)
                .value(authResponse -> {
                    assert authResponse.getToken().equals(token);
                });

        verify(userHandler, times(1)).findByEmail(email);
        verify(passwordEncoder, atLeastOnce()).encode(password);
        verify(jwtUtil, times(1)).generateToken(userPost);
    }

    @Test
    public void testLoginErrorNotValidatedAccount() {
        String email = "test@example.com";
        String password = "password123";
        String encodedPassword = "encodedPassword123";
        String token = "jwt-token-123";

        UserLogin userLogin = new UserLogin();
        userLogin.setEmail(email);
        userLogin.setPassword(password);

        UserPost userPost = new UserPost();
        userPost.setEmail(email);
        userPost.setPassword(encodedPassword);
        userPost.setValidated(Boolean.FALSE);

        when(userHandler.findByEmail(email)).thenReturn(Mono.just(userPost));
        when(passwordEncoder.encode(password)).thenReturn(encodedPassword);

        webTestClient.post()
                .uri("/authentication/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(userLogin)
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody().isEmpty();

        verify(userHandler, times(1)).findByEmail(email);
        verify(passwordEncoder, atLeastOnce()).encode(password);
    }

    @Test
    public void testLoginInvalidPassword() {
        String email = "test@example.com";
        String password = "wrongPassword";
        String encodedPassword = "encodedPassword123";

        UserLogin userLogin = new UserLogin();
        userLogin.setEmail(email);
        userLogin.setPassword(password);

        UserPost userPost = new UserPost();
        userPost.setEmail(email);
        userPost.setPassword("differentEncodedPassword");

        when(userHandler.findByEmail(email)).thenReturn(Mono.just(userPost));
        when(passwordEncoder.encode(password)).thenReturn(encodedPassword);

        webTestClient.post()
                .uri("/authentication/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(userLogin)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody().isEmpty();

        verify(userHandler, times(1)).findByEmail(email);
        verify(passwordEncoder, atLeastOnce()).encode(password);
        verify(jwtUtil, never()).generateToken(any());
    }

    @Test
    public void testLoginUserNotFound() {
        String email = "nonexistent@example.com";
        String password = "password123";

        UserLogin userLogin = new UserLogin();
        userLogin.setEmail(email);
        userLogin.setPassword(password);

        when(userHandler.findByEmail(email)).thenReturn(Mono.empty());

        webTestClient.post()
                .uri("/authentication/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(userLogin)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody().isEmpty();

        verify(userHandler, times(1)).findByEmail(email);
        verify(passwordEncoder, never()).encode(anyString());
        verify(jwtUtil, never()).generateToken(any());
    }

    @Test
    public void testLoginWithNullPassword() {
        String email = "test@example.com";

        UserLogin userLogin = new UserLogin();
        userLogin.setEmail(email);
        userLogin.setPassword(null);

        when(userHandler.findByEmail(email)).thenReturn(Mono.empty());

        webTestClient.post()
                .uri("/authentication/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(userLogin)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody().isEmpty();

        verify(userHandler, times(1)).findByEmail(email);
        verify(passwordEncoder, never()).encode(anyString());
        verify(jwtUtil, never()).generateToken(any());
    }

    @Test
    public void testLoginWithNullEmail() {
        String password = "password123";

        UserLogin userLogin = new UserLogin();
        userLogin.setEmail(null);
        userLogin.setPassword(password);

        when(userHandler.findByEmail(null)).thenReturn(Mono.empty());

        webTestClient.post()
                .uri("/authentication/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(userLogin)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody().isEmpty();

        verify(userHandler, times(1)).findByEmail(null);
        verify(passwordEncoder, never()).encode(anyString());
        verify(jwtUtil, never()).generateToken(any());
    }

    @Test
    public void testLoginWithEmptyCredentials() {
        UserLogin userLogin = new UserLogin();
        userLogin.setEmail("");
        userLogin.setPassword("");

        when(userHandler.findByEmail("")).thenReturn(Mono.empty());

        webTestClient.post()
                .uri("/authentication/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(userLogin)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody().isEmpty();

        verify(userHandler, times(1)).findByEmail("");
        verify(passwordEncoder, never()).encode(anyString());
        verify(jwtUtil, never()).generateToken(any());
    }

    @Test
    public void testLoginWithEmptyEmail() {
        UserLogin userLogin = new UserLogin();
        userLogin.setEmail("");
        userLogin.setPassword("password123");

        when(userHandler.findByEmail("")).thenReturn(Mono.empty());

        webTestClient.post()
                .uri("/authentication/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(userLogin)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody().isEmpty();

        verify(userHandler, times(1)).findByEmail("");
        verify(passwordEncoder, never()).encode(anyString());
        verify(jwtUtil, never()).generateToken(any());
    }

    @Test
    public void testLoginWithEmptyPassword() {
        String email = "test@example.com";

        UserLogin userLogin = new UserLogin();
        userLogin.setEmail(email);
        userLogin.setPassword("");

        when(userHandler.findByEmail(email)).thenReturn(Mono.empty());

        webTestClient.post()
                .uri("/authentication/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(userLogin)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody().isEmpty();

        verify(userHandler, times(1)).findByEmail(email);
        verify(passwordEncoder, never()).encode(anyString());
        verify(jwtUtil, never()).generateToken(any());
    }
}