package com.itexpert.content.api.mappers;

import com.itexpert.content.lib.entities.Node;
import com.itexpert.content.lib.models.Value;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NodeMapperTest {

    private NodeMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(NodeMapper.class);
    }

    @Test
    void shouldMapFromEntityToModel() {
        Node source = new Node();
        source.setCreationDate(123456789L);
        source.setModificationDate(987654321L);
        Value value = new Value();
        value.setKey("test");
        value.setValue("test-value");
        List<Value> values = Arrays.asList(value);
        source.setValues(values);
        source.setTriggerSecret("secret");
        source.setTriggerUrl("http://trigger.com");
        source.setSsg(true);

        com.itexpert.content.lib.models.Node result = mapper.fromEntity(source);

        assertThat(result).isNotNull();
        assertThat(result.getCreationDate()).isEqualTo(123456789L);
        assertThat(result.getModificationDate()).isEqualTo(987654321L);
        assertThat(result.getValues()).hasSize(1);
        assertThat(result.getValues().get(0).getKey()).isEqualTo("test");
        assertThat(result.getValues().get(0).getValue()).isEqualTo("test-value");
        assertThat(result.getContents()).isNull();
        assertThat(result.getTriggerSecret()).isNull();
        assertThat(result.getTriggerUrl()).isNull();
        assertThat(result.isSsg()).isFalse();
    }

    @Test
    void shouldMapFromModelToEntity() {
        com.itexpert.content.lib.models.Node source = new com.itexpert.content.lib.models.Node();
        source.setCreationDate(111222333L);
        source.setModificationDate(444555666L);
        Value value = new Value();
        value.setKey("model-key");
        value.setValue("model-value");
        List<Value> values = Arrays.asList(value);
        source.setValues(values);

        Node result = mapper.fromModel(source);

        assertThat(result).isNotNull();
        assertThat(result.getCreationDate()).isEqualTo(111222333L);
        assertThat(result.getModificationDate()).isEqualTo(444555666L);
        assertThat(result.getValues()).hasSize(1);
        assertThat(result.getValues().get(0).getKey()).isEqualTo("model-key");
        assertThat(result.getValues().get(0).getValue()).isEqualTo("model-value");
    }

    @Test
    void shouldHandleNullValues() {
        Node source = new Node();
        source.setCreationDate(null);
        source.setModificationDate(null);
        source.setValues(null);

        com.itexpert.content.lib.models.Node result = mapper.fromEntity(source);

        assertThat(result).isNotNull();
        assertThat(result.getCreationDate()).isNull();
        assertThat(result.getModificationDate()).isNull();
        assertThat(result.getValues()).isNull();
    }

    @Test
    void shouldIgnoreContentsField() {
        Node source = new Node();
        source.setCreationDate(123L);
        source.setModificationDate(456L);

        com.itexpert.content.lib.models.Node result = mapper.fromEntity(source);

        assertThat(result).isNotNull();
        assertThat(result.getContents()).isNull();
    }

    @Test
    void shouldMapAllIgnoredFieldsToNull() {
        Node source = new Node();
        source.setTriggerSecret("my-secret");
        source.setTriggerUrl("my-url");
        source.setSsg(true);
        source.setCreationDate(123L);
        source.setModificationDate(456L);

        com.itexpert.content.lib.models.Node result = mapper.fromEntity(source);

        assertThat(result.getTriggerSecret()).isNull();
        assertThat(result.getTriggerUrl()).isNull();
        assertThat(result.isSsg()).isFalse();
        assertThat(result.getCreationDate()).isEqualTo(123L);
        assertThat(result.getModificationDate()).isEqualTo(456L);
    }

    @Test
    void shouldMapEmptyValuesList() {
        Node source = new Node();
        source.setValues(Arrays.asList());
        source.setCreationDate(123L);
        source.setModificationDate(456L);

        com.itexpert.content.lib.models.Node result = mapper.fromEntity(source);

        assertThat(result).isNotNull();
        assertThat(result.getValues()).isEmpty();
    }
}