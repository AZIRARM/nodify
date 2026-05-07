package com.itexpert.content.api.mappers;

import com.itexpert.content.lib.entities.Data;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DataMapperTest {

    private DataMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(DataMapper.class);
    }

    @Test
    void shouldMapFromEntityToModel() {
        Data source = new Data();
        UUID id = UUID.randomUUID();
        source.setId(id);
        source.setContentNodeCode("node-code-123");
        source.setCreationDate(123456789L);
        source.setModificationDate(987654321L);
        source.setDataType("string");
        source.setName("Test Data");
        source.setUser("john.doe");
        source.setKey("unique-key");
        source.setValue("test-value");

        com.itexpert.content.lib.models.Data result = mapper.fromEntity(source);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getContentNodeCode()).isEqualTo("node-code-123");
        assertThat(result.getCreationDate()).isEqualTo(123456789L);
        assertThat(result.getModificationDate()).isEqualTo(987654321L);
        assertThat(result.getDataType()).isEqualTo("string");
        assertThat(result.getName()).isEqualTo("Test Data");
        assertThat(result.getUser()).isEqualTo("john.doe");
        assertThat(result.getKey()).isEqualTo("unique-key");
        assertThat(result.getValue()).isEqualTo("test-value");
    }

    @Test
    void shouldMapFromModelToEntity() {
        com.itexpert.content.lib.models.Data source = new com.itexpert.content.lib.models.Data();
        UUID id = UUID.randomUUID();
        source.setId(id);
        source.setContentNodeCode("model-node-code");
        source.setCreationDate(111222333L);
        source.setModificationDate(444555666L);
        source.setDataType("number");
        source.setName("Model Data");
        source.setUser("jane.doe");
        source.setKey("model-key");
        source.setValue("42");

        Data result = mapper.fromModel(source);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getContentNodeCode()).isEqualTo("model-node-code");
        assertThat(result.getCreationDate()).isEqualTo(111222333L);
        assertThat(result.getModificationDate()).isEqualTo(444555666L);
        assertThat(result.getDataType()).isEqualTo("number");
        assertThat(result.getName()).isEqualTo("Model Data");
        assertThat(result.getUser()).isEqualTo("jane.doe");
        assertThat(result.getKey()).isEqualTo("model-key");
        assertThat(result.getValue()).isEqualTo("42");
    }

    @Test
    void shouldHandleNullValues() {
        Data source = new Data();
        source.setId(null);
        source.setContentNodeCode(null);
        source.setCreationDate(null);
        source.setModificationDate(null);
        source.setDataType(null);
        source.setName(null);
        source.setUser(null);
        source.setKey(null);
        source.setValue(null);

        com.itexpert.content.lib.models.Data result = mapper.fromEntity(source);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isNull();
        assertThat(result.getContentNodeCode()).isNull();
        assertThat(result.getCreationDate()).isNull();
        assertThat(result.getModificationDate()).isNull();
        assertThat(result.getDataType()).isNull();
        assertThat(result.getName()).isNull();
        assertThat(result.getUser()).isNull();
        assertThat(result.getKey()).isNull();
        assertThat(result.getValue()).isNull();
    }

    @Test
    void shouldMapAllFieldsCorrectly() {
        Data source = new Data();
        source.setId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));
        source.setContentNodeCode("content-node");
        source.setCreationDate(1000000000L);
        source.setModificationDate(2000000000L);
        source.setDataType("boolean");
        source.setName("Active Flag");
        source.setUser("admin");
        source.setKey("app.active");
        source.setValue("true");

        com.itexpert.content.lib.models.Data result = mapper.fromEntity(source);

        assertThat(result.getId()).isEqualTo(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));
        assertThat(result.getContentNodeCode()).isEqualTo("content-node");
        assertThat(result.getCreationDate()).isEqualTo(1000000000L);
        assertThat(result.getModificationDate()).isEqualTo(2000000000L);
        assertThat(result.getDataType()).isEqualTo("boolean");
        assertThat(result.getName()).isEqualTo("Active Flag");
        assertThat(result.getUser()).isEqualTo("admin");
        assertThat(result.getKey()).isEqualTo("app.active");
        assertThat(result.getValue()).isEqualTo("true");
    }

    @Test
    void shouldMapEmptyStrings() {
        Data source = new Data();
        source.setContentNodeCode("");
        source.setDataType("");
        source.setName("");
        source.setUser("");
        source.setKey("");
        source.setValue("");

        com.itexpert.content.lib.models.Data result = mapper.fromEntity(source);

        assertThat(result.getContentNodeCode()).isEmpty();
        assertThat(result.getDataType()).isEmpty();
        assertThat(result.getName()).isEmpty();
        assertThat(result.getUser()).isEmpty();
        assertThat(result.getKey()).isEmpty();
        assertThat(result.getValue()).isEmpty();
    }
}