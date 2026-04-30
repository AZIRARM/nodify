package com.itexpert.content.core.mappers;

import com.itexpert.content.lib.models.ContentDisplay;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ContentDisplayMapperTest {

    private ContentDisplayMapper contentDisplayMapper;
    private UUID testId;
    private String testContentCode;
    private Long testDisplays;

    @BeforeEach
    void setUp() {
        // Initialisation manuelle du mapper (MapStruct génère l'implémentation)
        contentDisplayMapper = new ContentDisplayMapperImpl();

        testId = UUID.randomUUID();
        testContentCode = "ARTICLE_001";
        testDisplays = 250L;
    }

    @Test
    void fromEntity_ShouldMapAllFieldsCorrectly() {
        // Given
        com.itexpert.content.lib.entities.ContentDisplay entity = new com.itexpert.content.lib.entities.ContentDisplay();
        entity.setId(testId);
        entity.setContentCode(testContentCode);
        entity.setDisplays(testDisplays);

        // When
        ContentDisplay model = contentDisplayMapper.fromEntity(entity);

        // Then
        assertNotNull(model);
        assertEquals(testId, model.getId());
        assertEquals(testContentCode, model.getContentCode());
        assertEquals(testDisplays, model.getDisplays());
    }

    @Test
    void fromEntity_WithNullEntity_ShouldReturnNull() {
        // When
        ContentDisplay model = contentDisplayMapper.fromEntity(null);

        // Then
        assertNull(model);
    }

    @Test
    void fromEntity_WithPartialEntity_ShouldMapOnlyNonNullFields() {
        // Given
        com.itexpert.content.lib.entities.ContentDisplay entity = new com.itexpert.content.lib.entities.ContentDisplay();
        entity.setId(testId);
        entity.setContentCode(testContentCode);
        // displays est null

        // When
        ContentDisplay model = contentDisplayMapper.fromEntity(entity);

        // Then
        assertNotNull(model);
        assertEquals(testId, model.getId());
        assertEquals(testContentCode, model.getContentCode());
        assertNull(model.getDisplays());
    }

    @Test
    void fromEntity_WithZeroDisplays_ShouldMapCorrectly() {
        // Given
        com.itexpert.content.lib.entities.ContentDisplay entity = new com.itexpert.content.lib.entities.ContentDisplay();
        entity.setId(testId);
        entity.setContentCode(testContentCode);
        entity.setDisplays(0L);

        // When
        ContentDisplay model = contentDisplayMapper.fromEntity(entity);

        // Then
        assertNotNull(model);
        assertEquals(testId, model.getId());
        assertEquals(testContentCode, model.getContentCode());
        assertEquals(0L, model.getDisplays());
    }

    @Test
    void fromModel_ShouldMapAllFieldsCorrectly() {
        // Given
        ContentDisplay model = new ContentDisplay();
        model.setId(testId);
        model.setContentCode(testContentCode);
        model.setDisplays(testDisplays);

        // When
        com.itexpert.content.lib.entities.ContentDisplay entity = contentDisplayMapper.fromModel(model);

        // Then
        assertNotNull(entity);
        assertEquals(testId, entity.getId());
        assertEquals(testContentCode, entity.getContentCode());
        assertEquals(testDisplays, entity.getDisplays());
    }

    @Test
    void fromModel_WithNullModel_ShouldReturnNull() {
        // When
        com.itexpert.content.lib.entities.ContentDisplay entity = contentDisplayMapper.fromModel(null);

        // Then
        assertNull(entity);
    }

    @Test
    void fromModel_WithPartialModel_ShouldMapOnlyNonNullFields() {
        // Given
        ContentDisplay model = new ContentDisplay();
        model.setId(testId);
        model.setContentCode(testContentCode);
        // displays est null

        // When
        com.itexpert.content.lib.entities.ContentDisplay entity = contentDisplayMapper.fromModel(model);

        // Then
        assertNotNull(entity);
        assertEquals(testId, entity.getId());
        assertEquals(testContentCode, entity.getContentCode());
        assertNull(entity.getDisplays());
    }

    @Test
    void fromModel_WithZeroDisplays_ShouldMapCorrectly() {
        // Given
        ContentDisplay model = new ContentDisplay();
        model.setId(testId);
        model.setContentCode(testContentCode);
        model.setDisplays(0L);

        // When
        com.itexpert.content.lib.entities.ContentDisplay entity = contentDisplayMapper.fromModel(model);

        // Then
        assertNotNull(entity);
        assertEquals(testId, entity.getId());
        assertEquals(testContentCode, entity.getContentCode());
        assertEquals(0L, entity.getDisplays());
    }

    @Test
    void fromEntityAndBack_ShouldBeConsistent() {
        // Given
        com.itexpert.content.lib.entities.ContentDisplay originalEntity = new com.itexpert.content.lib.entities.ContentDisplay();
        originalEntity.setId(testId);
        originalEntity.setContentCode(testContentCode);
        originalEntity.setDisplays(testDisplays);

        // When
        ContentDisplay model = contentDisplayMapper.fromEntity(originalEntity);
        com.itexpert.content.lib.entities.ContentDisplay mappedBackEntity = contentDisplayMapper.fromModel(model);

        // Then
        assertNotNull(model);
        assertNotNull(mappedBackEntity);
        assertEquals(originalEntity.getId(), mappedBackEntity.getId());
        assertEquals(originalEntity.getContentCode(), mappedBackEntity.getContentCode());
        assertEquals(originalEntity.getDisplays(), mappedBackEntity.getDisplays());
    }

    @Test
    void fromModelAndBack_ShouldBeConsistent() {
        // Given
        ContentDisplay originalModel = new ContentDisplay();
        originalModel.setId(testId);
        originalModel.setContentCode(testContentCode);
        originalModel.setDisplays(testDisplays);

        // When
        com.itexpert.content.lib.entities.ContentDisplay entity = contentDisplayMapper.fromModel(originalModel);
        ContentDisplay mappedBackModel = contentDisplayMapper.fromEntity(entity);

        // Then
        assertNotNull(entity);
        assertNotNull(mappedBackModel);
        assertEquals(originalModel.getId(), mappedBackModel.getId());
        assertEquals(originalModel.getContentCode(), mappedBackModel.getContentCode());
        assertEquals(originalModel.getDisplays(), mappedBackModel.getDisplays());
    }

    @Test
    void fromEntity_ShouldHandleLargeDisplayNumbers() {
        // Given
        com.itexpert.content.lib.entities.ContentDisplay entity = new com.itexpert.content.lib.entities.ContentDisplay();
        entity.setId(testId);
        entity.setContentCode(testContentCode);
        entity.setDisplays(Long.MAX_VALUE);

        // When
        ContentDisplay model = contentDisplayMapper.fromEntity(entity);

        // Then
        assertNotNull(model);
        assertEquals(Long.MAX_VALUE, model.getDisplays());
    }

    @Test
    void fromModel_ShouldHandleLargeDisplayNumbers() {
        // Given
        ContentDisplay model = new ContentDisplay();
        model.setId(testId);
        model.setContentCode(testContentCode);
        model.setDisplays(Long.MAX_VALUE);

        // When
        com.itexpert.content.lib.entities.ContentDisplay entity = contentDisplayMapper.fromModel(model);

        // Then
        assertNotNull(entity);
        assertEquals(Long.MAX_VALUE, entity.getDisplays());
    }

    @Test
    void fromEntity_ShouldHandleNegativeDisplayNumbers() {
        // Given
        com.itexpert.content.lib.entities.ContentDisplay entity = new com.itexpert.content.lib.entities.ContentDisplay();
        entity.setId(testId);
        entity.setContentCode(testContentCode);
        entity.setDisplays(-100L);

        // When
        ContentDisplay model = contentDisplayMapper.fromEntity(entity);

        // Then
        assertNotNull(model);
        assertEquals(-100L, model.getDisplays());
    }

    @Test
    void fromModel_ShouldHandleNegativeDisplayNumbers() {
        // Given
        ContentDisplay model = new ContentDisplay();
        model.setId(testId);
        model.setContentCode(testContentCode);
        model.setDisplays(-100L);

        // When
        com.itexpert.content.lib.entities.ContentDisplay entity = contentDisplayMapper.fromModel(model);

        // Then
        assertNotNull(entity);
        assertEquals(-100L, entity.getDisplays());
    }

    @Test
    void fromEntity_ShouldHandleEmptyContentCode() {
        // Given
        com.itexpert.content.lib.entities.ContentDisplay entity = new com.itexpert.content.lib.entities.ContentDisplay();
        entity.setId(testId);
        entity.setContentCode("");
        entity.setDisplays(testDisplays);

        // When
        ContentDisplay model = contentDisplayMapper.fromEntity(entity);

        // Then
        assertNotNull(model);
        assertEquals("", model.getContentCode());
    }

    @Test
    void fromModel_ShouldHandleEmptyContentCode() {
        // Given
        ContentDisplay model = new ContentDisplay();
        model.setId(testId);
        model.setContentCode("");
        model.setDisplays(testDisplays);

        // When
        com.itexpert.content.lib.entities.ContentDisplay entity = contentDisplayMapper.fromModel(model);

        // Then
        assertNotNull(entity);
        assertEquals("", entity.getContentCode());
    }

    @Test
    void fromEntity_ShouldHandleNullContentCode() {
        // Given
        com.itexpert.content.lib.entities.ContentDisplay entity = new com.itexpert.content.lib.entities.ContentDisplay();
        entity.setId(testId);
        entity.setContentCode(null);
        entity.setDisplays(testDisplays);

        // When
        ContentDisplay model = contentDisplayMapper.fromEntity(entity);

        // Then
        assertNotNull(model);
        assertNull(model.getContentCode());
    }

    @Test
    void fromModel_ShouldHandleNullContentCode() {
        // Given
        ContentDisplay model = new ContentDisplay();
        model.setId(testId);
        model.setContentCode(null);
        model.setDisplays(testDisplays);

        // When
        com.itexpert.content.lib.entities.ContentDisplay entity = contentDisplayMapper.fromModel(model);

        // Then
        assertNotNull(entity);
        assertNull(entity.getContentCode());
    }

    @Test
    void fromEntity_ShouldHandleDifferentIdTypes() {
        // Given
        com.itexpert.content.lib.entities.ContentDisplay entity = new com.itexpert.content.lib.entities.ContentDisplay();
        UUID firstId = UUID.randomUUID();
        UUID secondId = UUID.randomUUID();

        entity.setId(firstId);
        entity.setContentCode("TEST");
        entity.setDisplays(10L);

        // When
        ContentDisplay model = contentDisplayMapper.fromEntity(entity);

        // Then
        assertNotNull(model);
        assertEquals(firstId, model.getId());
        assertNotEquals(secondId, model.getId());
    }

    @Test
    void fromModel_ShouldHandleDifferentIdTypes() {
        // Given
        ContentDisplay model = new ContentDisplay();
        UUID firstId = UUID.randomUUID();
        UUID secondId = UUID.randomUUID();

        model.setId(firstId);
        model.setContentCode("TEST");
        model.setDisplays(10L);

        // When
        com.itexpert.content.lib.entities.ContentDisplay entity = contentDisplayMapper.fromModel(model);

        // Then
        assertNotNull(entity);
        assertEquals(firstId, entity.getId());
        assertNotEquals(secondId, entity.getId());
    }

    @Test
    void fromEntity_ShouldHandleMultipleMappingsWithSameEntity() {
        // Given
        com.itexpert.content.lib.entities.ContentDisplay entity = new com.itexpert.content.lib.entities.ContentDisplay();
        entity.setId(testId);
        entity.setContentCode(testContentCode);
        entity.setDisplays(testDisplays);

        // When
        ContentDisplay model1 = contentDisplayMapper.fromEntity(entity);
        ContentDisplay model2 = contentDisplayMapper.fromEntity(entity);

        // Then
        assertNotNull(model1);
        assertNotNull(model2);
        assertEquals(model1.getId(), model2.getId());
        assertEquals(model1.getContentCode(), model2.getContentCode());
        assertEquals(model1.getDisplays(), model2.getDisplays());
    }
}