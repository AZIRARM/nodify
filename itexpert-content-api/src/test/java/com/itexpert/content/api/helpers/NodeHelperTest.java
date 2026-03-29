package com.itexpert.content.api.helpers;

import com.itexpert.content.api.mappers.NodeMapper;
import com.itexpert.content.api.repositories.NodeRepository;
import com.itexpert.content.api.utils.RulesUtils;
import com.itexpert.content.lib.enums.StatusEnum;
import com.itexpert.content.lib.models.Node;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NodeHelperTest {

    @Mock
    private NodeRepository nodeRepository;

    @Mock
    private NodeMapper nodeMapper;

    @InjectMocks
    private NodeHelper nodeHelper;

    private Node mockNode;
    private com.itexpert.content.lib.entities.Node mockEntity;

    private MockedStatic<RulesUtils> rulesUtilsMockedStatic;

    @BeforeEach
    void setUp() {
        mockNode = new Node();
        mockNode.setCode("node1");
        
        mockEntity = new com.itexpert.content.lib.entities.Node();
        mockEntity.setCode("node1");

        rulesUtilsMockedStatic = Mockito.mockStatic(RulesUtils.class);
    }

    @AfterEach
    void tearDown() {
        rulesUtilsMockedStatic.close();
    }

    @Test
    void testFindByCodeAndStatus_Success() {
        when(nodeRepository.findByCodeAndStatus("node1", StatusEnum.PUBLISHED.name()))
                .thenReturn(Mono.just(mockEntity));
        when(nodeMapper.fromEntity(mockEntity)).thenReturn(mockNode);
        
        rulesUtilsMockedStatic.when(() -> RulesUtils.evaluateNode(mockNode))
                .thenReturn(Mono.just(true));

        StepVerifier.create(nodeHelper.findByCodeAndStatus("node1", StatusEnum.PUBLISHED))
                .expectNext(mockNode)
                .verifyComplete();
    }

    @Test
    void testFindByCodeAndStatus_FailsEvaluation() {
        when(nodeRepository.findByCodeAndStatus("node1", StatusEnum.PUBLISHED.name()))
                .thenReturn(Mono.just(mockEntity));
        when(nodeMapper.fromEntity(mockEntity)).thenReturn(mockNode);
        
        rulesUtilsMockedStatic.when(() -> RulesUtils.evaluateNode(mockNode))
                .thenReturn(Mono.just(false));

        StepVerifier.create(nodeHelper.findByCodeAndStatus("node1", StatusEnum.PUBLISHED))
                .verifyComplete();
    }

    @Test
    void testEvaluateNode_NoParent() {
        when(nodeRepository.findByCodeAndStatus("node1", StatusEnum.PUBLISHED.name()))
                .thenReturn(Mono.just(mockEntity));
        when(nodeMapper.fromEntity(mockEntity)).thenReturn(mockNode);
        
        rulesUtilsMockedStatic.when(() -> RulesUtils.evaluateNode(mockNode))
                .thenReturn(Mono.just(true));

        StepVerifier.create(nodeHelper.evaluateNode("node1", StatusEnum.PUBLISHED))
                .expectNext(mockNode)
                .verifyComplete();
    }
}
