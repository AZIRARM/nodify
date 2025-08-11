package com.itexpert.content.core.helpers;

import com.itexpert.content.core.mappers.ContentNodeMapper;
import com.itexpert.content.core.repositories.ContentNodeRepository;
import com.itexpert.content.core.repositories.NodeRepository;
import com.itexpert.content.core.utils.RulesUtils;
import com.itexpert.content.lib.entities.ContentNode;
import com.itexpert.content.lib.entities.Node;
import com.itexpert.content.lib.enums.StatusEnum;
import com.itexpert.content.lib.models.Translation;
import com.itexpert.content.lib.models.Value;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ContentHelperTest {

    private ContentNodeRepository contentNodeRepository;
    private NodeRepository nodeRepository;
    private ContentNodeMapper contentNodeMapper;
    private ContentHelper contentHelper;

    @BeforeEach
    void setup() {
        contentNodeRepository = mock(ContentNodeRepository.class);
        nodeRepository = mock(NodeRepository.class);
        contentNodeMapper = Mappers.getMapper(ContentNodeMapper.class);
        contentHelper = new ContentHelper(nodeRepository, contentNodeRepository, contentNodeMapper);
    }

    @Test
    void translate_shouldReplacePlaceholder_whenTranslationProvided() {
        ContentNode element = new ContentNode();
        element.setParentCode("PARENT");
        element.setContent("Hello $translate(greet)!");

        Node node = new Node();
        node.setParentCode(null);
        Translation tr = new Translation();
        tr.setLanguage("en");
        tr.setKey("greet");
        tr.setValue("World");
        node.setTranslations(List.of(tr));
        node.setDefaultLanguage("en");

        when(nodeRepository.findByCodeAndStatus("PARENT", StatusEnum.SNAPSHOT.name()))
                .thenReturn(Mono.just(node));

        StepVerifier.create(contentHelper.translate(element, "en", StatusEnum.SNAPSHOT))
                .expectNextMatches(result -> "Hello World!".equals(result.getContent()))
                .verifyComplete();
    }

    @Test
    void translate_shouldUseDefaultLanguage_whenTranslationNotProvided() {
        ContentNode element = new ContentNode();
        element.setParentCode("PARENT");
        element.setContent("Salut $translate(name)!");

        Node node = new Node();
        node.setParentCode(null);
        Translation tr = new Translation();
        tr.setLanguage("fr");
        tr.setKey("name");
        tr.setValue("Jean");
        node.setTranslations(List.of(tr));
        node.setDefaultLanguage("fr");

        when(nodeRepository.findByCodeAndStatus("PARENT", StatusEnum.SNAPSHOT.name()))
                .thenReturn(Mono.just(node));

        StepVerifier.create(contentHelper.translate(element, null, StatusEnum.SNAPSHOT))
                .expectNextMatches(result -> "Salut Jean!".equals(result.getContent()))
                .verifyComplete();
    }

    @Test
    void translate_shouldReturnOriginal_whenNoNodeFound() {
        ContentNode element = new ContentNode();
        element.setParentCode("UNKNOWN");
        element.setContent("Bonjour $translate(user)!");

        when(nodeRepository.findByCodeAndStatus("UNKNOWN", StatusEnum.SNAPSHOT.name()))
                .thenReturn(Mono.empty());

        StepVerifier.create(contentHelper.translate(element, "fr", StatusEnum.SNAPSHOT))
                .expectNextMatches(result -> "Bonjour $translate(user)!".equals(result.getContent()))
                .verifyComplete();
    }

    @Test
    void translate_shouldKeepPlaceholder_whenTranslationKeyNotFound() {
        ContentNode element = new ContentNode();
        element.setParentCode("PARENT");
        element.setContent("Hi $translate(unknown)!");

        Node node = new Node();
        node.setParentCode(null);
        Translation tr = new Translation();
        tr.setLanguage("en");
        tr.setKey("other");
        tr.setValue("Ignored");
        node.setTranslations(List.of(tr));
        node.setDefaultLanguage("en");

        when(nodeRepository.findByCodeAndStatus("PARENT", StatusEnum.SNAPSHOT.name()))
                .thenReturn(Mono.just(node));

        StepVerifier.create(contentHelper.translate(element, "en", StatusEnum.SNAPSHOT))
                .expectNextMatches(result -> "Hi $translate(unknown)!".equals(result.getContent()))
                .verifyComplete();
    }

    @Test
    void fillValues_shouldReplacePlaceholder_whenValueProvided() {
        ContentNode element = new ContentNode();
        element.setParentCode("PARENT");
        element.setContent("The price is $value(price)");

        Node node = new Node();
        node.setParentCode(null);
        Value val = new Value();
        val.setKey("price");
        val.setValue("100€");
        node.setValues(List.of(val));

        when(nodeRepository.findByCodeAndStatus("PARENT", StatusEnum.SNAPSHOT.name()))
                .thenReturn(Mono.just(node));

        StepVerifier.create(contentHelper.fillValues(element, StatusEnum.SNAPSHOT))
                .expectNextMatches(result -> "The price is 100€".equals(result.getContent()))
                .verifyComplete();
    }

    @Test
    void fillContents_shouldReplacePlaceholder_whenContentProvided() {
        ContentNode element = new ContentNode();
        element.setContent("Intro: $content(code123)");

        ContentNode entity = new ContentNode();
        entity.setContent("Child text");

        when(contentNodeRepository.findByCodeAndStatus("code123", StatusEnum.SNAPSHOT.name()))
                .thenReturn(Mono.just(entity));

        try (var mocked = Mockito.mockStatic(RulesUtils.class)) {
            mocked.when(() -> RulesUtils.evaluateContentNode(any()))
                    .thenReturn(Mono.just(true));

            StepVerifier.create(contentHelper.fillContents(element, StatusEnum.SNAPSHOT))
                    .expectNextMatches(result -> "Intro: Child text".equals(result.getContent()))
                    .verifyComplete();
        }
    }

    @Test
    void fillValues_shouldNotChangeContent_whenNoValues() {
        ContentNode element = new ContentNode();
        element.setParentCode("PARENT");
        element.setContent("No placeholders here");

        Node node = new Node();
        node.setParentCode(null);
        node.setValues(null);

        when(nodeRepository.findByCodeAndStatus("PARENT", StatusEnum.SNAPSHOT.name()))
                .thenReturn(Mono.just(node));

        StepVerifier.create(contentHelper.fillValues(element, StatusEnum.SNAPSHOT))
                .expectNextMatches(result -> "No placeholders here".equals(result.getContent()))
                .verifyComplete();
    }

    @Test
    void fillValues_shouldReplaceMultipleValues() {
        ContentNode element = new ContentNode();
        element.setParentCode("PARENT");
        element.setContent("Price: $value(price), Tax: $value(tax)");

        Node node = new Node();
        node.setParentCode(null);
        Value price = new Value();
        price.setKey("price");
        price.setValue("100€");
        Value tax = new Value();
        tax.setKey("tax");
        tax.setValue("20€");
        node.setValues(List.of(price, tax));

        when(nodeRepository.findByCodeAndStatus("PARENT", StatusEnum.SNAPSHOT.name()))
                .thenReturn(Mono.just(node));

        StepVerifier.create(contentHelper.fillValues(element, StatusEnum.SNAPSHOT))
                .expectNextMatches(result -> result.getContent().equals("Price: 100€, Tax: 20€"))
                .verifyComplete();
    }

    @Test
    void fillContents_shouldReturnElement_whenNoPlaceholder() {
        ContentNode element = new ContentNode();
        element.setContent("Plain content without placeholders");

        StepVerifier.create(contentHelper.fillContents(element, StatusEnum.SNAPSHOT))
                .expectNextMatches(result -> "Plain content without placeholders".equals(result.getContent()))
                .verifyComplete();
    }

    @Test
    void fillContents_shouldHandleEvaluateContentNodeFalse() {
        ContentNode element = new ContentNode();
        element.setContent("Start $content(code123)");

        com.itexpert.content.lib.models.ContentNode childContent = new com.itexpert.content.lib.models.ContentNode();
        childContent.setContent("Child text");

        when(contentNodeRepository.findByCodeAndStatus("code123", StatusEnum.SNAPSHOT.name()))
                .thenReturn(Mono.just(new ContentNode() {{
                    setCode("code123");
                    setContent("Child text");
                }}));

        try (MockedStatic<RulesUtils> mocked = Mockito.mockStatic(RulesUtils.class)) {
            mocked.when(() -> RulesUtils.evaluateContentNode(any()))
                    .thenReturn(Mono.just(false));

            StepVerifier.create(contentHelper.fillContents(element, StatusEnum.SNAPSHOT))
                    .expectNextMatches(result -> result.getContent().equals("Start $content(code123)"))
                    .verifyComplete();
        }
    }

    @Test
    void fillContents_shouldHandleMultipleNestedPlaceholders() {
        ContentNode rootEntity = new ContentNode();
        rootEntity.setContent("Intro $content(code1)");

        ContentNode entity1 = new ContentNode();
        entity1.setCode("code1");
        entity1.setContent("Part1 and $content(code2)");

        ContentNode entity2 = new ContentNode();
        entity2.setCode("code2");
        entity2.setContent("Part2");

        when(contentNodeRepository.findByCodeAndStatus("code1", StatusEnum.SNAPSHOT.name()))
                .thenReturn(Mono.just(entity1));

        when(contentNodeRepository.findByCodeAndStatus("code2", StatusEnum.SNAPSHOT.name()))
                .thenReturn(Mono.just(entity2));

        try (var mocked = mockStatic(RulesUtils.class)) {
            mocked.when(() -> RulesUtils.evaluateContentNode(any()))
                    .thenReturn(Mono.just(true));

            StepVerifier.create(contentHelper.fillContents(rootEntity, StatusEnum.SNAPSHOT))
                    .expectNextMatches(result -> "Intro Part1 and Part2".equals(result.getContent()))
                    .verifyComplete();
        }

        verify(contentNodeRepository).findByCodeAndStatus("code1", StatusEnum.SNAPSHOT.name());
        verify(contentNodeRepository).findByCodeAndStatus("code2", StatusEnum.SNAPSHOT.name());
    }

}
