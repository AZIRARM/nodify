package com.itexpert.content.core.schedulers;

import com.itexpert.content.core.repositories.ContentNodeRepository;
import com.itexpert.content.core.repositories.NodeRepository;
import com.itexpert.content.lib.entities.ContentNode;
import com.itexpert.content.lib.entities.Node;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResourceCleanupSchedulerTest {

    @Mock
    private NodeRepository nodeRepository;

    @Mock
    private ContentNodeRepository contentNodeRepository;

    @InjectMocks
    private ResourceCleanupScheduler resourceCleanupScheduler;

    // ==================== TESTS FOR cleanNodes() ====================

    /**
     * Test: Successfully deletes archived nodes exceeding maxVersionsToKeep.
     */
    @Test
    void cleanNodes_ShouldDeleteExcessArchivedNodes_WhenMaxVersionsToKeepIsSet() {
        String code = "TEST_CODE";
        int maxVersionsToKeep = 2;

        Node activeNode = new Node();
        activeNode.setCode(code);
        activeNode.setMaxVersionsToKeep(maxVersionsToKeep);

        Node archivedNode1 = createArchivedNode(code, 3000L);
        Node archivedNode2 = createArchivedNode(code, 2000L);
        Node archivedNode3 = createArchivedNode(code, 1000L); // Should be deleted (skip 2)

        when(nodeRepository.findActiveNodesToClean()).thenReturn(Flux.just(activeNode));
        when(nodeRepository.findArchivedByCode(code))
                .thenReturn(Flux.just(archivedNode1, archivedNode2, archivedNode3));
        when(nodeRepository.delete(any(Node.class))).thenReturn(Mono.empty());

        resourceCleanupScheduler.cleanNodes();

        verify(nodeRepository).findActiveNodesToClean();
        verify(nodeRepository).findArchivedByCode(code);
        // Should delete only the excess nodes (archivedNode3)
        verify(nodeRepository, times(1)).delete(archivedNode3);
        verify(nodeRepository, never()).delete(archivedNode1);
        verify(nodeRepository, never()).delete(archivedNode2);
    }

    /**
     * Test: Does nothing when maxVersionsToKeep is null.
     */
    @Test
    void cleanNodes_ShouldDoNothing_WhenMaxVersionsToKeepIsNull() {
        Node activeNode = new Node();
        activeNode.setCode("TEST_CODE");
        activeNode.setMaxVersionsToKeep(null);

        when(nodeRepository.findActiveNodesToClean()).thenReturn(Flux.just(activeNode));

        resourceCleanupScheduler.cleanNodes();

        verify(nodeRepository).findActiveNodesToClean();
        verify(nodeRepository, never()).findArchivedByCode(anyString());
        verify(nodeRepository, never()).delete(any(Node.class));
    }

    /**
     * Test: Does nothing when maxVersionsToKeep is zero or negative.
     */
    @Test
    void cleanNodes_ShouldDoNothing_WhenMaxVersionsToKeepIsZeroOrNegative() {
        Node activeNode = new Node();
        activeNode.setCode("TEST_CODE");
        activeNode.setMaxVersionsToKeep(0);

        when(nodeRepository.findActiveNodesToClean()).thenReturn(Flux.just(activeNode));

        resourceCleanupScheduler.cleanNodes();

        verify(nodeRepository).findActiveNodesToClean();
        verify(nodeRepository, never()).findArchivedByCode(anyString());
        verify(nodeRepository, never()).delete(any(Node.class));
    }

    /**
     * Test: Handles multiple active nodes correctly.
     */
    @Test
    void cleanNodes_ShouldHandleMultipleActiveNodes_Correctly() {
        String code1 = "CODE1";
        String code2 = "CODE2";
        int maxVersionsToKeep = 1;

        Node activeNode1 = new Node();
        activeNode1.setCode(code1);
        activeNode1.setMaxVersionsToKeep(maxVersionsToKeep);

        Node activeNode2 = new Node();
        activeNode2.setCode(code2);
        activeNode2.setMaxVersionsToKeep(maxVersionsToKeep);

        Node archivedNode1_1 = createArchivedNode(code1, 3000L);
        Node archivedNode1_2 = createArchivedNode(code1, 2000L); // Should be deleted
        Node archivedNode2_1 = createArchivedNode(code2, 1500L);
        Node archivedNode2_2 = createArchivedNode(code2, 1000L); // Should be deleted

        when(nodeRepository.findActiveNodesToClean()).thenReturn(Flux.just(activeNode1, activeNode2));
        when(nodeRepository.findArchivedByCode(code1)).thenReturn(Flux.just(archivedNode1_1, archivedNode1_2));
        when(nodeRepository.findArchivedByCode(code2)).thenReturn(Flux.just(archivedNode2_1, archivedNode2_2));
        when(nodeRepository.delete(any(Node.class))).thenReturn(Mono.empty());

        resourceCleanupScheduler.cleanNodes();

        verify(nodeRepository).findActiveNodesToClean();
        verify(nodeRepository).findArchivedByCode(code1);
        verify(nodeRepository).findArchivedByCode(code2);
        verify(nodeRepository, times(1)).delete(archivedNode1_2);
        verify(nodeRepository, times(1)).delete(archivedNode2_2);
        verify(nodeRepository, never()).delete(archivedNode1_1);
        verify(nodeRepository, never()).delete(archivedNode2_1);
    }

    /**
     * Test: Handles empty active nodes list gracefully.
     */
    @Test
    void cleanNodes_ShouldDoNothing_WhenNoActiveNodesFound() {
        when(nodeRepository.findActiveNodesToClean()).thenReturn(Flux.empty());

        resourceCleanupScheduler.cleanNodes();

        verify(nodeRepository).findActiveNodesToClean();
        verify(nodeRepository, never()).findArchivedByCode(anyString());
        verify(nodeRepository, never()).delete(any(Node.class));
    }

    /**
     * Test: Handles error during findActiveNodesToClean gracefully.
     */
    @Test
    void cleanNodes_ShouldHandleError_WhenFindActiveNodesFails() {
        RuntimeException error = new RuntimeException("Database connection failed");

        when(nodeRepository.findActiveNodesToClean()).thenReturn(Flux.error(error));

        resourceCleanupScheduler.cleanNodes();

        verify(nodeRepository).findActiveNodesToClean();
        verify(nodeRepository, never()).findArchivedByCode(anyString());
        verify(nodeRepository, never()).delete(any(Node.class));
    }

    /**
     * Test: Handles error during findArchivedByCode gracefully (fail-fast
     * behavior).
     */
    @Test
    void cleanNodes_ShouldHandleError_WhenFindArchivedByCodeFails() {
        String code = "TEST_CODE";
        int maxVersionsToKeep = 2;

        Node activeNode = new Node();
        activeNode.setCode(code);
        activeNode.setMaxVersionsToKeep(maxVersionsToKeep);
        RuntimeException error = new RuntimeException("Archived retrieval failed");

        when(nodeRepository.findActiveNodesToClean()).thenReturn(Flux.just(activeNode));
        when(nodeRepository.findArchivedByCode(code)).thenReturn(Flux.error(error));

        resourceCleanupScheduler.cleanNodes();

        verify(nodeRepository).findActiveNodesToClean();
        verify(nodeRepository).findArchivedByCode(code);
        verify(nodeRepository, never()).delete(any(Node.class));
    }

    /**
     * Test: Does not delete when archived count is less than or equal to
     * maxVersionsToKeep.
     */
    @Test
    void cleanNodes_ShouldNotDeleteAnything_WhenArchivedCountIsLessThanOrEqualToMax() {
        String code = "TEST_CODE";
        int maxVersionsToKeep = 3;

        Node activeNode = new Node();
        activeNode.setCode(code);
        activeNode.setMaxVersionsToKeep(maxVersionsToKeep);

        Node archivedNode1 = createArchivedNode(code, 3000L);
        Node archivedNode2 = createArchivedNode(code, 2000L);
        Node archivedNode3 = createArchivedNode(code, 1000L);

        when(nodeRepository.findActiveNodesToClean()).thenReturn(Flux.just(activeNode));
        when(nodeRepository.findArchivedByCode(code))
                .thenReturn(Flux.just(archivedNode1, archivedNode2, archivedNode3));

        resourceCleanupScheduler.cleanNodes();

        verify(nodeRepository).findActiveNodesToClean();
        verify(nodeRepository).findArchivedByCode(code);
        verify(nodeRepository, never()).delete(any(Node.class));
    }

    // ==================== TESTS FOR cleanContentNodes() ====================

    /**
     * Test: Successfully deletes archived content nodes exceeding
     * maxVersionsToKeep.
     */
    @Test
    void cleanContentNodes_ShouldDeleteExcessArchivedContentNodes_WhenMaxVersionsToKeepIsSet() {
        String code = "TEST_CODE";
        int maxVersionsToKeep = 2;

        ContentNode activeContent = createContentNode(code, maxVersionsToKeep, 4000L);
        ContentNode archivedContent1 = createContentNode(code, maxVersionsToKeep, 3000L);
        ContentNode archivedContent2 = createContentNode(code, maxVersionsToKeep, 2000L);
        ContentNode archivedContent3 = createContentNode(code, maxVersionsToKeep, 1000L); // Should be deleted

        when(contentNodeRepository.findContentToClean()).thenReturn(Flux.just(activeContent));
        when(contentNodeRepository.findArchivedByNodeCode(code))
                .thenReturn(Flux.just(archivedContent1, archivedContent2, archivedContent3));
        when(contentNodeRepository.delete(any(ContentNode.class))).thenReturn(Mono.empty());

        resourceCleanupScheduler.cleanContentNodes();

        verify(contentNodeRepository).findContentToClean();
        verify(contentNodeRepository).findArchivedByNodeCode(code);
        verify(contentNodeRepository, times(1)).delete(archivedContent3);
        verify(contentNodeRepository, never()).delete(archivedContent1);
        verify(contentNodeRepository, never()).delete(archivedContent2);
    }

    /**
     * Test: Does nothing when maxVersionsToKeep is null.
     */
    @Test
    void cleanContentNodes_ShouldDoNothing_WhenMaxVersionsToKeepIsNull() {
        ContentNode activeContent = new ContentNode();
        activeContent.setCode("TEST_CODE");
        activeContent.setMaxVersionsToKeep(null);

        when(contentNodeRepository.findContentToClean()).thenReturn(Flux.just(activeContent));

        resourceCleanupScheduler.cleanContentNodes();

        verify(contentNodeRepository).findContentToClean();
        verify(contentNodeRepository, never()).findArchivedByNodeCode(anyString());
        verify(contentNodeRepository, never()).delete(any(ContentNode.class));
    }

    /**
     * Test: Does nothing when maxVersionsToKeep is zero or negative.
     */
    @Test
    void cleanContentNodes_ShouldDoNothing_WhenMaxVersionsToKeepIsZeroOrNegative() {
        ContentNode activeContent = new ContentNode();
        activeContent.setCode("TEST_CODE");
        activeContent.setMaxVersionsToKeep(0);

        when(contentNodeRepository.findContentToClean()).thenReturn(Flux.just(activeContent));

        resourceCleanupScheduler.cleanContentNodes();

        verify(contentNodeRepository).findContentToClean();
        verify(contentNodeRepository, never()).findArchivedByNodeCode(anyString());
        verify(contentNodeRepository, never()).delete(any(ContentNode.class));
    }

    /**
     * Test: Does nothing when code is null.
     */
    @Test
    void cleanContentNodes_ShouldDoNothing_WhenCodeIsNull() {
        ContentNode activeContent = new ContentNode();
        activeContent.setCode(null);
        activeContent.setMaxVersionsToKeep(5);

        when(contentNodeRepository.findContentToClean()).thenReturn(Flux.just(activeContent));

        resourceCleanupScheduler.cleanContentNodes();

        verify(contentNodeRepository).findContentToClean();
        verify(contentNodeRepository, never()).findArchivedByNodeCode(anyString());
        verify(contentNodeRepository, never()).delete(any(ContentNode.class));
    }

    /**
     * Test: Handles multiple active content nodes correctly.
     */
    @Test
    void cleanContentNodes_ShouldHandleMultipleActiveContents_Correctly() {
        String code1 = "CODE1";
        String code2 = "CODE2";
        int maxVersionsToKeep = 1;

        ContentNode activeContent1 = createContentNode(code1, maxVersionsToKeep, 4000L);
        ContentNode activeContent2 = createContentNode(code2, maxVersionsToKeep, 3500L);
        ContentNode archivedContent1_1 = createContentNode(code1, maxVersionsToKeep, 3000L);
        ContentNode archivedContent1_2 = createContentNode(code1, maxVersionsToKeep, 2000L); // Should be deleted
        ContentNode archivedContent2_1 = createContentNode(code2, maxVersionsToKeep, 2500L);
        ContentNode archivedContent2_2 = createContentNode(code2, maxVersionsToKeep, 1500L); // Should be deleted

        when(contentNodeRepository.findContentToClean()).thenReturn(Flux.just(activeContent1, activeContent2));
        when(contentNodeRepository.findArchivedByNodeCode(code1))
                .thenReturn(Flux.just(archivedContent1_1, archivedContent1_2));
        when(contentNodeRepository.findArchivedByNodeCode(code2))
                .thenReturn(Flux.just(archivedContent2_1, archivedContent2_2));
        when(contentNodeRepository.delete(any(ContentNode.class))).thenReturn(Mono.empty());

        resourceCleanupScheduler.cleanContentNodes();

        verify(contentNodeRepository).findContentToClean();
        verify(contentNodeRepository).findArchivedByNodeCode(code1);
        verify(contentNodeRepository).findArchivedByNodeCode(code2);
        verify(contentNodeRepository, times(1)).delete(archivedContent1_2);
        verify(contentNodeRepository, times(1)).delete(archivedContent2_2);
        verify(contentNodeRepository, never()).delete(archivedContent1_1);
        verify(contentNodeRepository, never()).delete(archivedContent2_1);
    }

    /**
     * Test: Handles empty active content nodes list gracefully.
     */
    @Test
    void cleanContentNodes_ShouldDoNothing_WhenNoActiveContentNodesFound() {
        when(contentNodeRepository.findContentToClean()).thenReturn(Flux.empty());

        resourceCleanupScheduler.cleanContentNodes();

        verify(contentNodeRepository).findContentToClean();
        verify(contentNodeRepository, never()).findArchivedByNodeCode(anyString());
        verify(contentNodeRepository, never()).delete(any(ContentNode.class));
    }

    /**
     * Test: Handles error during findContentToClean gracefully.
     */
    @Test
    void cleanContentNodes_ShouldHandleError_WhenFindContentToCleanFails() {
        RuntimeException error = new RuntimeException("Database connection failed");

        when(contentNodeRepository.findContentToClean()).thenReturn(Flux.error(error));

        resourceCleanupScheduler.cleanContentNodes();

        verify(contentNodeRepository).findContentToClean();
        verify(contentNodeRepository, never()).findArchivedByNodeCode(anyString());
        verify(contentNodeRepository, never()).delete(any(ContentNode.class));
    }

    /**
     * Test: Handles error during findArchivedByNodeCode gracefully (fail-fast
     * behavior).
     */
    @Test
    void cleanContentNodes_ShouldHandleError_WhenFindArchivedByNodeCodeFails() {
        String code = "TEST_CODE";
        int maxVersionsToKeep = 2;

        ContentNode activeContent = createContentNode(code, maxVersionsToKeep, 4000L);
        RuntimeException error = new RuntimeException("Archived retrieval failed");

        when(contentNodeRepository.findContentToClean()).thenReturn(Flux.just(activeContent));
        when(contentNodeRepository.findArchivedByNodeCode(code)).thenReturn(Flux.error(error));

        resourceCleanupScheduler.cleanContentNodes();

        verify(contentNodeRepository).findContentToClean();
        verify(contentNodeRepository).findArchivedByNodeCode(code);
        verify(contentNodeRepository, never()).delete(any(ContentNode.class));
    }

    /**
     * Test: Does not delete when archived content count is less than or equal to
     * maxVersionsToKeep.
     */
    @Test
    void cleanContentNodes_ShouldNotDeleteAnything_WhenArchivedCountIsLessThanOrEqualToMax() {
        String code = "TEST_CODE";
        int maxVersionsToKeep = 3;

        ContentNode activeContent = createContentNode(code, maxVersionsToKeep, 4000L);
        ContentNode archivedContent1 = createContentNode(code, maxVersionsToKeep, 3000L);
        ContentNode archivedContent2 = createContentNode(code, maxVersionsToKeep, 2000L);
        ContentNode archivedContent3 = createContentNode(code, maxVersionsToKeep, 1000L);

        when(contentNodeRepository.findContentToClean()).thenReturn(Flux.just(activeContent));
        when(contentNodeRepository.findArchivedByNodeCode(code))
                .thenReturn(Flux.just(archivedContent1, archivedContent2, archivedContent3));

        resourceCleanupScheduler.cleanContentNodes();

        verify(contentNodeRepository).findContentToClean();
        verify(contentNodeRepository).findArchivedByNodeCode(code);
        verify(contentNodeRepository, never()).delete(any(ContentNode.class));
    }

    /**
     * Test: Verifies sorting order (newest first, oldest deleted).
     */
    @Test
    void cleanContentNodes_ShouldSortByModificationDateDescending() {
        String code = "TEST_CODE";
        int maxVersionsToKeep = 1;

        ContentNode activeContent = createContentNode(code, maxVersionsToKeep, 5000L);
        ContentNode archivedContentNewest = createContentNode(code, maxVersionsToKeep, 4000L);
        ContentNode archivedContentMiddle = createContentNode(code, maxVersionsToKeep, 3000L);
        ContentNode archivedContentOldest = createContentNode(code, maxVersionsToKeep, 2000L);

        when(contentNodeRepository.findContentToClean()).thenReturn(Flux.just(activeContent));
        when(contentNodeRepository.findArchivedByNodeCode(code)).thenReturn(Flux.just(
                archivedContentNewest, archivedContentMiddle, archivedContentOldest));
        when(contentNodeRepository.delete(any(ContentNode.class))).thenReturn(Mono.empty());

        resourceCleanupScheduler.cleanContentNodes();

        verify(contentNodeRepository).findContentToClean();
        verify(contentNodeRepository).findArchivedByNodeCode(code);
        // Should keep the newest (archivedContentNewest) and delete the rest
        verify(contentNodeRepository, never()).delete(archivedContentNewest);
        verify(contentNodeRepository).delete(archivedContentMiddle);
        verify(contentNodeRepository).delete(archivedContentOldest);
    }

    // ==================== HELPER METHODS ====================

    private Node createArchivedNode(String code, Long modificationDate) {
        Node node = new Node();
        node.setCode(code);
        node.setModificationDate(modificationDate);
        return node;
    }

    private ContentNode createContentNode(String code, Integer maxVersionsToKeep, Long modificationDate) {
        ContentNode contentNode = new ContentNode();
        contentNode.setCode(code);
        contentNode.setMaxVersionsToKeep(maxVersionsToKeep);
        contentNode.setModificationDate(modificationDate);
        return contentNode;
    }
}