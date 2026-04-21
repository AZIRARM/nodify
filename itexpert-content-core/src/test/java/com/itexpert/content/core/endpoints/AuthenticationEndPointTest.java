package com.itexpert.content.core.endpoints;

import com.itexpert.content.core.config.OAuth2Properties;
import com.itexpert.content.core.config.OpenIDProperties;
import com.itexpert.content.core.config.SecurityProperties;
import com.itexpert.content.core.handlers.UserHandler;
import com.itexpert.content.core.handlers.oauth2.OAuth2Service;
import com.itexpert.content.core.handlers.openid.OpenIDService;
import com.itexpert.content.core.models.auth.AuthResponse;
import com.itexpert.content.core.models.oauth.AuthUserInfo;
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

import java.util.ArrayList;
import java.util.Map;

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

    @Mock
    private SecurityProperties securityProperties;

    @Mock
    private OAuth2Properties oauth2Properties;

    @Mock
    private OpenIDProperties openidProperties;

    @Mock
    private OAuth2Service oauth2Service;

    @Mock
    private OpenIDService openidService;

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

        UserLogin userLogin = new UserLogin();
        userLogin.setEmail(email);
        userLogin.setPassword(password);

        UserPost userPost = new UserPost();
        userPost.setEmail(email);
        userPost.setPassword(encodedPassword);
        userPost.setValidated(Boolean.FALSE);
        userPost.setRoles(new ArrayList<>()); // Ajouter une liste vide de rôles

        when(userHandler.findByEmail(email)).thenReturn(Mono.just(userPost));
        when(passwordEncoder.encode(password)).thenReturn(encodedPassword);

        webTestClient.post()
                .uri("/authentication/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(userLogin)
                .exchange()
                .expectStatus().isUnauthorized() // Maintenant ça devrait être 401
                .expectBody().isEmpty();

        verify(userHandler, times(1)).findByEmail(email);
        verify(passwordEncoder, atLeastOnce()).encode(password);
        verify(jwtUtil, never()).generateToken(any());
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

    @Test
    public void testGetAuthModeInternal() {
        when(securityProperties.getMode()).thenReturn("internal");
        when(oauth2Properties.isEnabled()).thenReturn(false);
        when(openidProperties.isEnabled()).thenReturn(false);

        webTestClient.get()
                .uri("/authentication/mode")
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .value(response -> {
                    assert response.get("mode").equals("internal");
                    assert response.get("internalEnabled").equals("true");
                    assert response.get("oauth2Enabled").equals("false");
                    assert response.get("openidEnabled").equals("false");
                });
    }

    @Test
    public void testGetAuthModeOAuth2() {
        when(securityProperties.getMode()).thenReturn("oauth2");
        when(oauth2Properties.isEnabled()).thenReturn(true);
        when(openidProperties.isEnabled()).thenReturn(false);

        webTestClient.get()
                .uri("/authentication/mode")
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .value(response -> {
                    assert response.get("mode").equals("oauth2");
                    assert response.get("internalEnabled").equals("false");
                    assert response.get("oauth2Enabled").equals("true");
                    assert response.get("openidEnabled").equals("false");
                });
    }

    @Test
    public void testGetAuthModeOpenID() {
        when(securityProperties.getMode()).thenReturn("openid");
        when(oauth2Properties.isEnabled()).thenReturn(false);
        when(openidProperties.isEnabled()).thenReturn(true);

        webTestClient.get()
                .uri("/authentication/mode")
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .value(response -> {
                    assert response.get("mode").equals("openid");
                    assert response.get("internalEnabled").equals("false");
                    assert response.get("oauth2Enabled").equals("false");
                    assert response.get("openidEnabled").equals("true");
                });
    }

    @Test
    public void testOAuth2TokenSuccess() {
        String email = "test@example.com";
        String accessToken = "oauth2-access-token";
        String jwtToken = "jwt-token-123";

        UserLogin userLogin = new UserLogin();
        userLogin.setEmail(email);
        userLogin.setPassword(accessToken);

        AuthUserInfo authUserInfo = new AuthUserInfo();
        authUserInfo.setEmail(email);
        authUserInfo.setUsername(email);

        UserPost userPost = new UserPost();
        userPost.setEmail(email);

        when(oauth2Service.validateAndGetUserInfo(accessToken)).thenReturn(Mono.just(authUserInfo));
        when(userHandler.findByEmail(email)).thenReturn(Mono.just(userPost));
        when(jwtUtil.generateToken(userPost)).thenReturn(jwtToken);

        webTestClient.post()
                .uri("/authentication/oauth2/token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(userLogin)
                .exchange()
                .expectStatus().isOk()
                .expectBody(AuthResponse.class)
                .value(response -> {
                    assert response.getToken().equals(jwtToken);
                });

        verify(oauth2Service, times(1)).validateAndGetUserInfo(accessToken);
        verify(userHandler, times(1)).findByEmail(email);
        verify(jwtUtil, times(1)).generateToken(userPost);
    }

    @Test
    public void testOAuth2TokenUserNotFound() {
        String email = "test@example.com";
        String accessToken = "oauth2-access-token";

        UserLogin userLogin = new UserLogin();
        userLogin.setEmail(email);
        userLogin.setPassword(accessToken);

        AuthUserInfo authUserInfo = new AuthUserInfo();
        authUserInfo.setEmail(email);
        authUserInfo.setUsername(email);

        when(oauth2Service.validateAndGetUserInfo(accessToken)).thenReturn(Mono.just(authUserInfo));
        when(userHandler.findByEmail(email)).thenReturn(Mono.empty());

        webTestClient.post()
                .uri("/authentication/oauth2/token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(userLogin)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody().isEmpty();

        verify(oauth2Service, times(1)).validateAndGetUserInfo(accessToken);
        verify(userHandler, times(1)).findByEmail(email);
        verify(jwtUtil, never()).generateToken(any());
    }

    @Test
    public void testOAuth2TokenFailure() {
        String accessToken = "invalid-token";

        UserLogin userLogin = new UserLogin();
        userLogin.setEmail("test@example.com");
        userLogin.setPassword(accessToken);

        when(oauth2Service.validateAndGetUserInfo(accessToken))
                .thenReturn(Mono.error(new RuntimeException("Invalid token")));

        webTestClient.post()
                .uri("/authentication/oauth2/token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(userLogin)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody().isEmpty();

        verify(oauth2Service, times(1)).validateAndGetUserInfo(accessToken);
        verify(userHandler, never()).findByEmail(anyString());
        verify(jwtUtil, never()).generateToken(any());
    }

    @Test
    public void testOpenIdTokenSuccess() {
        String email = "test@example.com";
        String accessToken = "openid-access-token";
        String jwtToken = "jwt-token-123";

        UserLogin userLogin = new UserLogin();
        userLogin.setEmail(email);
        userLogin.setPassword(accessToken);

        AuthUserInfo authUserInfo = new AuthUserInfo();
        authUserInfo.setEmail(email);
        authUserInfo.setUsername(email);

        UserPost userPost = new UserPost();
        userPost.setEmail(email);

        when(openidService.validateAndGetUserInfo(accessToken)).thenReturn(Mono.just(authUserInfo));
        when(userHandler.findByEmail(email)).thenReturn(Mono.just(userPost));
        when(jwtUtil.generateToken(userPost)).thenReturn(jwtToken);

        webTestClient.post()
                .uri("/authentication/openid/token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(userLogin)
                .exchange()
                .expectStatus().isOk()
                .expectBody(AuthResponse.class)
                .value(response -> {
                    assert response.getToken().equals(jwtToken);
                });

        verify(openidService, times(1)).validateAndGetUserInfo(accessToken);
        verify(userHandler, times(1)).findByEmail(email);
        verify(jwtUtil, times(1)).generateToken(userPost);
    }

    @Test
    public void testOpenIdTokenUserNotFound() {
        String email = "test@example.com";
        String accessToken = "openid-access-token";

        UserLogin userLogin = new UserLogin();
        userLogin.setEmail(email);
        userLogin.setPassword(accessToken);

        AuthUserInfo authUserInfo = new AuthUserInfo();
        authUserInfo.setEmail(email);
        authUserInfo.setUsername(email);

        when(openidService.validateAndGetUserInfo(accessToken)).thenReturn(Mono.just(authUserInfo));
        when(userHandler.findByEmail(email)).thenReturn(Mono.empty());

        webTestClient.post()
                .uri("/authentication/openid/token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(userLogin)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody().isEmpty();

        verify(openidService, times(1)).validateAndGetUserInfo(accessToken);
        verify(userHandler, times(1)).findByEmail(email);
        verify(jwtUtil, never()).generateToken(any());
    }

    @Test
    public void testOpenIdTokenFailure() {
        String accessToken = "invalid-token";

        UserLogin userLogin = new UserLogin();
        userLogin.setEmail("test@example.com");
        userLogin.setPassword(accessToken);

        when(openidService.validateAndGetUserInfo(accessToken))
                .thenReturn(Mono.error(new RuntimeException("Invalid token")));

        webTestClient.post()
                .uri("/authentication/openid/token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(userLogin)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody().isEmpty();

        verify(openidService, times(1)).validateAndGetUserInfo(accessToken);
        verify(userHandler, never()).findByEmail(anyString());
        verify(jwtUtil, never()).generateToken(any());
    }

    @Test
    public void testOAuth2Logout() {
        webTestClient.post()
                .uri("/authentication/oauth2/logout")
                .exchange()
                .expectStatus().isOk()
                .expectBody().isEmpty();
    }

    @Test
    public void testOpenIdLogout() {
        webTestClient.post()
                .uri("/authentication/openid/logout")
                .exchange()
                .expectStatus().isOk()
                .expectBody().isEmpty();
    }
}