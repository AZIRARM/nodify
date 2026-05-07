package com.itexpert.content.api.mappers;

import com.google.gson.Gson;
import com.itexpert.content.lib.entities.ContentNode;
import com.itexpert.content.lib.enums.ContentTypeEnum;
import com.itexpert.content.lib.models.ContentFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.assertj.core.api.Assertions.assertThat;

class ContentNodeMapperTest {

    private ContentNodeMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(ContentNodeMapper.class);
    }

    @Test
    void shouldMapContentToPayloadForHtmlType() {
        ContentNode source = new ContentNode();
        source.setType(ContentTypeEnum.HTML);
        source.setContent("{\"title\":\"Test\",\"body\":\"Content\"}");

        com.itexpert.content.lib.models.ContentNode result = mapper.fromEntity(source);

        assertThat(result).isNotNull();
        assertThat(result.getPayload()).isNotNull();
    }

    @Test
    void shouldMapFileDataToPayloadForFileType() {
        ContentNode source = new ContentNode();
        source.setType(ContentTypeEnum.FILE);
        ContentFile file = new ContentFile();
        file.setData("base64EncodedData");
        source.setFile(file);

        com.itexpert.content.lib.models.ContentNode result = mapper.fromEntity(source);

        assertThat(result).isNotNull();
        assertThat(result.getPayload()).isEqualTo("base64EncodedData");
    }

    @Test
    void shouldMapFileDataToPayloadForPictureType() {
        ContentNode source = new ContentNode();
        source.setType(ContentTypeEnum.PICTURE);
        ContentFile file = new ContentFile();
        file.setData("/9j/4AAQSkZJRgABAQEAYABgAAD");
        source.setFile(file);

        com.itexpert.content.lib.models.ContentNode result = mapper.fromEntity(source);

        assertThat(result).isNotNull();
        assertThat(result.getPayload()).isEqualTo("/9j/4AAQSkZJRgABAQEAYABgAAD");
    }

    @Test
    void shouldReturnNullPayloadWhenFileIsNullForFileType() {
        ContentNode source = new ContentNode();
        source.setType(ContentTypeEnum.FILE);
        source.setFile(null);

        com.itexpert.content.lib.models.ContentNode result = mapper.fromEntity(source);

        assertThat(result).isNotNull();
        assertThat(result.getPayload()).isNull();
    }

    @Test
    void shouldReturnNullPayloadWhenContentIsNullForHtmlType() {
        ContentNode source = new ContentNode();
        source.setType(ContentTypeEnum.HTML);
        source.setContent(null);

        com.itexpert.content.lib.models.ContentNode result = mapper.fromEntity(source);

        assertThat(result).isNotNull();
        assertThat(result.getPayload()).isNull();
    }

    @Test
    void shouldMapRedirectUrlForUrlType() {
        ContentNode source = new ContentNode();
        source.setType(ContentTypeEnum.URLS);
        source.setRedirectUrl("https://example.com");

        com.itexpert.content.lib.models.ContentNode result = mapper.fromEntity(source);

        assertThat(result).isNotNull();
        assertThat(result.getPayload()).isEqualTo("https://example.com");
    }

    @Test
    void shouldIgnoreTriggerUrl() {
        ContentNode source = new ContentNode();
        source.setTriggerUrl("https://trigger.com");
        source.setType(ContentTypeEnum.HTML);
        source.setContent("{}");

        com.itexpert.content.lib.models.ContentNode result = mapper.fromEntity(source);

        assertThat(result).isNotNull();
        assertThat(result.getTriggerUrl()).isNull();
    }

    @Test
    void shouldIgnoreTriggerSecret() {
        ContentNode source = new ContentNode();
        source.setTriggerSecret("secret123");
        source.setType(ContentTypeEnum.HTML);
        source.setContent("{}");

        com.itexpert.content.lib.models.ContentNode result = mapper.fromEntity(source);

        assertThat(result).isNotNull();
        assertThat(result.getTriggerSecret()).isNull();
    }

    @Test
    void shouldIgnoreSsg() {
        ContentNode source = new ContentNode();
        source.setSsg(true);
        source.setType(ContentTypeEnum.HTML);
        source.setContent("{}");

        com.itexpert.content.lib.models.ContentNode result = mapper.fromEntity(source);

        assertThat(result).isNotNull();
        assertThat(result.isSsg()).isFalse();
    }

    @Test
    void shouldMapFromModelToEntity() {
        com.itexpert.content.lib.models.ContentNode source = new com.itexpert.content.lib.models.ContentNode();
        source.setCode("test-code");
        source.setTitle("Test Title");

        ContentNode result = mapper.fromModel(source);

        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo("test-code");
        assertThat(result.getTitle()).isEqualTo("Test Title");
    }
}