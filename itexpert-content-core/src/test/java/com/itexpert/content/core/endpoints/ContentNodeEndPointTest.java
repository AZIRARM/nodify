package com.itexpert.content.core.endpoints;

import com.google.gson.Gson;
import com.itexpert.content.core.handlers.ContentNodeHandler;
import com.itexpert.content.core.handlers.NodeHandler;
import com.itexpert.content.core.helpers.RenameContentNodeCodesHelper;
import com.itexpert.content.lib.models.ContentNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ContentNodeEndPointTest {

        @Mock
        private ContentNodeHandler contentNodeHandler;

        @Mock
        private NodeHandler nodeHandler;

        @Mock
        private RenameContentNodeCodesHelper renameContentNodeCodesHelper;

        @InjectMocks
        private ContentNodeEndPoint contentNodeEndPoint;

        private WebTestClient webTestClient;
        private Gson gson;

        @BeforeEach
        public void setUp() {
                webTestClient = WebTestClient.bindToController(contentNodeEndPoint).build();
                gson = new Gson();
        }

        // ==================== TESTS SANS SECURITE ====================

        @Test
        public void testFindAll() {
                ContentNode node1 = new ContentNode();
                ContentNode node2 = new ContentNode();

                when(contentNodeHandler.findAll()).thenReturn(Flux.just(node1, node2));
                when(contentNodeHandler.setPublicationStatus(any(ContentNode.class)))
                                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

                webTestClient.get()
                                .uri("/v0/content-node/")
                                .exchange()
                                .expectStatus().isOk()
                                .expectBodyList(ContentNode.class)
                                .hasSize(2);
        }

        @Test
        public void testFindAllByStatus() {
                String status = "PUBLISHED";
                ContentNode node = new ContentNode();

                when(contentNodeHandler.findAllByStatus(status)).thenReturn(Flux.just(node));
                when(contentNodeHandler.setPublicationStatus(any(ContentNode.class)))
                                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

                webTestClient.get()
                                .uri("/v0/content-node/status/{status}", status)
                                .exchange()
                                .expectStatus().isOk()
                                .expectBodyList(ContentNode.class)
                                .hasSize(1);
        }

        @Test
        public void testFindAllByCode() {
                String code = "test-code";
                ContentNode node = new ContentNode();

                when(contentNodeHandler.findAllByCode(code)).thenReturn(Flux.just(node));
                when(contentNodeHandler.setPublicationStatus(any(ContentNode.class)))
                                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

                webTestClient.get()
                                .uri("/v0/content-node/code/{code}", code)
                                .exchange()
                                .expectStatus().isOk()
                                .expectBodyList(ContentNode.class)
                                .hasSize(1);
        }

        @Test
        public void testFindByNodeCodeAndStatus() {
                String code = "node-code";
                String status = "PUBLISHED";
                ContentNode node = new ContentNode();

                when(contentNodeHandler.findByNodeCodeAndStatus(code, status)).thenReturn(Flux.just(node));
                when(contentNodeHandler.setPublicationStatus(any(ContentNode.class)))
                                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

                webTestClient.get()
                                .uri("/v0/content-node/node/code/{code}/status/{status}", code, status)
                                .exchange()
                                .expectStatus().isOk()
                                .expectBodyList(ContentNode.class)
                                .hasSize(1);
        }

        @Test
        public void testFindAllByNodeCode() {
                String code = "node-code";
                ContentNode node = new ContentNode();

                when(contentNodeHandler.findAllByNodeCode(code)).thenReturn(Flux.just(node));
                when(contentNodeHandler.setPublicationStatus(any(ContentNode.class)))
                                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

                webTestClient.get()
                                .uri("/v0/content-node/node/code/{code}", code)
                                .exchange()
                                .expectStatus().isOk()
                                .expectBodyList(ContentNode.class)
                                .hasSize(1);
        }
}