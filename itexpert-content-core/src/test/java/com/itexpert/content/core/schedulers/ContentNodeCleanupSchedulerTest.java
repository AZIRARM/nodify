package com.itexpert.content.core.schedulers;

import com.itexpert.content.core.repositories.ContentNodeRepository;
import com.itexpert.content.core.repositories.NodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContentNodeCleanupSchedulerTest {

    @Mock
    private ContentNodeRepository contentNodeRepository;

    @Mock
    private NodeRepository nodeRepository;

    @InjectMocks
    private ContentNodeCleanupScheduler contentNodeCleanupScheduler;

    @BeforeEach
    void setUp() {
        // Initialization if needed
    }

    /**
     * Test: Successfully identifies an orphan and deletes it.
     * ContentNode has [CODE1, CODE2], Node only has [CODE1].
     * CODE2 should be deleted.
     */
    @Test
    void cleanNodesWithInvalidParentCodes_ShouldDeleteOrphanedNodes_WhenTheyExist() {
        List<String> codesInContent = List.of("CODE1", "CODE2");
        List<String> existingCodesInNode = List.of("CODE1");
        List<String> expectedOrphans = List.of("CODE2");
        long deletedCount = 1L;

        // 1. Get distinct parent codes from ContentNode
        when(contentNodeRepository.findDistinctParentCodes()).thenReturn(Flux.fromIterable(codesInContent));

        // 2. Check which ones exist in Node collection
        when(nodeRepository.findExistingCodesIn(codesInContent)).thenReturn(Flux.fromIterable(existingCodesInNode));

        // 3. Delete the orphans (CODE2)
        when(contentNodeRepository.deleteByParentCodeIn(expectedOrphans)).thenReturn(Mono.just(deletedCount));

        StepVerifier.create(contentNodeCleanupScheduler.cleanNodesWithInvalidParentCodesReactive())
                .expectNext(deletedCount)
                .verifyComplete();

        verify(contentNodeRepository).findDistinctParentCodes();
        verify(nodeRepository).findExistingCodesIn(codesInContent);
        verify(contentNodeRepository).deleteByParentCodeIn(expectedOrphans);
    }

    /**
     * Test: No deletion should occur if all parentCodes are found in Node
     * collection.
     */
    @Test
    void cleanNodesWithInvalidParentCodes_ShouldNotDelete_WhenAllCodesAreValid() {
        List<String> codes = List.of("CODE1", "CODE2");

        when(contentNodeRepository.findDistinctParentCodes()).thenReturn(Flux.fromIterable(codes));
        when(nodeRepository.findExistingCodesIn(codes)).thenReturn(Flux.fromIterable(codes));

        StepVerifier.create(contentNodeCleanupScheduler.cleanNodesWithInvalidParentCodesReactive())
                .expectNext(0L)
                .verifyComplete();

        verify(contentNodeRepository, never()).deleteByParentCodeIn(anyList());
    }

    /**
     * Test: If ContentNode collection is empty, the process stops early.
     */
    @Test
    void cleanNodesWithInvalidParentCodes_ShouldReturnZero_WhenNoContentNodesExist() {
        when(contentNodeRepository.findDistinctParentCodes()).thenReturn(Flux.empty());

        StepVerifier.create(contentNodeCleanupScheduler.cleanNodesWithInvalidParentCodesReactive())
                .expectNext(0L)
                .verifyComplete();

        verify(nodeRepository, never()).findExistingCodesIn(anyList());
        verify(contentNodeRepository, never()).deleteByParentCodeIn(anyList());
    }

    /**
     * Test: Handles error when Node repository fails.
     */
    @Test
    void cleanNodesWithInvalidParentCodes_ShouldHandleError_WhenNodeRepositoryFails() {
        List<String> codes = List.of("CODE1");
        RuntimeException error = new RuntimeException("Database error");

        when(contentNodeRepository.findDistinctParentCodes()).thenReturn(Flux.fromIterable(codes));
        when(nodeRepository.findExistingCodesIn(anyList())).thenReturn(Flux.error(error));

        StepVerifier.create(contentNodeCleanupScheduler.cleanNodesWithInvalidParentCodesReactive())
                .verifyErrorMatches(e -> e instanceof RuntimeException && e.getMessage().equals("Database error"));

        verify(contentNodeRepository, never()).deleteByParentCodeIn(anyList());
    }

    /**
     * Test: Handles error when deletion operation fails.
     */
    @Test
    void cleanNodesWithInvalidParentCodes_ShouldHandleError_WhenDeleteFails() {
        List<String> codesInContent = List.of("ORPHAN");
        List<String> emptyExisting = List.of();
        RuntimeException error = new RuntimeException("Delete failed");

        when(contentNodeRepository.findDistinctParentCodes()).thenReturn(Flux.fromIterable(codesInContent));
        when(nodeRepository.findExistingCodesIn(codesInContent)).thenReturn(Flux.fromIterable(emptyExisting));
        when(contentNodeRepository.deleteByParentCodeIn(anyList())).thenReturn(Mono.error(error));

        StepVerifier.create(contentNodeCleanupScheduler.cleanNodesWithInvalidParentCodesReactive())
                .verifyErrorMatches(e -> e.getMessage().equals("Delete failed"));
    }

    /**
     * Test: Ensure null or blank parentCodes are filtered out to avoid unnecessary
     * DB checks.
     */
    @Test
    void cleanNodesWithInvalidParentCodes_ShouldFilterInvalidStrings() {
        // We use blank and empty strings.
        // We avoid literal null because Reactor Flux cannot emit null signals.
        List<String> codesWithBlanks = Arrays.asList("CODE1", "", "   ");

        when(contentNodeRepository.findDistinctParentCodes())
                .thenReturn(Flux.fromIterable(codesWithBlanks));

        // The scheduler should filter out "" and " ", leaving only "CODE1"
        when(nodeRepository.findExistingCodesIn(List.of("CODE1")))
                .thenReturn(Flux.just("CODE1"));

        StepVerifier.create(contentNodeCleanupScheduler.cleanNodesWithInvalidParentCodesReactive())
                .expectNext(0L) // No orphans because CODE1 is found in Node
                .verifyComplete();

        // Verify only the valid code was used for the check
        verify(nodeRepository).findExistingCodesIn(List.of("CODE1"));
        verify(contentNodeRepository, never()).deleteByParentCodeIn(anyList());
    }
}