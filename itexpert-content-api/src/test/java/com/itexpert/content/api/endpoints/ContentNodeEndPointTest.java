package com.itexpert.content.api.endpoints;

import com.itexpert.content.api.handlers.ContentNodeHandler;
import com.itexpert.content.lib.enums.StatusEnum;
import com.itexpert.content.lib.models.ContentFile;
import com.itexpert.content.lib.models.ContentNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Base64;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContentNodeEndPointTest {

    @Mock
    private ContentNodeHandler contentNodeHandler;

    @InjectMocks
    private ContentNodeEndPoint contentNodeEndPoint;

    private ContentNode contentNode;
    private ContentFile contentFile;
    private String code;
    private String slug;
    private UUID id;

    @BeforeEach
    void setUp() {
        code = "test-code";
        slug = "test-slug";
        id = UUID.randomUUID();

        contentNode = new ContentNode();
        contentNode.setId(id);
        contentNode.setCode(code);
        contentNode.setSlug(slug);
        contentNode.setStatus(StatusEnum.PUBLISHED);
        contentNode.setPayload("test-payload");

        contentFile = new ContentFile();
        contentFile.setName("test-file.pdf");
        contentFile.setSize(1024);
        contentFile.setData(Base64.getEncoder().encodeToString("file content".getBytes()));
    }

    @Test
    void findAllByNodeCodeShouldReturnContentNodes() {
        when(contentNodeHandler.findAllByNodeCode(eq(code), eq(StatusEnum.PUBLISHED), eq(null), eq(false), eq(false)))
                .thenReturn(Flux.just(contentNode));

        StepVerifier.create(contentNodeEndPoint.findAllByNodeCode(code, StatusEnum.PUBLISHED, null, false, false, false))
                .expectNext(contentNode)
                .verifyComplete();
    }

    @Test
    void findAllByNodeCodeShouldReturnPayloadOnly() {
        when(contentNodeHandler.findAllByNodeCode(eq(code), eq(StatusEnum.PUBLISHED), eq(null), eq(false), eq(false)))
                .thenReturn(Flux.just(contentNode));

        StepVerifier.create(contentNodeEndPoint.findAllByNodeCode(code, StatusEnum.PUBLISHED, null, false, true, false))
                .expectNext(contentNode.getPayload())
                .verifyComplete();
    }

    @Test
    void findByCodeShouldReturnContentNode() {
        when(contentNodeHandler.findByCodeAndStatus(eq(code), eq(StatusEnum.PUBLISHED), eq(null), eq(false)))
                .thenReturn(Mono.just(contentNode));

        StepVerifier.create(contentNodeEndPoint.findByCode(code, StatusEnum.PUBLISHED, null, false, false))
                .expectNext(ResponseEntity.ok(contentNode))
                .verifyComplete();
    }

    @Test
    void findByCodeShouldReturnNotFound() {
        when(contentNodeHandler.findByCodeAndStatus(eq(code), eq(StatusEnum.PUBLISHED), eq(null), eq(false)))
                .thenReturn(Mono.empty());

        StepVerifier.create(contentNodeEndPoint.findByCode(code, StatusEnum.PUBLISHED, null, false, false))
                .expectNext(new ResponseEntity<>(HttpStatus.NOT_FOUND))
                .verifyComplete();
    }

    @Test
    void findByCodeShouldReturnPayloadOnly() {
        when(contentNodeHandler.findByCodeAndStatus(eq(code), eq(StatusEnum.PUBLISHED), eq(null), eq(false)))
                .thenReturn(Mono.just(contentNode));

        StepVerifier.create(contentNodeEndPoint.findByCode(code, StatusEnum.PUBLISHED, null, true, false))
                .expectNext(ResponseEntity.ok(contentNode.getPayload()))
                .verifyComplete();
    }

    @Test
    void findBySlugShouldReturnContentNode() {
        when(contentNodeHandler.findBySlugAndStatus(eq(slug), eq(StatusEnum.PUBLISHED), eq(null), eq(false)))
                .thenReturn(Mono.just(contentNode));

        StepVerifier.create(contentNodeEndPoint.findBySlug(slug, StatusEnum.PUBLISHED, null, false, false))
                .expectNext(ResponseEntity.ok(contentNode))
                .verifyComplete();
    }

    @Test
    void findBySlugShouldReturnNotFound() {
        when(contentNodeHandler.findBySlugAndStatus(eq(slug), eq(StatusEnum.PUBLISHED), eq(null), eq(false)))
                .thenReturn(Mono.empty());

        StepVerifier.create(contentNodeEndPoint.findBySlug(slug, StatusEnum.PUBLISHED, null, false, false))
                .expectNext(new ResponseEntity<>(HttpStatus.NOT_FOUND))
                .verifyComplete();
    }

    @Test
    void getContentAsFileDataFromCodeShouldReturnFile() {
        when(contentNodeHandler.findResourceByCode(eq(code), eq(StatusEnum.PUBLISHED)))
                .thenReturn(Mono.just(contentFile));

        StepVerifier.create(contentNodeEndPoint.getContentAsFileDataFromCode(code, StatusEnum.PUBLISHED))
                .expectNextMatches(response -> response.getStatusCode().is2xxSuccessful())
                .verifyComplete();
    }

    @Test
    void getContentAsFileDataFromCodeShouldReturnEmpty() {
        when(contentNodeHandler.findResourceByCode(eq(code), eq(StatusEnum.PUBLISHED)))
                .thenReturn(Mono.empty());

        StepVerifier.create(contentNodeEndPoint.getContentAsFileDataFromCode(code, StatusEnum.PUBLISHED))
                .verifyComplete();
    }

    @Test
    void getContentAsFileDataFromSlugShouldReturnFile() {
        when(contentNodeHandler.findResourceBySlug(eq(slug), eq(StatusEnum.PUBLISHED)))
                .thenReturn(Mono.just(contentFile));

        StepVerifier.create(contentNodeEndPoint.getContentAsFileDataFromSlug(slug, StatusEnum.PUBLISHED))
                .expectNextMatches(response -> response.getStatusCode().is2xxSuccessful())
                .verifyComplete();
    }

    @Test
    void getContentAsFileDataFromSlugShouldReturnEmpty() {
        when(contentNodeHandler.findResourceBySlug(eq(slug), eq(StatusEnum.PUBLISHED)))
                .thenReturn(Mono.empty());

        StepVerifier.create(contentNodeEndPoint.getContentAsFileDataFromSlug(slug, StatusEnum.PUBLISHED))
                .verifyComplete();
    }
}