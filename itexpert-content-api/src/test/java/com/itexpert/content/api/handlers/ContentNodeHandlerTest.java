package com.itexpert.content.api.handlers;

import com.itexpert.content.api.helpers.ContentHelper;
import com.itexpert.content.api.helpers.NodeHelper;
import com.itexpert.content.api.helpers.PluginHelper;
import com.itexpert.content.api.mappers.ContentNodeMapper;
import com.itexpert.content.api.repositories.ContentNodeRepository;
import com.itexpert.content.api.utils.RulesUtils;
import com.itexpert.content.lib.entities.ContentNode;
import com.itexpert.content.lib.enums.ContentTypeEnum;
import com.itexpert.content.lib.enums.StatusEnum;
import com.itexpert.content.lib.models.ContentDisplay;
import com.itexpert.content.lib.models.ContentFile;
import com.itexpert.content.lib.models.Node;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContentNodeHandlerTest {

    @Mock
    private ContentNodeRepository contentNodeRepository;

    @Mock
    private ContentNodeMapper contentNodeMapper;

    @Mock
    private ContentDisplayHandler contentDisplayHandler;

    @Mock
    private ContentHelper contentHelper;

    @Mock
    private PluginHelper pluginHelper;

    @Mock
    private NodeHelper nodeHelper;

    @InjectMocks
    private ContentNodeHandler contentNodeHandler;

    private MockedStatic<RulesUtils> rulesUtilsMock;

    private ContentNode entityContentNode;
    private ContentNode entityContentNodeWithoutFile;
    private ContentNode entityFileNode;

    private com.itexpert.content.lib.models.ContentNode modelContentNode;
    private com.itexpert.content.lib.models.ContentNode modelContentNodeWithoutFile;
    private com.itexpert.content.lib.models.ContentNode modelFileNode;

    private Node node;
    private ContentDisplay contentDisplay;
    private ContentFile contentFile;

    private String code;
    private String slug;
    private UUID id;

    @BeforeEach
    void setUp() {

        code = "test-code";
        slug = "test-slug";
        id = UUID.randomUUID();

        contentFile = new ContentFile();
        contentFile.setData("base64Data");

        entityContentNode = new ContentNode();
        entityContentNode.setId(id);
        entityContentNode.setCode(code);
        entityContentNode.setSlug(slug);
        entityContentNode.setStatus(StatusEnum.PUBLISHED);
        entityContentNode.setType(ContentTypeEnum.HTML);
        entityContentNode.setContent("<html><body>Test</body></html>");
        entityContentNode.setFile(contentFile);

        entityContentNodeWithoutFile = new ContentNode();
        entityContentNodeWithoutFile.setId(id);
        entityContentNodeWithoutFile.setCode(code);
        entityContentNodeWithoutFile.setSlug(slug);
        entityContentNodeWithoutFile.setStatus(StatusEnum.PUBLISHED);
        entityContentNodeWithoutFile.setType(ContentTypeEnum.HTML);
        entityContentNodeWithoutFile.setContent("<html><body>Test</body></html>");
        entityContentNodeWithoutFile.setFile(null);

        entityFileNode = new ContentNode();
        entityFileNode.setId(id);
        entityFileNode.setCode(code);
        entityFileNode.setSlug(slug);
        entityFileNode.setStatus(StatusEnum.PUBLISHED);
        entityFileNode.setType(ContentTypeEnum.FILE);
        entityFileNode.setFile(contentFile);

        modelContentNode = new com.itexpert.content.lib.models.ContentNode();
        modelContentNode.setId(id);
        modelContentNode.setCode(code);
        modelContentNode.setSlug(slug);
        modelContentNode.setStatus(StatusEnum.PUBLISHED);
        modelContentNode.setType(ContentTypeEnum.HTML);
        modelContentNode.setContent("<html><body>Test</body></html>");
        modelContentNode.setFile(contentFile);

        modelContentNodeWithoutFile = new com.itexpert.content.lib.models.ContentNode();
        modelContentNodeWithoutFile.setId(id);
        modelContentNodeWithoutFile.setCode(code);
        modelContentNodeWithoutFile.setSlug(slug);
        modelContentNodeWithoutFile.setStatus(StatusEnum.PUBLISHED);
        modelContentNodeWithoutFile.setType(ContentTypeEnum.HTML);
        modelContentNodeWithoutFile.setContent("<html><body>Test</body></html>");
        modelContentNodeWithoutFile.setFile(null);

        modelFileNode = new com.itexpert.content.lib.models.ContentNode();
        modelFileNode.setId(id);
        modelFileNode.setCode(code);
        modelFileNode.setSlug(slug);
        modelFileNode.setStatus(StatusEnum.PUBLISHED);
        modelFileNode.setType(ContentTypeEnum.FILE);
        modelFileNode.setFile(contentFile);

        node = new Node();
        node.setCode(code);
        node.setStatus(StatusEnum.PUBLISHED);

        contentDisplay = new ContentDisplay();
        contentDisplay.setContentCode(code);

        rulesUtilsMock = mockStatic(RulesUtils.class);

        rulesUtilsMock
                .when(() -> RulesUtils.evaluateContentNode(any(com.itexpert.content.lib.models.ContentNode.class)))
                .thenReturn(Mono.just(true));

        /*
         * Mocks génériques pour éviter les NPE Reactor
         */

        lenient().when(contentHelper.fillContents(any(ContentNode.class), any(), any()))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        lenient().when(pluginHelper.fillPlugin(any(ContentNode.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        lenient().when(contentNodeMapper.fromEntity(any(ContentNode.class)))
                .thenReturn(modelContentNode);

        lenient().when(contentNodeMapper.fromModel(any(com.itexpert.content.lib.models.ContentNode.class)))
                .thenReturn(entityContentNode);

        lenient().when(contentDisplayHandler.findByContentCode(any()))
                .thenReturn(Mono.just(contentDisplay));

        lenient().when(contentDisplayHandler.addDisplay(any()))
                .thenReturn(Mono.just(true));
    }

    @AfterEach
    void tearDown() {
        rulesUtilsMock.close();
    }

    @Test
    void findBySlugAndStatusShouldReturnEmptyWhenSlugNotFound() {

        when(contentNodeRepository.findBySlugAndStatus(slug, StatusEnum.PUBLISHED.name()))
                .thenReturn(Mono.empty());

        StepVerifier.create(
                        contentNodeHandler.findBySlugAndStatus(
                                slug,
                                StatusEnum.PUBLISHED,
                                "fr",
                                false
                        )
                )
                .verifyComplete();
    }

    @Test
    void findResourceByCodeShouldReturnEmptyWhenNodeNotFound() {

        when(contentNodeRepository.findByCodeAndStatus(code, StatusEnum.PUBLISHED.name()))
                .thenReturn(Mono.empty());

        StepVerifier.create(
                        contentNodeHandler.findResourceByCode(
                                code,
                                StatusEnum.PUBLISHED
                        )
                )
                .verifyComplete();
    }

    @Test
    void findResourceBySlugShouldReturnEmptyWhenSlugNotFound() {

        when(contentNodeRepository.findBySlugAndStatus(slug, StatusEnum.PUBLISHED.name()))
                .thenReturn(Mono.empty());

        StepVerifier.create(
                        contentNodeHandler.findResourceBySlug(
                                slug,
                                StatusEnum.PUBLISHED
                        )
                )
                .verifyComplete();
    }

    @Test
    void findAllByNodeCodeShouldReturnContentNodes() {

        when(nodeHelper.evaluateNode(code, StatusEnum.PUBLISHED))
                .thenReturn(Mono.just(node));

        when(contentNodeRepository.findAllByNodeCodeAndStatus(code, StatusEnum.PUBLISHED.name()))
                .thenReturn(Flux.just(entityContentNode));

        when(contentNodeRepository.findByCodeAndStatus(code, StatusEnum.PUBLISHED.name()))
                .thenReturn(Mono.just(entityContentNode));

        StepVerifier.create(
                        contentNodeHandler.findAllByNodeCode(
                                code,
                                StatusEnum.PUBLISHED,
                                "fr",
                                true,
                                true
                        )
                )
                .expectNextMatches(result -> result.getCode().equals(code))
                .verifyComplete();
    }

    @Test
    void findAllByNodeCodeShouldReturnEmptyWhenNodeEvaluationFails() {

        when(nodeHelper.evaluateNode(code, StatusEnum.PUBLISHED))
                .thenReturn(Mono.empty());

        StepVerifier.create(
                        contentNodeHandler.findAllByNodeCode(
                                code,
                                StatusEnum.PUBLISHED,
                                "fr",
                                true,
                                true
                        )
                )
                .verifyComplete();
    }

    @Test
    void findAllByNodeCodeShouldReturnEmptyWhenNoContentNodes() {

        when(nodeHelper.evaluateNode(code, StatusEnum.PUBLISHED))
                .thenReturn(Mono.just(node));

        when(contentNodeRepository.findAllByNodeCodeAndStatus(code, StatusEnum.PUBLISHED.name()))
                .thenReturn(Flux.empty());

        StepVerifier.create(
                        contentNodeHandler.findAllByNodeCode(
                                code,
                                StatusEnum.PUBLISHED,
                                "fr",
                                true,
                                true
                        )
                )
                .verifyComplete();
    }

    @Test
    void findByCodeAndStatusShouldReturnEmptyWhenNodeNotFound() {

        when(contentNodeRepository.findByCodeAndStatus(code, StatusEnum.PUBLISHED.name()))
                .thenReturn(Mono.empty());

        StepVerifier.create(
                        contentNodeHandler.findByCodeAndStatus(
                                code,
                                StatusEnum.PUBLISHED,
                                "fr",
                                false
                        )
                )
                .verifyComplete();
    }
}