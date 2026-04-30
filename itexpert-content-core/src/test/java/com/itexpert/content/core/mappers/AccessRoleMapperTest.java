package com.itexpert.content.core.mappers;

import com.itexpert.content.core.models.AccessRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AccessRoleMapperTest {

    private AccessRoleMapper accessRoleMapper;
    private UUID testId;
    private String testName;
    private String testDescription;
    private String testCode;

    @BeforeEach
    void setUp() {
        // Initialisation manuelle du mapper (MapStruct génère l'implémentation)
        accessRoleMapper = new AccessRoleMapperImpl();

        testId = UUID.randomUUID();
        testName = "Administrator";
        testDescription = "Full access to all resources";
        testCode = "ADMIN";
    }

    @Test
    void fromEntity_ShouldMapAllFieldsCorrectly() {
        // Given
        com.itexpert.content.lib.entities.AccessRole entity = new com.itexpert.content.lib.entities.AccessRole();
        entity.setId(testId);
        entity.setName(testName);
        entity.setDescription(testDescription);
        entity.setCode(testCode);

        // When
        AccessRole model = accessRoleMapper.fromEntity(entity);

        // Then
        assertNotNull(model);
        assertEquals(testId, model.getId());
        assertEquals(testName, model.getName());
        assertEquals(testDescription, model.getDescription());
        assertEquals(testCode, model.getCode());
    }

    @Test
    void fromEntity_WithNullEntity_ShouldReturnNull() {
        // When
        AccessRole model = accessRoleMapper.fromEntity(null);

        // Then
        assertNull(model);
    }

    @Test
    void fromEntity_WithPartialEntity_ShouldMapOnlyNonNullFields() {
        // Given
        com.itexpert.content.lib.entities.AccessRole entity = new com.itexpert.content.lib.entities.AccessRole();
        entity.setId(testId);
        entity.setName(testName);
        // description et code sont null

        // When
        AccessRole model = accessRoleMapper.fromEntity(entity);

        // Then
        assertNotNull(model);
        assertEquals(testId, model.getId());
        assertEquals(testName, model.getName());
        assertNull(model.getDescription());
        assertNull(model.getCode());
    }

    @Test
    void fromModel_ShouldMapAllFieldsCorrectly() {
        // Given
        AccessRole model = new AccessRole();
        model.setId(testId);
        model.setName(testName);
        model.setDescription(testDescription);
        model.setCode(testCode);

        // When
        com.itexpert.content.lib.entities.AccessRole entity = accessRoleMapper.fromModel(model);

        // Then
        assertNotNull(entity);
        assertEquals(testId, entity.getId());
        assertEquals(testName, entity.getName());
        assertEquals(testDescription, entity.getDescription());
        assertEquals(testCode, entity.getCode());
    }

    @Test
    void fromModel_WithNullModel_ShouldReturnNull() {
        // When
        com.itexpert.content.lib.entities.AccessRole entity = accessRoleMapper.fromModel(null);

        // Then
        assertNull(entity);
    }

    @Test
    void fromModel_WithPartialModel_ShouldMapOnlyNonNullFields() {
        // Given
        AccessRole model = new AccessRole();
        model.setId(testId);
        model.setName(testName);
        // description et code sont null

        // When
        com.itexpert.content.lib.entities.AccessRole entity = accessRoleMapper.fromModel(model);

        // Then
        assertNotNull(entity);
        assertEquals(testId, entity.getId());
        assertEquals(testName, entity.getName());
        assertNull(entity.getDescription());
        assertNull(entity.getCode());
    }

    @Test
    void fromEntityAndBack_ShouldBeConsistent() {
        // Given
        com.itexpert.content.lib.entities.AccessRole originalEntity = new com.itexpert.content.lib.entities.AccessRole();
        originalEntity.setId(testId);
        originalEntity.setName(testName);
        originalEntity.setDescription(testDescription);
        originalEntity.setCode(testCode);

        // When
        AccessRole model = accessRoleMapper.fromEntity(originalEntity);
        com.itexpert.content.lib.entities.AccessRole mappedBackEntity = accessRoleMapper.fromModel(model);

        // Then
        assertNotNull(model);
        assertNotNull(mappedBackEntity);
        assertEquals(originalEntity.getId(), mappedBackEntity.getId());
        assertEquals(originalEntity.getName(), mappedBackEntity.getName());
        assertEquals(originalEntity.getDescription(), mappedBackEntity.getDescription());
        assertEquals(originalEntity.getCode(), mappedBackEntity.getCode());
    }

    @Test
    void fromModelAndBack_ShouldBeConsistent() {
        // Given
        AccessRole originalModel = new AccessRole();
        originalModel.setId(testId);
        originalModel.setName(testName);
        originalModel.setDescription(testDescription);
        originalModel.setCode(testCode);

        // When
        com.itexpert.content.lib.entities.AccessRole entity = accessRoleMapper.fromModel(originalModel);
        AccessRole mappedBackModel = accessRoleMapper.fromEntity(entity);

        // Then
        assertNotNull(entity);
        assertNotNull(mappedBackModel);
        assertEquals(originalModel.getId(), mappedBackModel.getId());
        assertEquals(originalModel.getName(), mappedBackModel.getName());
        assertEquals(originalModel.getDescription(), mappedBackModel.getDescription());
        assertEquals(originalModel.getCode(), mappedBackModel.getCode());
    }

    @Test
    void fromEntity_ShouldHandleUUIDProperly() {
        // Given
        com.itexpert.content.lib.entities.AccessRole entity = new com.itexpert.content.lib.entities.AccessRole();
        UUID uniqueId = UUID.randomUUID();
        entity.setId(uniqueId);
        entity.setName("Test");
        entity.setCode("TEST");

        // When
        AccessRole model = accessRoleMapper.fromEntity(entity);

        // Then
        assertNotNull(model);
        assertEquals(uniqueId, model.getId());
        assertNotEquals(UUID.randomUUID(), model.getId());
    }

    @Test
    void fromModel_ShouldHandleUUIDProperly() {
        // Given
        AccessRole model = new AccessRole();
        UUID uniqueId = UUID.randomUUID();
        model.setId(uniqueId);
        model.setName("Test");
        model.setCode("TEST");

        // When
        com.itexpert.content.lib.entities.AccessRole entity = accessRoleMapper.fromModel(model);

        // Then
        assertNotNull(entity);
        assertEquals(uniqueId, entity.getId());
        assertNotEquals(UUID.randomUUID(), entity.getId());
    }
}