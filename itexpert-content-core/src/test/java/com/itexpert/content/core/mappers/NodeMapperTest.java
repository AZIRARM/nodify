package com.itexpert.content.core.mappers;

import com.itexpert.content.lib.enums.OperatorEnum;
import com.itexpert.content.lib.enums.StatusEnum;
import com.itexpert.content.lib.enums.TypeEnum;
import com.itexpert.content.lib.models.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class NodeMapperTest {

    private NodeMapper nodeMapper;
    private UUID testId;
    private String testCode;
    private String testName;
    private String testDescription;
    private String testSlug;
    private String testParentCode;
    private String testVersion;
    private Long testCreationDate;
    private Long testModificationDate;

    @BeforeEach
    void setUp() {
        nodeMapper = new NodeMapperImpl();

        testId = UUID.randomUUID();
        testCode = "HOME_NODE";
        testName = "Home Node";
        testDescription = "Main navigation node";
        testSlug = "home-node";
        testParentCode = "ROOT";
        testVersion = "1.0.0";
        testCreationDate = System.currentTimeMillis();
        testModificationDate = System.currentTimeMillis();
    }

    @Test
    void fromEntity_ShouldMapAllBasicFieldsCorrectly() {
        // Given
        com.itexpert.content.lib.entities.Node entity = new com.itexpert.content.lib.entities.Node();
        entity.setId(testId);
        entity.setCode(testCode);
        entity.setSlug(testSlug);
        entity.setParentCode(testParentCode);
        entity.setParentCodeOrigin("ROOT_ORIGIN");
        entity.setName(testName);
        entity.setDescription(testDescription);
        entity.setDefaultLanguage("fr");
        entity.setType("PAGE");
        entity.setVersion(testVersion);
        entity.setPublicationDate(testCreationDate);
        entity.setStatus(StatusEnum.PUBLISHED);
        entity.setFavorite(true);
        entity.setCreationDate(testCreationDate);
        entity.setModificationDate(testModificationDate);
        entity.setModifiedBy("admin@test.com");
        entity.setMaxVersionsToKeep(10);

        // When
        Node model = nodeMapper.fromEntity(entity);

        // Then
        assertNotNull(model);
        assertEquals(testId, model.getId());
        assertEquals(testCode, model.getCode());
        assertEquals(testSlug, model.getSlug());
        assertEquals(testParentCode, model.getParentCode());
        assertEquals("ROOT_ORIGIN", model.getParentCodeOrigin());
        assertEquals(testName, model.getName());
        assertEquals(testDescription, model.getDescription());
        assertEquals("fr", model.getDefaultLanguage());
        assertEquals("PAGE", model.getType());
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
    void fromEntity_ShouldMapSubNodesCorrectly() {
        // Given
        com.itexpert.content.lib.entities.Node entity = new com.itexpert.content.lib.entities.Node();
        entity.setId(testId);
        entity.setCode(testCode);

        List<String> subNodes = List.of("NODE_001", "NODE_002", "NODE_003");
        entity.setSubNodes(subNodes);

        // When
        Node model = nodeMapper.fromEntity(entity);

        // Then
        assertNotNull(model);
        assertNotNull(model.getSubNodes());
        assertEquals(3, model.getSubNodes().size());
        assertTrue(model.getSubNodes().contains("NODE_001"));
        assertTrue(model.getSubNodes().contains("NODE_002"));
        assertTrue(model.getSubNodes().contains("NODE_003"));
    }

    @Test
    void fromEntity_ShouldMapTagsCorrectly() {
        // Given
        com.itexpert.content.lib.entities.Node entity = new com.itexpert.content.lib.entities.Node();
        entity.setId(testId);
        entity.setCode(testCode);

        List<String> tags = List.of("important", "featured", "main");
        entity.setTags(tags);

        // When
        Node model = nodeMapper.fromEntity(entity);

        // Then
        assertNotNull(model);
        assertNotNull(model.getTags());
        assertEquals(3, model.getTags().size());
        assertTrue(model.getTags().contains("important"));
        assertTrue(model.getTags().contains("featured"));
        assertTrue(model.getTags().contains("main"));
    }

    @Test
    void fromEntity_ShouldMapValuesCorrectly() {
        // Given
        com.itexpert.content.lib.entities.Node entity = new com.itexpert.content.lib.entities.Node();
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
        Node model = nodeMapper.fromEntity(entity);

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
        com.itexpert.content.lib.entities.Node entity = new com.itexpert.content.lib.entities.Node();
        entity.setId(testId);
        entity.setCode(testCode);

        List<String> roles = List.of("ADMIN", "EDITOR", "READER");
        entity.setRoles(roles);

        // When
        Node model = nodeMapper.fromEntity(entity);

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
        com.itexpert.content.lib.entities.Node entity = new com.itexpert.content.lib.entities.Node();
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
        Node model = nodeMapper.fromEntity(entity);

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
    void fromEntity_ShouldMapLanguagesCorrectly() {
        // Given
        com.itexpert.content.lib.entities.Node entity = new com.itexpert.content.lib.entities.Node();
        entity.setId(testId);
        entity.setCode(testCode);

        List<String> languages = List.of("fr", "en", "es", "de");
        entity.setLanguages(languages);

        // When
        Node model = nodeMapper.fromEntity(entity);

        // Then
        assertNotNull(model);
        assertNotNull(model.getLanguages());
        assertEquals(4, model.getLanguages().size());
        assertTrue(model.getLanguages().contains("fr"));
        assertTrue(model.getLanguages().contains("en"));
        assertTrue(model.getLanguages().contains("es"));
        assertTrue(model.getLanguages().contains("de"));
    }

    @Test
    void fromEntity_ShouldMapTranslationsCorrectly() {
        // Given
        com.itexpert.content.lib.entities.Node entity = new com.itexpert.content.lib.entities.Node();
        entity.setId(testId);
        entity.setCode(testCode);

        List<Translation> translations = new ArrayList<>();
        Translation translation = new Translation();
        translation.setLanguage("en");
        translation.setKey("name");
        translation.setValue("Home");
        translations.add(translation);
        entity.setTranslations(translations);

        // When
        Node model = nodeMapper.fromEntity(entity);

        // Then
        assertNotNull(model);
        assertNotNull(model.getTranslations());
        assertEquals(1, model.getTranslations().size());
        assertEquals("en", model.getTranslations().get(0).getLanguage());
        assertEquals("name", model.getTranslations().get(0).getKey());
        assertEquals("Home", model.getTranslations().get(0).getValue());
    }

    @Test
    void fromEntity_WithNullEntity_ShouldReturnNull() {
        // When
        Node model = nodeMapper.fromEntity(null);

        // Then
        assertNull(model);
    }

    @Test
    void fromEntity_WithEmptyLists_ShouldMapEmptyLists() {
        // Given
        com.itexpert.content.lib.entities.Node entity = new com.itexpert.content.lib.entities.Node();
        entity.setId(testId);
        entity.setCode(testCode);
        entity.setSubNodes(new ArrayList<>());
        entity.setTags(new ArrayList<>());
        entity.setValues(new ArrayList<>());
        entity.setRoles(new ArrayList<>());
        entity.setRules(new ArrayList<>());
        entity.setLanguages(new ArrayList<>());
        entity.setTranslations(new ArrayList<>());

        // When
        Node model = nodeMapper.fromEntity(entity);

        // Then
        assertNotNull(model);
        assertNotNull(model.getSubNodes());
        assertTrue(model.getSubNodes().isEmpty());
        assertNotNull(model.getTags());
        assertTrue(model.getTags().isEmpty());
        assertNotNull(model.getValues());
        assertTrue(model.getValues().isEmpty());
        assertNotNull(model.getRoles());
        assertTrue(model.getRoles().isEmpty());
        assertNotNull(model.getRules());
        assertTrue(model.getRules().isEmpty());
        assertNotNull(model.getLanguages());
        assertTrue(model.getLanguages().isEmpty());
        assertNotNull(model.getTranslations());
        assertTrue(model.getTranslations().isEmpty());
    }

    @Test
    void fromModel_ShouldMapAllBasicFieldsCorrectly() {
        // Given
        Node model = new Node();
        model.setId(testId);
        model.setParentCode(testParentCode);
        model.setParentCodeOrigin("ROOT_ORIGIN");
        model.setName(testName);
        model.setCode(testCode);
        model.setSlug(testSlug);
        model.setEnvironmentCode("prod");
        model.setDescription(testDescription);
        model.setDefaultLanguage("fr");
        model.setType("PAGE");
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
        com.itexpert.content.lib.entities.Node entity = nodeMapper.fromModel(model);

        // Then
        assertNotNull(entity);
        assertEquals(testId, entity.getId());
        assertEquals(testParentCode, entity.getParentCode());
        assertEquals("ROOT_ORIGIN", entity.getParentCodeOrigin());
        assertEquals(testName, entity.getName());
        assertEquals(testCode, entity.getCode());
        assertEquals(testSlug, entity.getSlug());
        assertEquals(testDescription, entity.getDescription());
        assertEquals("fr", entity.getDefaultLanguage());
        assertEquals("PAGE", entity.getType());
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
    void fromModel_ShouldMapSubNodesCorrectly() {
        // Given
        Node model = new Node();
        model.setId(testId);
        model.setCode(testCode);

        List<String> subNodes = List.of("NODE_001", "NODE_002");
        model.setSubNodes(subNodes);

        // When
        com.itexpert.content.lib.entities.Node entity = nodeMapper.fromModel(model);

        // Then
        assertNotNull(entity);
        assertNotNull(entity.getSubNodes());
        assertEquals(2, entity.getSubNodes().size());
        assertTrue(entity.getSubNodes().contains("NODE_001"));
        assertTrue(entity.getSubNodes().contains("NODE_002"));
    }

    @Test
    void fromModel_ShouldMapTagsCorrectly() {
        // Given
        Node model = new Node();
        model.setId(testId);
        model.setCode(testCode);

        List<String> tags = List.of("tag1", "tag2");
        model.setTags(tags);

        // When
        com.itexpert.content.lib.entities.Node entity = nodeMapper.fromModel(model);

        // Then
        assertNotNull(entity);
        assertNotNull(entity.getTags());
        assertEquals(2, entity.getTags().size());
        assertTrue(entity.getTags().contains("tag1"));
        assertTrue(entity.getTags().contains("tag2"));
    }

    @Test
    void fromModel_ShouldMapValuesCorrectly() {
        // Given
        Node model = new Node();
        model.setId(testId);
        model.setCode(testCode);

        List<Value> values = new ArrayList<>();
        Value value = new Value();
        UUID valueId = UUID.randomUUID();
        value.setId(valueId);
        value.setKey("setting");
        value.setValue("enabled");
        values.add(value);
        model.setValues(values);

        // When
        com.itexpert.content.lib.entities.Node entity = nodeMapper.fromModel(model);

        // Then
        assertNotNull(entity);
        assertNotNull(entity.getValues());
        assertEquals(1, entity.getValues().size());
        assertEquals(valueId, entity.getValues().get(0).getId());
        assertEquals("setting", entity.getValues().get(0).getKey());
        assertEquals("enabled", entity.getValues().get(0).getValue());
    }

    @Test
    void fromModel_ShouldMapRolesCorrectly() {
        // Given
        Node model = new Node();
        model.setId(testId);
        model.setCode(testCode);

        List<String> roles = List.of("ADMIN", "USER");
        model.setRoles(roles);

        // When
        com.itexpert.content.lib.entities.Node entity = nodeMapper.fromModel(model);

        // Then
        assertNotNull(entity);
        assertNotNull(entity.getRoles());
        assertEquals(2, entity.getRoles().size());
        assertTrue(entity.getRoles().contains("ADMIN"));
        assertTrue(entity.getRoles().contains("USER"));
    }

    @Test
    void fromModel_ShouldMapRulesCorrectly() {
        // Given
        Node model = new Node();
        model.setId(testId);
        model.setCode(testCode);

        List<Rule> rules = new ArrayList<>();
        Rule rule = new Rule();
        rule.setType(TypeEnum.STRING);
        rule.setName("test_rule");
        rule.setValue("test_value");
        rule.setEditable(true);
        rule.setErasable(true);
        rule.setOperator(OperatorEnum.SUP);
        rule.setBehavior(false);
        rule.setEnable(true);
        rule.setDescription("Test rule");
        rules.add(rule);
        model.setRules(rules);

        // When
        com.itexpert.content.lib.entities.Node entity = nodeMapper.fromModel(model);

        // Then
        assertNotNull(entity);
        assertNotNull(entity.getRules());
        assertEquals(1, entity.getRules().size());
        assertEquals(TypeEnum.STRING, entity.getRules().get(0).getType());
        assertEquals("test_rule", entity.getRules().get(0).getName());
        assertEquals("test_value", entity.getRules().get(0).getValue());
        assertTrue(entity.getRules().get(0).isEditable());
        assertTrue(entity.getRules().get(0).isErasable());
        assertEquals(OperatorEnum.SUP, entity.getRules().get(0).getOperator());
        assertFalse(entity.getRules().get(0).getBehavior());
        assertTrue(entity.getRules().get(0).getEnable());
        assertEquals("Test rule", entity.getRules().get(0).getDescription());
    }

    @Test
    void fromModel_ShouldMapLanguagesCorrectly() {
        // Given
        Node model = new Node();
        model.setId(testId);
        model.setCode(testCode);

        List<String> languages = List.of("fr", "en");
        model.setLanguages(languages);

        // When
        com.itexpert.content.lib.entities.Node entity = nodeMapper.fromModel(model);

        // Then
        assertNotNull(entity);
        assertNotNull(entity.getLanguages());
        assertEquals(2, entity.getLanguages().size());
        assertTrue(entity.getLanguages().contains("fr"));
        assertTrue(entity.getLanguages().contains("en"));
    }

    @Test
    void fromModel_ShouldMapTranslationsCorrectly() {
        // Given
        Node model = new Node();
        model.setId(testId);
        model.setCode(testCode);

        List<Translation> translations = new ArrayList<>();
        Translation translation = new Translation();
        translation.setLanguage("es");
        translation.setKey("description");
        translation.setValue("Descripción");
        translations.add(translation);
        model.setTranslations(translations);

        // When
        com.itexpert.content.lib.entities.Node entity = nodeMapper.fromModel(model);

        // Then
        assertNotNull(entity);
        assertNotNull(entity.getTranslations());
        assertEquals(1, entity.getTranslations().size());
        assertEquals("es", entity.getTranslations().get(0).getLanguage());
        assertEquals("description", entity.getTranslations().get(0).getKey());
        assertEquals("Descripción", entity.getTranslations().get(0).getValue());
    }

    @Test
    void fromModel_WithNullModel_ShouldReturnNull() {
        // When
        com.itexpert.content.lib.entities.Node entity = nodeMapper.fromModel(null);

        // Then
        assertNull(entity);
    }

    @Test
    void fromEntityAndBack_ShouldBeConsistent() {
        // Given
        com.itexpert.content.lib.entities.Node originalEntity = new com.itexpert.content.lib.entities.Node();
        originalEntity.setId(testId);
        originalEntity.setParentCode(testParentCode);
        originalEntity.setCode(testCode);
        originalEntity.setSlug(testSlug);
        originalEntity.setName(testName);
        originalEntity.setDescription(testDescription);
        originalEntity.setDefaultLanguage("fr");
        originalEntity.setType("PAGE");
        originalEntity.setVersion(testVersion);
        originalEntity.setStatus(StatusEnum.PUBLISHED);
        originalEntity.setCreationDate(testCreationDate);

        List<String> tags = List.of("tag1", "tag2");
        originalEntity.setTags(tags);

        List<String> roles = List.of("ADMIN");
        originalEntity.setRoles(roles);

        List<String> languages = List.of("fr", "en");
        originalEntity.setLanguages(languages);

        // When
        Node model = nodeMapper.fromEntity(originalEntity);
        com.itexpert.content.lib.entities.Node mappedBackEntity = nodeMapper.fromModel(model);

        // Then
        assertNotNull(model);
        assertNotNull(mappedBackEntity);
        assertEquals(originalEntity.getId(), mappedBackEntity.getId());
        assertEquals(originalEntity.getParentCode(), mappedBackEntity.getParentCode());
        assertEquals(originalEntity.getCode(), mappedBackEntity.getCode());
        assertEquals(originalEntity.getSlug(), mappedBackEntity.getSlug());
        assertEquals(originalEntity.getName(), mappedBackEntity.getName());
        assertEquals(originalEntity.getDescription(), mappedBackEntity.getDescription());
        assertEquals(originalEntity.getDefaultLanguage(), mappedBackEntity.getDefaultLanguage());
        assertEquals(originalEntity.getType(), mappedBackEntity.getType());
        assertEquals(originalEntity.getVersion(), mappedBackEntity.getVersion());
        assertEquals(originalEntity.getStatus(), mappedBackEntity.getStatus());
        assertEquals(originalEntity.getTags(), mappedBackEntity.getTags());
        assertEquals(originalEntity.getRoles(), mappedBackEntity.getRoles());
        assertEquals(originalEntity.getLanguages(), mappedBackEntity.getLanguages());
    }

    @Test
    void fromEntity_ShouldHandleNullLists() {
        // Given
        com.itexpert.content.lib.entities.Node entity = new com.itexpert.content.lib.entities.Node();
        entity.setId(testId);
        entity.setCode(testCode);
        entity.setSubNodes(null);
        entity.setTags(null);
        entity.setValues(null);
        entity.setRoles(null);
        entity.setRules(null);
        entity.setLanguages(null);
        entity.setTranslations(null);

        // When
        Node model = nodeMapper.fromEntity(entity);

        // Then
        assertNotNull(model);
        assertNull(model.getSubNodes());
        assertNull(model.getTags());
        assertNull(model.getValues());
        assertNull(model.getRoles());
        assertNull(model.getRules());
        assertNull(model.getLanguages());
        assertNull(model.getTranslations());
    }

    @Test
    void fromEntity_ShouldHandleAllStatusEnum() {
        // Given
        for (StatusEnum status : StatusEnum.values()) {
            com.itexpert.content.lib.entities.Node entity = new com.itexpert.content.lib.entities.Node();
            entity.setId(testId);
            entity.setCode(testCode + "_" + status.name());
            entity.setStatus(status);

            // When
            Node model = nodeMapper.fromEntity(entity);

            // Then
            assertNotNull(model);
            assertEquals(status, model.getStatus());
        }
    }

    @Test
    void fromModel_ShouldHandleContentsList() {
        // Given
        Node model = new Node();
        model.setId(testId);
        model.setCode(testCode);

        List<ContentNode> contents = new ArrayList<>();
        ContentNode contentNode = new ContentNode();
        contentNode.setId(UUID.randomUUID());
        contentNode.setCode("CONTENT_001");
        contentNode.setTitle("Test Content");
        contents.add(contentNode);
        model.setContents(contents);

        // When
        com.itexpert.content.lib.entities.Node entity = nodeMapper.fromModel(model);

        // Then
        assertNotNull(entity);
        // Note: contents n'existe pas dans l'entité Node, donc ne sera pas mappé
        // Ce test vérifie simplement que le mapping ne plante pas
    }

    @Test
    void fromModel_ShouldHandleEnvironmentCode() {
        // Given
        Node model = new Node();
        model.setId(testId);
        model.setCode(testCode);
        model.setEnvironmentCode("staging");

        // When
        com.itexpert.content.lib.entities.Node entity = nodeMapper.fromModel(model);

        // Then
        assertNotNull(entity);
        // Note: environmentCode n'existe pas dans l'entité Node, donc ne sera pas mappé
        // Ce test vérifie simplement que le mapping ne plante pas
    }

    @Test
    void fromModel_ShouldHandlePublicationStatus() {
        // Given
        Node model = new Node();
        model.setId(testId);
        model.setCode(testCode);
        model.setPublicationStatus("DRAFT");

        // When
        com.itexpert.content.lib.entities.Node entity = nodeMapper.fromModel(model);

        // Then
        assertNotNull(entity);
        // Note: publicationStatus n'existe pas dans l'entité Node, donc ne sera pas
        // mappé
        // Ce test vérifie simplement que le mapping ne plante pas
    }
}