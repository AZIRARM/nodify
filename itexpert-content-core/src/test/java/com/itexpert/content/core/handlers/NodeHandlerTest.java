
package com.itexpert.content.core.handlers;

import com.itexpert.content.core.helpers.NodeSlugHelper;
import com.itexpert.content.core.helpers.RenameNodeCodesHelper;
import com.itexpert.content.core.mappers.NodeMapper;
import com.itexpert.content.core.repositories.NodeRepository;
import com.itexpert.content.lib.enums.StatusEnum;
import com.itexpert.content.lib.models.Node;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class NodeHandlerTest {

        private NodeRepository nodeRepository;
        private NodeMapper nodeMapper;
        private ContentNodeHandler contentNodeHandler;
        private NotificationHandler notificationHandler;
        private RenameNodeCodesHelper renameNodeCodesHelper;
        private UserHandler userHandler;
        private NodeHandler nodeHandler;
        private NodeSlugHelper nodeSlugHelper;

        @BeforeEach
        void setup() {
                contentNodeHandler = mock(ContentNodeHandler.class);
                nodeMapper = Mappers.getMapper(NodeMapper.class);
                nodeRepository = mock(NodeRepository.class);
                userHandler = mock(UserHandler.class);
                notificationHandler = mock(NotificationHandler.class);
                renameNodeCodesHelper = new RenameNodeCodesHelper();
                nodeSlugHelper = mock(NodeSlugHelper.class);

                nodeHandler = spy(new NodeHandler(
                        nodeRepository,
                        nodeMapper,
                        contentNodeHandler,
                        notificationHandler,
                        renameNodeCodesHelper,
                        userHandler,
                        nodeSlugHelper
                ));

                when(nodeRepository.save(any()))
                        .thenAnswer(invocation ->
                                Mono.just(invocation.getArgument(0)));

                doAnswer(invocation ->
                        Mono.just(invocation.getArgument(0))
                ).when(nodeHandler).notify(any(), any());
        }

        @Test
        void save_shouldGenerateCodeWithNodePrefixWhenParentCodeExists() {
                Node node = new Node();
                node.setName("My Node");
                node.setParentCode("PARENT-123");

                StepVerifier.create(nodeHandler.save(node))
                        .assertNext(saved ->
                                assertTrue(saved.getCode().startsWith("MY_NODE-NODE-")))
                        .verifyComplete();
        }

        @Test
        void save_shouldGenerateCodeWithParentCodeOriginPrefix() {
                Node node = new Node();
                node.setName("My Node");
                node.setParentCode("PARENT-123");
                node.setParentCodeOrigin("ORIGIN-456");

                StepVerifier.create(nodeHandler.save(node))
                        .assertNext(saved ->
                                assertTrue(saved.getCode().startsWith("MY_NODE-ORIGIN-")))
                        .verifyComplete();
        }

        @Test
        void save_shouldUseFirstSegmentOfParentCodeOrigin() {
                Node node = new Node();
                node.setName("My Node");
                node.setParentCode("PARENT-123");
                node.setParentCodeOrigin("ORIGIN-SECOND-THIRD");

                StepVerifier.create(nodeHandler.save(node))
                        .assertNext(saved ->
                                assertTrue(saved.getCode().startsWith("MY_NODE-ORIGIN-")))
                        .verifyComplete();
        }

        @Test
        void save_shouldNotAddIntermediatePrefixWhenParentCodeIsEmpty() {
                Node node = new Node();
                node.setName("My Node");
                node.setParentCode("");

                StepVerifier.create(nodeHandler.save(node))
                        .assertNext(saved ->
                                assertTrue(saved.getCode().startsWith("MY_NODE-")))
                        .verifyComplete();
        }

        @Test
        void save_shouldUseUnknownWhenNameIsNull() {
                Node node = new Node();
                node.setParentCode("PARENT-123");

                StepVerifier.create(nodeHandler.save(node))
                        .assertNext(saved ->
                                assertTrue(saved.getCode().startsWith("UNKNOWN-NODE-")))
                        .verifyComplete();
        }

        @Test
        void save_shouldNormalizeSpecialCharactersInName() {
                Node node = new Node();
                node.setName("My Node / Test");
                node.setParentCode("PARENT-123");

                StepVerifier.create(nodeHandler.save(node))
                        .assertNext(saved ->
                                assertTrue(saved.getCode().startsWith("MY_NODE_TEST-NODE-")))
                        .verifyComplete();
        }

        @Test
        void save_shouldAppendTimestampToCode() {
                long beforeSave = System.currentTimeMillis();

                Node node = new Node();
                node.setName("My Node");
                node.setParentCode("PARENT-123");

                StepVerifier.create(nodeHandler.save(node))
                        .assertNext(saved -> {
                                String[] parts = saved.getCode().split("-");
                                long timestamp = Long.parseLong(parts[parts.length - 1]);

                                assertTrue(timestamp >= beforeSave);
                                assertTrue(timestamp <= System.currentTimeMillis());
                        })
                        .verifyComplete();
        }

        @Test
        void save_shouldSetCreationMetadata() {
                Node node = new Node();
                node.setName("My Node");
                node.setParentCode("PARENT-123");

                StepVerifier.create(nodeHandler.save(node))
                        .assertNext(saved -> {
                                assertNotNull(saved.getId());
                                assertEquals("0", saved.getVersion());
                                assertEquals(StatusEnum.SNAPSHOT, saved.getStatus());
                                assertNotNull(saved.getCreationDate());
                                assertNotNull(saved.getModificationDate());
                                assertEquals(saved.getCreationDate(), saved.getModificationDate());
                        })
                        .verifyComplete();
        }

        @Test
        void save_shouldSetDefaultRulesWhenRulesAreEmpty() {
                Node node = new Node();
                node.setName("My Node");
                node.setParentCode("PARENT-123");
                node.setRules(null);

                StepVerifier.create(nodeHandler.save(node))
                        .assertNext(saved -> assertNotNull(saved.getRules()))
                        .verifyComplete();
        }

        @Test
        void save_shouldPreserveExistingCodeOnUpdate() {
                String existingCode = "MY_NODE-ORIGIN-123";

                Node node = new Node();
                node.setId(UUID.randomUUID());
                node.setName("My Node");
                node.setCode(existingCode);

                StepVerifier.create(nodeHandler.save(node))
                        .assertNext(saved -> {
                                assertEquals(existingCode, saved.getCode());
                                assertEquals(node.getId(), saved.getId());
                                assertNotNull(saved.getModificationDate());
                        })
                        .verifyComplete();
        }

        @Test
        void save_shouldGenerateCodeOnUpdateWhenCodeIsEmpty() {
                Node node = new Node();
                node.setId(UUID.randomUUID());
                node.setName("My Node");
                node.setParentCode("PARENT-123");
                node.setCode(null);

                StepVerifier.create(nodeHandler.save(node))
                        .assertNext(saved ->
                                assertTrue(saved.getCode().startsWith("MY_NODE-NODE-")))
                        .verifyComplete();
        }
}
