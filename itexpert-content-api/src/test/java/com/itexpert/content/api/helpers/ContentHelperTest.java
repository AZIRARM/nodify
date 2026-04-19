package com.itexpert.content.api.helpers;

import com.itexpert.content.api.handlers.ContentDisplayHandler;
import com.itexpert.content.api.mappers.ContentNodeMapper;
import com.itexpert.content.api.repositories.ContentNodeRepository;
import com.itexpert.content.api.repositories.NodeRepository;
import com.itexpert.content.api.repositories.PluginRepository;
import com.itexpert.content.api.utils.RulesUtils;
import com.itexpert.content.lib.entities.ContentNode;
import com.itexpert.content.lib.enums.StatusEnum;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContentHelperTest {

        @Mock
        private ContentNodeMapper contentNodeMapper;
        @Mock
        private ContentDisplayHandler contentDisplayHandler;
        @Mock
        private ContentNodeRepository contentNodeRepository;
        @Mock
        private NodeRepository nodeRepository;
        @Mock
        private PluginRepository pluginRepository;
        @Mock
        private PluginHelper pluginHelper;

        @InjectMocks
        private ContentHelper contentHelper;

        private ContentNode mockEntity;
        private com.itexpert.content.lib.models.ContentNode mockModel;
        private MockedStatic<RulesUtils> rulesUtilsMockedStatic;

        @BeforeEach
        void setUp() {
                mockEntity = new ContentNode();
                mockEntity.setCode("CONTENT-1");
                mockEntity.setContent("A simple text content");

                mockModel = new com.itexpert.content.lib.models.ContentNode();
                mockModel.setCode("CONTENT-1");

                rulesUtilsMockedStatic = Mockito.mockStatic(RulesUtils.class);
        }

        @AfterEach
        void tearDown() {
                rulesUtilsMockedStatic.close();
        }

        @Test
        void testFindByCodeAndStatus_Success() {
                when(contentNodeRepository.findByCodeAndStatus("CONTENT-1", StatusEnum.PUBLISHED.name()))
                                .thenReturn(Mono.just(mockEntity));
                when(contentNodeMapper.fromEntity(mockEntity)).thenReturn(mockModel);

                rulesUtilsMockedStatic.when(() -> RulesUtils.evaluateContentNode(mockModel))
                                .thenReturn(Mono.just(true));

                StepVerifier.create(contentHelper.findByCodeAndStatus("CONTENT-1", StatusEnum.PUBLISHED))
                                .expectNext(mockModel)
                                .verifyComplete();
        }

        @Test
        void testFindByCodeAndStatus_FailsEvaluation() {
                when(contentNodeRepository.findByCodeAndStatus("CONTENT-1", StatusEnum.PUBLISHED.name()))
                                .thenReturn(Mono.just(mockEntity));
                when(contentNodeMapper.fromEntity(mockEntity)).thenReturn(mockModel);

                rulesUtilsMockedStatic.when(() -> RulesUtils.evaluateContentNode(mockModel))
                                .thenReturn(Mono.just(false));

                StepVerifier.create(contentHelper.findByCodeAndStatus("CONTENT-1", StatusEnum.PUBLISHED))
                                .verifyComplete();
        }

        @Test
        void testFillContents_SimpleContent() {
                // When content has no codes
                when(pluginHelper.fillPlugin(mockEntity)).thenReturn(Mono.just(mockEntity));
                when(contentNodeMapper.fromEntity(mockEntity)).thenReturn(mockModel);

                rulesUtilsMockedStatic.when(() -> RulesUtils.evaluateContentNode(mockModel))
                                .thenReturn(Mono.just(true));

                StepVerifier.create(contentHelper.fillContents(mockEntity, StatusEnum.PUBLISHED, "fr"))
                                .expectNext(mockEntity)
                                .verifyComplete();
        }
}
