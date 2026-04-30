package com.itexpert.content.core.mappers;

import com.itexpert.content.lib.models.ContentClick;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ContentClickMapperTest {

    private ContentClickMapper contentClickMapper;
    private UUID testId;
    private String testContentCode;
    private Long testClicks;

    @BeforeEach
    void setUp() {
        // Initialisation manuelle du mapper (MapStruct génère l'implémentation)
        contentClickMapper = new ContentClickMapperImpl();

        testId = UUID.randomUUID();
        testContentCode = "ARTICLE_001";
        testClicks = 150L;
    }

    @Test
    void fromEntity_ShouldMapAllFieldsCorrectly() {
        // Given
        com.itexpert.content.lib.entities.ContentClick entity = new com.itexpert.content.lib.entities.ContentClick();
        entity.setId(testId);
        entity.setContentCode(testContentCode);
        entity.setClicks(testClicks);

        // When
        ContentClick model = contentClickMapper.fromEntity(entity);

        // Then
        assertNotNull(model);
        assertEquals(testId, model.getId());
        assertEquals(testContentCode, model.getContentCode());
        assertEquals(testClicks, model.getClicks());
    }

    @Test
    void fromEntity_WithNullEntity_ShouldReturnNull() {
        // When
        ContentClick model = contentClickMapper.fromEntity(null);

        // Then
        assertNull(model);
    }

    @Test
    void fromEntity_WithPartialEntity_ShouldMapOnlyNonNullFields() {
        // Given
        com.itexpert.content.lib.entities.ContentClick entity = new com.itexpert.content.lib.entities.ContentClick();
        entity.setId(testId);
        entity.setContentCode(testContentCode);
        // clicks est null

        // When
        ContentClick model = contentClickMapper.fromEntity(entity);

        // Then
        assertNotNull(model);
        assertEquals(testId, model.getId());
        assertEquals(testContentCode, model.getContentCode());
        assertNull(model.getClicks());
    }

    @Test
    void fromEntity_WithZeroClicks_ShouldMapCorrectly() {
        // Given
        com.itexpert.content.lib.entities.ContentClick entity = new com.itexpert.content.lib.entities.ContentClick();
        entity.setId(testId);
        entity.setContentCode(testContentCode);
        entity.setClicks(0L);

        // When
        ContentClick model = contentClickMapper.fromEntity(entity);

        // Then
        assertNotNull(model);
        assertEquals(testId, model.getId());
        assertEquals(testContentCode, model.getContentCode());
        assertEquals(0L, model.getClicks());
    }

    @Test
    void fromModel_ShouldMapAllFieldsCorrectly() {
        // Given
        ContentClick model = new ContentClick();
        model.setId(testId);
        model.setContentCode(testContentCode);
        model.setClicks(testClicks);

        // When
        com.itexpert.content.lib.entities.ContentClick entity = contentClickMapper.fromModel(model);

        // Then
        assertNotNull(entity);
        assertEquals(testId, entity.getId());
        assertEquals(testContentCode, entity.getContentCode());
        assertEquals(testClicks, entity.getClicks());
    }

    @Test
    void fromModel_WithNullModel_ShouldReturnNull() {
        // When
        com.itexpert.content.lib.entities.ContentClick entity = contentClickMapper.fromModel(null);

        // Then
        assertNull(entity);
    }

    @Test
    void fromModel_WithPartialModel_ShouldMapOnlyNonNullFields() {
        // Given
        ContentClick model = new ContentClick();
        model.setId(testId);
        model.setContentCode(testContentCode);
        // clicks est null

        // When
        com.itexpert.content.lib.entities.ContentClick entity = contentClickMapper.fromModel(model);

        // Then
        assertNotNull(entity);
        assertEquals(testId, entity.getId());
        assertEquals(testContentCode, entity.getContentCode());
        assertNull(entity.getClicks());
    }

    @Test
    void fromModel_WithZeroClicks_ShouldMapCorrectly() {
        // Given
        ContentClick model = new ContentClick();
        model.setId(testId);
        model.setContentCode(testContentCode);
        model.setClicks(0L);

        // When
        com.itexpert.content.lib.entities.ContentClick entity = contentClickMapper.fromModel(model);

        // Then
        assertNotNull(entity);
        assertEquals(testId, entity.getId());
        assertEquals(testContentCode, entity.getContentCode());
        assertEquals(0L, entity.getClicks());
    }

    @Test
    void fromEntityAndBack_ShouldBeConsistent() {
        // Given
        com.itexpert.content.lib.entities.ContentClick originalEntity = new com.itexpert.content.lib.entities.ContentClick();
        originalEntity.setId(testId);
        originalEntity.setContentCode(testContentCode);
        originalEntity.setClicks(testClicks);

        // When
        ContentClick model = contentClickMapper.fromEntity(originalEntity);
        com.itexpert.content.lib.entities.ContentClick mappedBackEntity = contentClickMapper.fromModel(model);

        // Then
        assertNotNull(model);
        assertNotNull(mappedBackEntity);
        assertEquals(originalEntity.getId(), mappedBackEntity.getId());
        assertEquals(originalEntity.getContentCode(), mappedBackEntity.getContentCode());
        assertEquals(originalEntity.getClicks(), mappedBackEntity.getClicks());
    }

    @Test
    void fromModelAndBack_ShouldBeConsistent() {
        // Given
        ContentClick originalModel = new ContentClick();
        originalModel.setId(testId);
        originalModel.setContentCode(testContentCode);
        originalModel.setClicks(testClicks);

        // When
        com.itexpert.content.lib.entities.ContentClick entity = contentClickMapper.fromModel(originalModel);
        ContentClick mappedBackModel = contentClickMapper.fromEntity(entity);

        // Then
        assertNotNull(entity);
        assertNotNull(mappedBackModel);
        assertEquals(originalModel.getId(), mappedBackModel.getId());
        assertEquals(originalModel.getContentCode(), mappedBackModel.getContentCode());
        assertEquals(originalModel.getClicks(), mappedBackModel.getClicks());
    }

    @Test
    void fromEntity_ShouldHandleLargeClickNumbers() {
        // Given
        com.itexpert.content.lib.entities.ContentClick entity = new com.itexpert.content.lib.entities.ContentClick();
        entity.setId(testId);
        entity.setContentCode(testContentCode);
        entity.setClicks(Long.MAX_VALUE);

        // When
        ContentClick model = contentClickMapper.fromEntity(entity);

        // Then
        assertNotNull(model);
        assertEquals(Long.MAX_VALUE, model.getClicks());
    }

    @Test
    void fromModel_ShouldHandleLargeClickNumbers() {
        // Given
        ContentClick model = new ContentClick();
        model.setId(testId);
        model.setContentCode(testContentCode);
        model.setClicks(Long.MAX_VALUE);

        // When
        com.itexpert.content.lib.entities.ContentClick entity = contentClickMapper.fromModel(model);

        // Then
        assertNotNull(entity);
        assertEquals(Long.MAX_VALUE, entity.getClicks());
    }

    @Test
    void fromEntity_ShouldHandleEmptyContentCode() {
        // Given
        com.itexpert.content.lib.entities.ContentClick entity = new com.itexpert.content.lib.entities.ContentClick();
        entity.setId(testId);
        entity.setContentCode("");
        entity.setClicks(testClicks);

        // When
        ContentClick model = contentClickMapper.fromEntity(entity);

        // Then
        assertNotNull(model);
        assertEquals("", model.getContentCode());
    }

    @Test
    void fromModel_ShouldHandleEmptyContentCode() {
        // Given
        ContentClick model = new ContentClick();
        model.setId(testId);
        model.setContentCode("");
        model.setClicks(testClicks);

        // When
        com.itexpert.content.lib.entities.ContentClick entity = contentClickMapper.fromModel(model);

        // Then
        assertNotNull(entity);
        assertEquals("", entity.getContentCode());
    }

    @Test
    void fromEntity_ShouldHandleDifferentIdTypes() {
        // Given
        com.itexpert.content.lib.entities.ContentClick entity = new com.itexpert.content.lib.entities.ContentClick();
        UUID firstId = UUID.randomUUID();
        UUID secondId = UUID.randomUUID();

        entity.setId(firstId);
        entity.setContentCode("TEST");
        entity.setClicks(10L);

        // When
        ContentClick model = contentClickMapper.fromEntity(entity);

        // Then
        assertNotNull(model);
        assertEquals(firstId, model.getId());
        assertNotEquals(secondId, model.getId());
    }

    @Test
    void fromModel_ShouldHandleDifferentIdTypes() {
        // Given
        ContentClick model = new ContentClick();
        UUID firstId = UUID.randomUUID();
        UUID secondId = UUID.randomUUID();

        model.setId(firstId);
        model.setContentCode("TEST");
        model.setClicks(10L);

        // When
        com.itexpert.content.lib.entities.ContentClick entity = contentClickMapper.fromModel(model);

        // Then
        assertNotNull(entity);
        assertEquals(firstId, entity.getId());
        assertNotEquals(secondId, entity.getId());
    }
}