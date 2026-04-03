package com.itexpert.content.core.endpoints;

import com.itexpert.content.core.handlers.AccessRoleHandler;
import com.itexpert.content.core.models.AccessRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AccessRoleEndPointTest {

    @Mock
    private AccessRoleHandler accessRoleHandlerMock;

    @InjectMocks
    private AccessRoleEndPoint accessRoleEndPoint;

    private WebTestClient webTestClient;

    @BeforeEach
    public void setUp() {
        webTestClient = WebTestClient.bindToController(accessRoleEndPoint).build();
    }

    @Test
    public void testFindAll() {
        Flux<AccessRole> rolesFlux = Flux.just(new AccessRole(), new AccessRole());
        when(accessRoleHandlerMock.findAll()).thenReturn(rolesFlux);

        webTestClient.get()
                .uri("/v0/access-roles/")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(AccessRole.class)
                .hasSize(2);

        verify(accessRoleHandlerMock, times(1)).findAll();
    }

    @Test
    public void testFindById() {
        AccessRole role = new AccessRole();
        UUID id = UUID.randomUUID();
        when(accessRoleHandlerMock.findById(id)).thenReturn(Mono.just(role));

        webTestClient.get()
                .uri("/v0/access-roles/id/{id}", id)
                .exchange()
                .expectStatus().isOk()
                .expectBody(AccessRole.class);

        verify(accessRoleHandlerMock, times(1)).findById(id);
    }

    @Test
    public void testFindByIdNotFound() {
        UUID id = UUID.randomUUID();
        when(accessRoleHandlerMock.findById(id)).thenReturn(Mono.empty());

        webTestClient.get()
                .uri("/v0/access-roles/id/{id}", id)
                .exchange()
                .expectStatus().isNotFound();

        verify(accessRoleHandlerMock, times(1)).findById(id);
    }

    @Test
    public void testSave() throws Exception {
        AccessRole role = new AccessRole();
        when(accessRoleHandlerMock.save(any(AccessRole.class))).thenReturn(Mono.just(role));

        webTestClient.post()
                .uri("/v0/access-roles/")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(role)
                .exchange()
                .expectStatus().isOk()
                .expectBody(AccessRole.class);

        verify(accessRoleHandlerMock, times(1)).save(any(AccessRole.class));
    }

    @Test
    public void testDelete() {
        UUID id = UUID.randomUUID();
        when(accessRoleHandlerMock.delete(id)).thenReturn(Mono.just(true));

        webTestClient.delete()
                .uri("/v0/access-roles/id/{id}", id)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Boolean.class)
                .isEqualTo(true);

        verify(accessRoleHandlerMock, times(1)).delete(id);
    }
}