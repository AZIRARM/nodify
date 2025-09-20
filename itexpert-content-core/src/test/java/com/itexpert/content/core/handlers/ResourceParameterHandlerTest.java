package com.itexpert.content.core.handlers;

import com.itexpert.content.core.mappers.ResourceParameterMapper;
import com.itexpert.content.core.repositories.ResourceParamaterRepository;
import com.itexpert.content.lib.enums.ResourceActionEnum;
import com.itexpert.content.lib.enums.ResourceTypeEnum;
import com.itexpert.content.lib.enums.StatusEnum;
import com.itexpert.content.lib.models.ContentNode;
import com.itexpert.content.lib.models.Node;
import com.itexpert.content.lib.models.ResourceParameter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ResourceParameterHandlerTest {

    private ResourceParameterHandler resourceParameterHandler;

    private ResourceParamaterRepository resourceParamaterRepository;
    private ResourceParameterMapper resourceParamaterMapper;
    private NodeHandler nodeHandler;
    private ContentNodeHandler contentNodeHandler;

    @BeforeEach
    void setup() {
        resourceParamaterRepository = mock(ResourceParamaterRepository.class);
        nodeHandler = mock(NodeHandler.class);
        contentNodeHandler = mock(ContentNodeHandler.class);
        resourceParamaterMapper = mock(ResourceParameterMapper.class);

        resourceParameterHandler = new ResourceParameterHandler(resourceParamaterRepository,
                resourceParamaterMapper,
                nodeHandler,
                contentNodeHandler);
    }

    @Test
    void testCleanupArchivedChildren() {
        // Mock ResourceParameter
        com.itexpert.content.lib.entities.ResourceParameter parameter = new com.itexpert.content.lib.entities.ResourceParameter();
        parameter.setCode("PARENT_CODE");
        parameter.setValue(1); // garder le dernier
        when(resourceParamaterRepository.findByTypeAndAction(ResourceTypeEnum.NODE, ResourceActionEnum.ARCHIVE))
                .thenReturn(Flux.just(parameter));

        // Mock Nodes
        Node oldNode = new Node();
        oldNode.setId(UUID.randomUUID());
        oldNode.setCode("NODE1");
        oldNode.setCreationDate(System.currentTimeMillis() - 10000);
        oldNode.setStatus(StatusEnum.ARCHIVE);

        Node newNode = new Node();
        newNode.setId(UUID.randomUUID());
        newNode.setCode("NODE2");
        newNode.setCreationDate(System.currentTimeMillis());
        newNode.setStatus(StatusEnum.ARCHIVE);

        when(nodeHandler.findAllByParentCodeAndStatus("PARENT_CODE", StatusEnum.ARCHIVE.name()))
                .thenReturn(Flux.just(oldNode, newNode));

        when(nodeHandler.deleteById(any(UUID.class))).thenReturn(Mono.empty());

        // Mock ContentNodes
        ContentNode oldContent = new ContentNode();
        oldContent.setId(UUID.randomUUID());
        oldContent.setCode("CONTENT1");
        oldContent.setCreationDate(System.currentTimeMillis() - 10000);
        oldContent.setStatus(StatusEnum.ARCHIVE);

        ContentNode newContent = new ContentNode();
        newContent.setId(UUID.randomUUID());
        newContent.setCode("CONTENT2");
        newContent.setCreationDate(System.currentTimeMillis());
        newContent.setStatus(StatusEnum.ARCHIVE);

        when(contentNodeHandler.findAllByNodeCodeAndStatus("NODE1", StatusEnum.ARCHIVE.name()))
                .thenReturn(Flux.just(oldContent, newContent));

        when(contentNodeHandler.deleteById(any(UUID.class))).thenReturn(Mono.empty());

        StepVerifier.create(resourceParameterHandler.cleanupArchivedChildren())
                .expectNext(true)
                .verifyComplete();

        verify(nodeHandler, times(1)).deleteById(oldNode.getId());
        verify(contentNodeHandler, times(1)).deleteById(oldContent.getId());
    }

    @Test
    void testFindByTypeAndAction() {
        com.itexpert.content.lib.entities.ResourceParameter parameter = new com.itexpert.content.lib.entities.ResourceParameter();
        ResourceParameter model = new ResourceParameter();
        model.setId(UUID.randomUUID());
        model.setType(ResourceTypeEnum.NODE);
        model.setAction(ResourceActionEnum.ARCHIVE);

        when(resourceParamaterRepository.findByTypeAndAction(ResourceTypeEnum.NODE, ResourceActionEnum.ARCHIVE))
                .thenReturn(Flux.just(parameter));
        when(resourceParamaterMapper.fromEntity(parameter)).thenReturn(model);

        StepVerifier.create(resourceParameterHandler.findByTypeAndAction(ResourceTypeEnum.NODE, ResourceActionEnum.ARCHIVE))
                .expectNext(model)
                .verifyComplete();
    }

    @Test
    void testDeleteById() {
        UUID id = UUID.randomUUID();
        when(resourceParamaterRepository.deleteById(id)).thenReturn(Mono.empty());

        StepVerifier.create(resourceParameterHandler.deleteById(id))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void testSave() {
        com.itexpert.content.lib.entities.ResourceParameter resource = new com.itexpert.content.lib.entities.ResourceParameter();
        resource.setCode("TEST");

        ResourceParameter model = new ResourceParameter();
        model.setCode("TEST");

        when(resourceParamaterMapper.fromModel(any(ResourceParameter.class))).thenReturn(resource);
        when(resourceParamaterRepository.save(resource)).thenReturn(Mono.just(resource));
        when(resourceParamaterMapper.fromEntity(resource)).thenCallRealMethod();

        StepVerifier.create(resourceParameterHandler.save(model))
                .expectNext(model)
                .verifyComplete();
    }
}
