package com.itexpert.content.core.mappers;

import com.itexpert.content.lib.enums.*;
import com.itexpert.content.lib.models.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ContentNodeMapperTest {

    private ContentNodeMapper contentNodeMapper;
    private UUID testId;
    private String testCode;
    private String testTitle;
    private String testDescription;
    private String testLanguage;
    private String testVersion;
    private Long testCreationDate;
    private Long testModificationDate;

    @BeforeEach
    void setUp() {
        contentNodeMapper = new ContentNodeMapperImpl();

        testId = UUID.randomUUID();
        testCode = "HOME_PAGE";
        testTitle = "Home Page";
        testDescription = "Main landing page";
        testLanguage = "fr";
        testVersion = "1.0.0";
        testCreationDate = System.currentTimeMillis();
        testModificationDate = System.currentTimeMillis();
    }

    @Test
    void fromEntity_ShouldMapAllBasicFieldsCorrectly() {
        // Given
        com.itexpert.content.lib.entities.ContentNode entity = new com.itexpert.content.lib.entities.ContentNode();
        entity.setId(testId);
        entity.setParentCode("PARENT_001");
        entity.setParentCodeOrigin("PARENT_ORIGIN_001");
        entity.setCode(testCode);
        entity.setSlug("home-page");
        entity.setLanguage(testLanguage);
        entity.setType(ContentTypeEnum.HTML);
        entity.setContent("<html>Content</html>");
        entity.setTitle(testTitle);
        entity.setDescription(testDescription);
        entity.setRedirectUrl("/redirect");
        entity.setIconUrl("/icon.png");
        entity.setPictureUrl("/picture.png");
        entity.setVersion(testVersion);
        entity.setPublicationDate(testCreationDate);
        entity.setStatus(StatusEnum.PUBLISHED);
        entity.setFavorite(true);
        entity.setCreationDate(testCreationDate);
        entity.setModificationDate(testModificationDate);
        entity.setModifiedBy("admin@test.com");
        entity.setMaxVersionsToKeep(10);

        // When
        ContentNode model = contentNodeMapper.fromEntity(entity);

        // Then
        assertNotNull(model);
        assertEquals(testId, model.getId());
        assertEquals("PARENT_001", model.getParentCode());
        assertEquals("PARENT_ORIGIN_001", model.getParentCodeOrigin());
        assertEquals(testCode, model.getCode());
        assertEquals("home-page", model.getSlug());
        assertEquals(testLanguage, model.getLanguage());
        assertEquals(ContentTypeEnum.HTML, model.getType());
        assertEquals("<html>Content</html>", model.getContent());
        assertEquals(testTitle, model.getTitle());
        assertEquals(testDescription, model.getDescription());
        assertEquals("/redirect", model.getRedirectUrl());
        assertEquals("/icon.png", model.getIconUrl());
        assertEquals("/picture.png", model.getPictureUrl());
        assertEquals(testVersion, model.getVersion());
        assertEquals(testCreationDate, model.getPublicationDate());
        assertEquals(StatusEnum.PUBLISHED, model.getStatus());
        assertTrue(model.isFavorite());
        assertEquals(testCreationDate, model.getCreationDate());
        assertEquals(testModificationDate, model.getModificationDate());
        assertEquals("admin@test.com", model.getModifiedBy());
        assertEquals(10, model.getMaxVersionsToKeep());
    }

    @Test
    void fromEntity_ShouldMapUrlsCorrectly() {
        // Given
        com.itexpert.content.lib.entities.ContentNode entity = new com.itexpert.content.lib.entities.ContentNode();
        entity.setId(testId);
        entity.setCode(testCode);

        List<ContentUrl> urls = new ArrayList<>();
        ContentUrl url = new ContentUrl();
        UUID urlId = UUID.randomUUID();
        url.setId(urlId);
        url.setUrl("https://example.com");
        url.setDescription("Example website");
        url.setType(UrlTypeEnum.PAGE);
        urls.add(url);
        entity.setUrls(urls);

        // When
        ContentNode model = contentNodeMapper.fromEntity(entity);

        // Then
        assertNotNull(model);
        assertNotNull(model.getUrls());
        assertEquals(1, model.getUrls().size());
        assertEquals(urlId, model.getUrls().get(0).getId());
        assertEquals("https://example.com", model.getUrls().get(0).getUrl());
        assertEquals("Example website", model.getUrls().get(0).getDescription());
        assertEquals(UrlTypeEnum.PAGE, model.getUrls().get(0).getType());
    }

    @Test
    void fromEntity_ShouldMapFileCorrectly() {
        // Given
        com.itexpert.content.lib.entities.ContentNode entity = new com.itexpert.content.lib.entities.ContentNode();
        entity.setId(testId);
        entity.setCode(testCode);

        ContentFile file = new ContentFile();
        file.setName("document.pdf");
        file.setType("application/pdf");
        file.setData("base64EncodedData");
        file.setSize(1024);
        entity.setFile(file);

        // When
        ContentNode model = contentNodeMapper.fromEntity(entity);

        // Then
        assertNotNull(model);
        assertNotNull(model.getFile());
        assertEquals("document.pdf", model.getFile().getName());
        assertEquals("application/pdf", model.getFile().getType());
        assertEquals("base64EncodedData", model.getFile().getData());
        assertEquals(1024, model.getFile().getSize());
    }

    @Test
    void fromEntity_ShouldMapTagsCorrectly() {
        // Given
        com.itexpert.content.lib.entities.ContentNode entity = new com.itexpert.content.lib.entities.ContentNode();
        entity.setId(testId);
        entity.setCode(testCode);

        List<String> tags = List.of("tag1", "tag2", "tag3");
        entity.setTags(tags);

        // When
        ContentNode model = contentNodeMapper.fromEntity(entity);

        // Then
        assertNotNull(model);
        assertNotNull(model.getTags());
        assertEquals(3, model.getTags().size());
        assertTrue(model.getTags().contains("tag1"));
        assertTrue(model.getTags().contains("tag2"));
        assertTrue(model.getTags().contains("tag3"));
    }

    @Test
    void fromEntity_ShouldMapValuesCorrectly() {
        // Given
        com.itexpert.content.lib.entities.ContentNode entity = new com.itexpert.content.lib.entities.ContentNode();
        entity.setId(testId);
        entity.setCode(testCode);

        List<Value> values = new ArrayList<>();
        Value value = new Value();
        UUID valueId = UUID.randomUUID();
        value.setId(valueId);
        value.setKey("theme");
        value.setValue("dark");
        values.add(value);
        entity.setValues(values);

        // When
        ContentNode model = contentNodeMapper.fromEntity(entity);

        // Then
        assertNotNull(model);
        assertNotNull(model.getValues());
        assertEquals(1, model.getValues().size());
        assertEquals(valueId, model.getValues().get(0).getId());
        assertEquals("theme", model.getValues().get(0).getKey());
        assertEquals("dark", model.getValues().get(0).getValue());
    }

    @Test
    void fromEntity_ShouldMapRolesCorrectly() {
        // Given
        com.itexpert.content.lib.entities.ContentNode entity = new com.itexpert.content.lib.entities.ContentNode();
        entity.setId(testId);
        entity.setCode(testCode);

        List<String> roles = List.of("ADMIN", "EDITOR", "READER");
        entity.setRoles(roles);

        // When
        ContentNode model = contentNodeMapper.fromEntity(entity);

        // Then
        assertNotNull(model);
        assertNotNull(model.getRoles());
        assertEquals(3, model.getRoles().size());
        assertTrue(model.getRoles().contains("ADMIN"));
        assertTrue(model.getRoles().contains("EDITOR"));
        assertTrue(model.getRoles().contains("READER"));
    }

    @Test
    void fromEntity_ShouldMapRulesCorrectly() {
        // Given
        com.itexpert.content.lib.entities.ContentNode entity = new com.itexpert.content.lib.entities.ContentNode();
        entity.setId(testId);
        entity.setCode(testCode);

        List<Rule> rules = new ArrayList<>();
        Rule rule = new Rule();
        rule.setType(TypeEnum.BOOL);
        rule.setName("age_check");
        rule.setValue("18");
        rule.setEditable(true);
        rule.setErasable(false);
        rule.setOperator(OperatorEnum.EQ);
        rule.setBehavior(true);
        rule.setEnable(true);
        rule.setDescription("Age verification rule");
        rules.add(rule);
        entity.setRules(rules);

        // When
        ContentNode model = contentNodeMapper.fromEntity(entity);

        // Then
        assertNotNull(model);
        assertNotNull(model.getRules());
        assertEquals(1, model.getRules().size());
        assertEquals(TypeEnum.BOOL, model.getRules().get(0).getType());
        assertEquals("age_check", model.getRules().get(0).getName());
        assertEquals("18", model.getRules().get(0).getValue());
        assertTrue(model.getRules().get(0).isEditable());
        assertFalse(model.getRules().get(0).isErasable());
        assertEquals(OperatorEnum.EQ, model.getRules().get(0).getOperator());
        assertTrue(model.getRules().get(0).getBehavior());
        assertTrue(model.getRules().get(0).getEnable());
        assertEquals("Age verification rule", model.getRules().get(0).getDescription());
    }

    @Test
    void fromEntity_ShouldMapTranslationsCorrectly() {
        // Given
        com.itexpert.content.lib.entities.ContentNode entity = new com.itexpert.content.lib.entities.ContentNode();
        entity.setId(testId);
        entity.setCode(testCode);

        List<Translation> translations = new ArrayList<>();
        Translation translation = new Translation();
        translation.setLanguage("en");
        translation.setKey("title");
        translation.setValue("Home Page");
        translations.add(translation);
        entity.setTranslations(translations);

        // When
        ContentNode model = contentNodeMapper.fromEntity(entity);

        // Then
        assertNotNull(model);
        assertNotNull(model.getTranslations());
        assertEquals(1, model.getTranslations().size());
        assertEquals("en", model.getTranslations().get(0).getLanguage());
        assertEquals("title", model.getTranslations().get(0).getKey());
        assertEquals("Home Page", model.getTranslations().get(0).getValue());
    }

    @Test
    void fromEntity_WithNullEntity_ShouldReturnNull() {
        // When
        ContentNode model = contentNodeMapper.fromEntity(null);

        // Then
        assertNull(model);
    }

    @Test
    void fromEntity_WithEmptyLists_ShouldMapEmptyLists() {
        // Given
        com.itexpert.content.lib.entities.ContentNode entity = new com.itexpert.content.lib.entities.ContentNode();
        entity.setId(testId);
        entity.setCode(testCode);
        entity.setUrls(new ArrayList<>());
        entity.setTags(new ArrayList<>());
        entity.setValues(new ArrayList<>());
        entity.setRoles(new ArrayList<>());
        entity.setRules(new ArrayList<>());
        entity.setTranslations(new ArrayList<>());

        // When
        ContentNode model = contentNodeMapper.fromEntity(entity);

        // Then
        assertNotNull(model);
        assertNotNull(model.getUrls());
        assertTrue(model.getUrls().isEmpty());
        assertNotNull(model.getTags());
        assertTrue(model.getTags().isEmpty());
        assertNotNull(model.getValues());
        assertTrue(model.getValues().isEmpty());
        assertNotNull(model.getRoles());
        assertTrue(model.getRoles().isEmpty());
        assertNotNull(model.getRules());
        assertTrue(model.getRules().isEmpty());
        assertNotNull(model.getTranslations());
        assertTrue(model.getTranslations().isEmpty());
    }

    @Test
    void fromModel_ShouldMapAllBasicFieldsCorrectly() {
        // Given
        ContentNode model = new ContentNode();
        model.setId(testId);
        model.setParentCode("PARENT_001");
        model.setParentCodeOrigin("PARENT_ORIGIN_001");
        model.setCode(testCode);
        model.setSlug("home-page");
        model.setEnvironmentCode("prod");
        model.setLanguage(testLanguage);
        model.setType(ContentTypeEnum.HTML);
        model.setContent("<html>Content</html>");
        model.setTitle(testTitle);
        model.setDescription(testDescription);
        model.setRedirectUrl("/redirect");
        model.setIconUrl("/icon.png");
        model.setPictureUrl("/picture.png");
        model.setVersion(testVersion);
        model.setPublicationDate(testCreationDate);
        model.setStatus(StatusEnum.PUBLISHED);
        model.setFavorite(true);
        model.setCreationDate(testCreationDate);
        model.setModificationDate(testModificationDate);
        model.setModifiedBy("admin@test.com");
        model.setMaxVersionsToKeep(10);
        model.setPublicationStatus("PUBLISHED");

        // When
        com.itexpert.content.lib.entities.ContentNode entity = contentNodeMapper.fromModel(model);

        // Then
        assertNotNull(entity);
        assertEquals(testId, entity.getId());
        assertEquals("PARENT_001", entity.getParentCode());
        assertEquals("PARENT_ORIGIN_001", entity.getParentCodeOrigin());
        assertEquals(testCode, entity.getCode());
        assertEquals("home-page", entity.getSlug());
        assertEquals(testLanguage, entity.getLanguage());
        assertEquals(ContentTypeEnum.HTML, entity.getType());
        assertEquals("<html>Content</html>", entity.getContent());
        assertEquals(testTitle, entity.getTitle());
        assertEquals(testDescription, entity.getDescription());
        assertEquals("/redirect", entity.getRedirectUrl());
        assertEquals("/icon.png", entity.getIconUrl());
        assertEquals("/picture.png", entity.getPictureUrl());
        assertEquals(testVersion, entity.getVersion());
        assertEquals(testCreationDate, entity.getPublicationDate());
        assertEquals(StatusEnum.PUBLISHED, entity.getStatus());
        assertTrue(entity.isFavorite());
        assertEquals(testCreationDate, entity.getCreationDate());
        assertEquals(testModificationDate, entity.getModificationDate());
        assertEquals("admin@test.com", entity.getModifiedBy());
        assertEquals(10, entity.getMaxVersionsToKeep());
    }

    @Test
    void fromModel_WithNullModel_ShouldReturnNull() {
        // When
        com.itexpert.content.lib.entities.ContentNode entity = contentNodeMapper.fromModel(null);

        // Then
        assertNull(entity);
    }

    @Test
    void fromEntityAndBack_ShouldBeConsistent() {
        // Given
        com.itexpert.content.lib.entities.ContentNode originalEntity = new com.itexpert.content.lib.entities.ContentNode();
        originalEntity.setId(testId);
        originalEntity.setParentCode("PARENT_001");
        originalEntity.setCode(testCode);
        originalEntity.setSlug("home-page");
        originalEntity.setLanguage(testLanguage);
        originalEntity.setType(ContentTypeEnum.HTML);
        originalEntity.setTitle(testTitle);
        originalEntity.setDescription(testDescription);
        originalEntity.setVersion(testVersion);
        originalEntity.setStatus(StatusEnum.PUBLISHED);
        originalEntity.setCreationDate(testCreationDate);

        List<String> tags = List.of("tag1", "tag2");
        originalEntity.setTags(tags);

        List<String> roles = List.of("ADMIN");
        originalEntity.setRoles(roles);

        List<Value> values = new ArrayList<>();
        Value value = new Value();
        value.setKey("test");
        value.setValue("ok");
        values.add(value);
        originalEntity.setValues(values);

        // When
        ContentNode model = contentNodeMapper.fromEntity(originalEntity);
        com.itexpert.content.lib.entities.ContentNode mappedBackEntity = contentNodeMapper.fromModel(model);

        // Then
        assertNotNull(model);
        assertNotNull(mappedBackEntity);
        assertEquals(originalEntity.getId(), mappedBackEntity.getId());
        assertEquals(originalEntity.getParentCode(), mappedBackEntity.getParentCode());
        assertEquals(originalEntity.getCode(), mappedBackEntity.getCode());
        assertEquals(originalEntity.getSlug(), mappedBackEntity.getSlug());
        assertEquals(originalEntity.getLanguage(), mappedBackEntity.getLanguage());
        assertEquals(originalEntity.getType(), mappedBackEntity.getType());
        assertEquals(originalEntity.getTitle(), mappedBackEntity.getTitle());
        assertEquals(originalEntity.getDescription(), mappedBackEntity.getDescription());
        assertEquals(originalEntity.getVersion(), mappedBackEntity.getVersion());
        assertEquals(originalEntity.getStatus(), mappedBackEntity.getStatus());
        assertEquals(originalEntity.getTags(), mappedBackEntity.getTags());
        assertEquals(originalEntity.getRoles(), mappedBackEntity.getRoles());
        assertEquals(originalEntity.getValues().size(), mappedBackEntity.getValues().size());
    }

    @Test
    void fromEntity_ShouldHandleNullLists() {
        // Given
        com.itexpert.content.lib.entities.ContentNode entity = new com.itexpert.content.lib.entities.ContentNode();
        entity.setId(testId);
        entity.setCode(testCode);
        entity.setUrls(null);
        entity.setTags(null);
        entity.setValues(null);
        entity.setRoles(null);
        entity.setRules(null);
        entity.setTranslations(null);

        // When
        ContentNode model = contentNodeMapper.fromEntity(entity);

        // Then
        assertNotNull(model);
        assertNull(model.getUrls());
        assertNull(model.getTags());
        assertNull(model.getValues());
        assertNull(model.getRoles());
        assertNull(model.getRules());
        assertNull(model.getTranslations());
    }

    @Test
    void fromEntity_ShouldHandleAllContentTypes() {
        // Given
        for (ContentTypeEnum contentType : ContentTypeEnum.values()) {
            com.itexpert.content.lib.entities.ContentNode entity = new com.itexpert.content.lib.entities.ContentNode();
            entity.setId(testId);
            entity.setCode(testCode + "_" + contentType.name());
            entity.setType(contentType);

            // When
            ContentNode model = contentNodeMapper.fromEntity(entity);

            // Then
            assertNotNull(model);
            assertEquals(contentType, model.getType());
        }
    }

    @Test
    void fromEntity_ShouldHandleAllStatusEnum() {
        // Given
        for (StatusEnum status : StatusEnum.values()) {
            com.itexpert.content.lib.entities.ContentNode entity = new com.itexpert.content.lib.entities.ContentNode();
            entity.setId(testId);
            entity.setCode(testCode + "_" + status.name());
            entity.setStatus(status);

            // When
            ContentNode model = contentNodeMapper.fromEntity(entity);

            // Then
            assertNotNull(model);
            assertEquals(status, model.getStatus());
        }
    }

    @Test
    void fromEntity_ShouldMapRuleWithAllOperatorEnum() {
        // Given
        com.itexpert.content.lib.entities.ContentNode entity = new com.itexpert.content.lib.entities.ContentNode();
        entity.setId(testId);
        entity.setCode(testCode);

        List<Rule> rules = new ArrayList<>();
        for (OperatorEnum operator : OperatorEnum.values()) {
            Rule rule = new Rule();
            rule.setOperator(operator);
            rule.setName("rule_" + operator.name());
            rules.add(rule);
        }
        entity.setRules(rules);

        // When
        ContentNode model = contentNodeMapper.fromEntity(entity);

        // Then
        assertNotNull(model);
        assertNotNull(model.getRules());
        assertEquals(OperatorEnum.values().length, model.getRules().size());
        for (int i = 0; i < OperatorEnum.values().length; i++) {
            assertEquals(OperatorEnum.values()[i], model.getRules().get(i).getOperator());
        }
    }

    @Test
    void fromEntity_ShouldMapRuleWithAllTypeEnum() {
        // Given
        com.itexpert.content.lib.entities.ContentNode entity = new com.itexpert.content.lib.entities.ContentNode();
        entity.setId(testId);
        entity.setCode(testCode);

        List<Rule> rules = new ArrayList<>();
        for (TypeEnum type : TypeEnum.values()) {
            Rule rule = new Rule();
            rule.setType(type);
            rule.setName("rule_" + type.name());
            rules.add(rule);
        }
        entity.setRules(rules);

        // When
        ContentNode model = contentNodeMapper.fromEntity(entity);

        // Then
        assertNotNull(model);
        assertNotNull(model.getRules());
        assertEquals(TypeEnum.values().length, model.getRules().size());
        for (int i = 0; i < TypeEnum.values().length; i++) {
            assertEquals(TypeEnum.values()[i], model.getRules().get(i).getType());
        }
    }

    @Test
    void fromEntity_ShouldMapUrlsWithAllUrlTypes() {
        // Given
        com.itexpert.content.lib.entities.ContentNode entity = new com.itexpert.content.lib.entities.ContentNode();
        entity.setId(testId);
        entity.setCode(testCode);

        List<ContentUrl> urls = new ArrayList<>();
        for (UrlTypeEnum urlType : UrlTypeEnum.values()) {
            ContentUrl url = new ContentUrl();
            url.setId(UUID.randomUUID());
            url.setUrl("https://example.com/" + urlType.name().toLowerCase());
            url.setDescription(urlType.name() + " URL");
            url.setType(urlType);
            urls.add(url);
        }
        entity.setUrls(urls);

        // When
        ContentNode model = contentNodeMapper.fromEntity(entity);

        // Then
        assertNotNull(model);
        assertNotNull(model.getUrls());
        assertEquals(UrlTypeEnum.values().length, model.getUrls().size());
        for (int i = 0; i < UrlTypeEnum.values().length; i++) {
            assertEquals(UrlTypeEnum.values()[i], model.getUrls().get(i).getType());
        }
    }
}