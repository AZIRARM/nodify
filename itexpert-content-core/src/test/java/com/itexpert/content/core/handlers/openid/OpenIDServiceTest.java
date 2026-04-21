package com.itexpert.content.core.handlers.openid;

import com.itexpert.content.core.config.OpenIDProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
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

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpenIDServiceTest {

    @Mock
    private WebClient.Builder webClientBuilder;
    @Mock
    private OpenIDProperties openIDProperties;
    @Mock
    private WebClient webClient;
    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;
    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;
    @Mock
    private WebClient.ResponseSpec responseSpec;

    @InjectMocks
    private OpenIDService openIDService;

    private OpenIDProperties.OpenIDConfig config;
    private KeyPair keyPair;

    @BeforeEach
    void setUp() throws Exception {
        config = new OpenIDProperties.OpenIDConfig();
        config.setIssuerUri("http://auth-server");
        config.setUserNameClaim("sub");
        config.setRolesClaim("roles");

        // Utilisation de lenient() pour éviter UnnecessaryStubbingException
        lenient().when(openIDProperties.getConfig()).thenReturn(config);
        lenient().when(webClientBuilder.build()).thenReturn(webClient);

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        keyPair = kpg.generateKeyPair();
    }

    @Test
    void validateAndGetUserInfo_Success() {
        when(openIDProperties.isEnabled()).thenReturn(true);

        // 3. Création d'un token JWT valide signé avec notre clé privée
        String token = Jwts.builder()
                .setSubject("user123")
                .setIssuer("http://auth-server")
                .signWith(keyPair.getPrivate(), SignatureAlgorithm.RS256)
                .compact();

        // 4. Mock du Discovery Document
        Map<String, Object> discoveryDoc = Map.of("jwks_uri", "http://auth-server/jwks");
        setupWebClientMock(discoveryDoc);

        // 5. Mock du JWKS (Renvoi de la clé publique pour la validation)
        RSAPublicKey pub = (RSAPublicKey) keyPair.getPublic();
        Map<String, Object> jwks = Map.of("keys", List.of(Map.of(
                "n", Base64.getUrlEncoder().withoutPadding().encodeToString(pub.getModulus().toByteArray()),
                "e", Base64.getUrlEncoder().withoutPadding().encodeToString(pub.getPublicExponent().toByteArray()))));

        // On configure le WebClient pour renvoyer le JWKS après le Discovery
        when(responseSpec.bodyToMono(ArgumentMatchers.<ParameterizedTypeReference<Map<String, Object>>>any()))
                .thenReturn(Mono.just(discoveryDoc)) // Premier appel (Discovery)
                .thenReturn(Mono.just(jwks)); // Deuxième appel (JWKS)

        // 6. Exécution
        StepVerifier.create(openIDService.validateAndGetUserInfo(token))
                .assertNext(userInfo -> {
                    assertEquals("user123", userInfo.getUsername());
                    assertEquals("openid", userInfo.getAuthType());
                })
                .verifyComplete();
    }

    @Test
    void validateAndGetUserInfo_Disabled() {
        when(openIDProperties.isEnabled()).thenReturn(false);

        StepVerifier.create(openIDService.validateAndGetUserInfo("token"))
                .expectError(IllegalStateException.class)
                .verify();
    }

    /**
     * Utilitaire pour éviter de répéter le mock du WebClient
     */
    private void setupWebClientMock(Object response) {
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        // On ne définit pas le bodyToMono ici car il change selon le test
    }
}