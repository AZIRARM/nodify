
package com.itexpert.content.core.handlers;

import com.itexpert.content.core.helpers.ContentHelper;
import com.itexpert.content.core.helpers.ContentNodeSlugHelper;
import com.itexpert.content.core.mappers.ContentNodeMapper;
import com.itexpert.content.core.repositories.ContentNodeRepository;
import com.itexpert.content.core.repositories.NodeRepository;
import com.itexpert.content.lib.enums.ContentTypeEnum;
import com.itexpert.content.lib.enums.StatusEnum;
import com.itexpert.content.lib.models.ContentNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.mockito.Mockito;

class ContentNodeHandlerTest {

    private ContentNodeRepository contentNodeRepository;
    private ContentNodeMapper contentNodeMapper;
    private NodeRepository nodeRepository;
    private UserHandler userHandler;
    private NotificationHandler notificationHandler;
    private DataHandler dataHandler;
    private ContentHelper contentHelper;
    private ContentNodeSlugHelper contentNodeSlugHelper;
    private WebClient webClient;

    private ContentNodeHandler contentNodeHandler;

    @BeforeEach
    void setup() {

        contentNodeRepository = mock(ContentNodeRepository.class);
        contentNodeMapper = Mappers.getMapper(ContentNodeMapper.class);
        nodeRepository = mock(NodeRepository.class);
        userHandler = mock(UserHandler.class);
        notificationHandler = mock(NotificationHandler.class);
        dataHandler = mock(DataHandler.class);
        contentHelper = mock(ContentHelper.class);
        contentNodeSlugHelper = mock(ContentNodeSlugHelper.class);
        webClient = mock(WebClient.class);

        ContentNodeHandler realHandler = new ContentNodeHandler(
                contentNodeRepository,
                contentNodeMapper,
                nodeRepository,
                userHandler,
                notificationHandler,
                dataHandler,
                contentHelper,
                contentNodeSlugHelper,
                webClient
        );

        contentNodeHandler = Mockito.spy(realHandler);

        // Aucun snapshot existant par défaut
        when(contentNodeRepository.findByCodeAndStatus(
                nullable(String.class),
                eq(StatusEnum.SNAPSHOT.name())
        )).thenReturn(Mono.empty());

        // Simulation du save MongoDB
        when(contentNodeRepository.save(any()))
                .thenAnswer(invocation ->
                        Mono.just(invocation.getArgument(0))
                );

        // IMPORTANT :
        // On conserve le ContentNode dans le flux.
        // Mono.empty() provoquerait un onComplete sans onNext.
        doAnswer(invocation ->
                Mono.just(invocation.getArgument(0))
        ).when(contentNodeHandler).notify(any(), any());
    }

    @Test
    void save_shouldGenerateCodeWithParentPrefix() {

        ContentNode contentNode = new ContentNode();
        contentNode.setType(ContentTypeEnum.JSON);
        contentNode.setParentCode("PARENT-DEV-123");

        StepVerifier.create(contentNodeHandler.save(contentNode))
                .assertNext(saved -> {
                    assertNotNull(saved.getCode());
                    assertTrue(saved.getCode().startsWith("JSON-PARENT-"));

                    String[] parts = saved.getCode().split("-");
                    assertEquals("JSON", parts[0]);
                    assertEquals("PARENT", parts[1]);
                })
                .verifyComplete();
    }

    @Test
    void save_shouldGenerateCodeWithSelfPrefixWhenParentIsNull() {

        ContentNode contentNode = new ContentNode();
        contentNode.setType(ContentTypeEnum.PYTHON);
        contentNode.setCode("EXISTING-CODE-123");
        contentNode.setParentCode(null);

        StepVerifier.create(contentNodeHandler.save(contentNode))
                .assertNext(saved -> {
                    assertNotNull(saved.getCode());
                    assertTrue(saved.getCode().startsWith("PYTHON-EXISTING-"));

                    String[] parts = saved.getCode().split("-");
                    assertEquals("PYTHON", parts[0]);
                    assertEquals("EXISTING", parts[1]);
                })
                .verifyComplete();
    }

    @Test
    void save_shouldGenerateCodeWithDefaultPrefixWhenParentAndCodeAreEmpty() {

        ContentNode contentNode = new ContentNode();
        contentNode.setType(ContentTypeEnum.XML);
        contentNode.setParentCode("");
        contentNode.setCode(null);

        StepVerifier.create(contentNodeHandler.save(contentNode))
                .assertNext(saved -> {
                    assertNotNull(saved.getCode());
                    assertTrue(saved.getCode().startsWith("XML-CONTENT-"));

                    String[] parts = saved.getCode().split("-");
                    assertEquals("XML", parts[0]);
                    assertEquals("CONTENT", parts[1]);
                })
                .verifyComplete();
    }

    @Test
    void save_shouldAppendTimestampToCode() {

        long beforeSave = System.currentTimeMillis();

        ContentNode contentNode = new ContentNode();
        contentNode.setType(ContentTypeEnum.JSON);
        contentNode.setParentCode("PARENT-DEV");

        StepVerifier.create(contentNodeHandler.save(contentNode))
                .assertNext(saved -> {
                    String[] parts = saved.getCode().split("-");

                    assertEquals(3, parts.length);

                    long timestamp = Long.parseLong(parts[2]);

                    long afterSave = System.currentTimeMillis();

                    assertTrue(timestamp >= beforeSave);
                    assertTrue(timestamp <= afterSave);
                })
                .verifyComplete();
    }

    @Test
    void save_shouldSetCorrectMetadataOnCreation() {

        ContentNode contentNode = new ContentNode();
        contentNode.setType(ContentTypeEnum.HTML);
        contentNode.setParentCode("PARENT-DEV");

        StepVerifier.create(contentNodeHandler.save(contentNode))
                .assertNext(saved -> {
                    assertNotNull(saved.getId());
                    assertEquals("0", saved.getVersion());
                    assertEquals(StatusEnum.SNAPSHOT, saved.getStatus());

                    assertNotNull(saved.getCreationDate());
                    assertNotNull(saved.getModificationDate());

                    assertEquals(
                            saved.getCreationDate(),
                            saved.getModificationDate()
                    );
                })
                .verifyComplete();
    }

    @Test
    void save_shouldHandleParentCodeWithMultipleSegments() {

        ContentNode contentNode = new ContentNode();
        contentNode.setType(ContentTypeEnum.FILE);
        contentNode.setParentCode("MY-COMPLEX-PARENT-CODE-123");

        StepVerifier.create(contentNodeHandler.save(contentNode))
                .assertNext(saved -> {
                    String[] parts = saved.getCode().split("-");

                    assertEquals("FILE", parts[0]);
                    assertEquals("MY", parts[1]);

                    assertTrue(saved.getCode().startsWith("FILE-MY-"));
                })
                .verifyComplete();
    }

    @Test
    void save_shouldGenerateCodeForAllContentTypes() {

        for (ContentTypeEnum type : ContentTypeEnum.values()) {

            ContentNode contentNode = new ContentNode();
            contentNode.setType(type);
            contentNode.setParentCode("PARENT-DEV");

            StepVerifier.create(contentNodeHandler.save(contentNode))
                    .assertNext(saved -> {
                        assertNotNull(saved.getCode());

                        assertTrue(
                                saved.getCode().startsWith(
                                        type.name() + "-PARENT-"
                                ),
                                "Invalid generated code for type: " + type
                        );
                    })
                    .verifyComplete();
        }
    }

    @Test
    void save_shouldNotCreateWhenSnapshotAlreadyExists() {

        ContentNode contentNode = new ContentNode();
        contentNode.setType(ContentTypeEnum.JSON);
        contentNode.setCode("JSON-PARENT-123");

        when(contentNodeRepository.findByCodeAndStatus(
                eq("JSON-PARENT-123"),
                eq(StatusEnum.SNAPSHOT.name())
        )).thenReturn(
                Mono.just(contentNodeMapper.fromModel(contentNode))
        );

        StepVerifier.create(contentNodeHandler.save(contentNode))
                .verifyComplete();

        verify(contentNodeRepository, never()).save(any());
    }

    @Test
    void save_shouldUpdateWhenIdExists() {

        ContentNode contentNode = new ContentNode();
        contentNode.setId(UUID.randomUUID());
        contentNode.setType(ContentTypeEnum.JSON);
        contentNode.setCode("JSON-PARENT-123");

        StepVerifier.create(contentNodeHandler.save(contentNode))
                .assertNext(saved -> {
                    assertEquals(contentNode.getId(), saved.getId());
                    assertNotNull(saved.getModificationDate());
                })
                .verifyComplete();

        verify(contentNodeRepository, never())
                .findByCodeAndStatus(any(), any());
    }
}
