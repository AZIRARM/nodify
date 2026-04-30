package com.itexpert.content.core.handlers.oauth2;

import com.itexpert.content.core.config.OAuth2Properties;
import com.itexpert.content.core.models.oauth.TokenResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class) // Jupiter extension pour Mockito
class OAuth2ServiceTest {

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private OAuth2Properties oauth2Properties;

    @Mock
    private WebClient webClient;

    // Mocks pour la syntaxe fluide de WebClient
    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;
    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;
    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;
    @Mock
    private WebClient.RequestBodySpec requestBodySpec;
    @Mock
    private WebClient.ResponseSpec responseSpec;

    @InjectMocks
    private OAuth2Service oauth2Service;

    private OAuth2Properties.OAuth2Config config;

    @BeforeEach
    void setUp() {
        config = new OAuth2Properties.OAuth2Config();
        config.setUserInfoUri("http://dummy-auth.com/user");
        config.setUserNameClaim("preferred_username");
        config.setRolesClaim("realm_access.roles");

        // Mock du Builder pour retourner notre WebClient mocké
        when(webClientBuilder.build()).thenReturn(webClient);
        when(oauth2Properties.getConfig()).thenReturn(config);
    }

    @Test
    void validateAndGetUserInfo_Success() {
        // GIVEN
        when(oauth2Properties.isEnabled()).thenReturn(true);
        Map<String, Object> mockResponse = Map.of(
                "preferred_username", "expert_user",
                "realm_access", Map.of("roles", List.of("ADMIN")));

        // Mock du chainage WebClient : get().uri().header().retrieve().bodyToMono()
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(ArgumentMatchers.<ParameterizedTypeReference<Map<String, Object>>>any()))
                .thenReturn(Mono.just(mockResponse));

        // WHEN & THEN
        StepVerifier.create(oauth2Service.validateAndGetUserInfo("token-123"))
                .assertNext(userInfo -> {
                    assertEquals("expert_user", userInfo.getUsername());
                    assertEquals("ADMIN", userInfo.getRoles().get(0));
                })
                .verifyComplete();
    }

    @Test
    void exchangeCodeForToken_Success() {
        // GIVEN
        TokenResponse mockToken = new TokenResponse();
        // Assure-toi que l'URL n'est pas nulle pour correspondre au stubbing
        config.setTokenUri("http://auth.com/token");
        config.setClientId("client-id");
        config.setClientSecret("secret");

        // Utilisation de any() pour éviter les problèmes de correspondance stricte
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec); // ou any()
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);

        // Pour le bodyValue, ton code concatène des chaînes, utilise anyString()
        when(requestBodySpec.bodyValue(anyString())).thenReturn(requestHeadersSpec);

        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(TokenResponse.class)).thenReturn(Mono.just(mockToken));

        // WHEN & THEN
        StepVerifier.create(oauth2Service.exchangeCodeForToken("code-456"))
                .expectNext(mockToken)
                .verifyComplete();
    }
}