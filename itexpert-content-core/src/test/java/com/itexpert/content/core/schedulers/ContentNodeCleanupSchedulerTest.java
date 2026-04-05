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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

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
    }

    /**
     * Test: Successfully deletes ContentNodes that have invalid parentCode
     * references
     * when valid Node codes exist.
     */
    @Test
    void cleanNodesWithInvalidParentCodes_ShouldDeleteInvalidNodes_WhenThereAreNodesToDelete() {
        List<String> distinctCodes = List.of("CODE1", "CODE2", "CODE3");
        long deletedCount = 5L;

        when(nodeRepository.findDistinctCodes()).thenReturn(Flux.fromIterable(distinctCodes));
        when(contentNodeRepository.deleteByParentCodeNotIn(distinctCodes)).thenReturn(Mono.just(deletedCount));

        StepVerifier.create(contentNodeCleanupScheduler.cleanNodesWithInvalidParentCodesReactive())
                .expectNext(deletedCount)
                .verifyComplete();

        verify(nodeRepository).findDistinctCodes();
        verify(contentNodeRepository).deleteByParentCodeNotIn(distinctCodes);
    }

    /**
     * Test: Prevents mass deletion when Node collection is empty (no distinct codes
     * found).
     * The safety check should return 0 without calling the delete operation.
     */
    @Test
    void cleanNodesWithInvalidParentCodes_ShouldNotDeleteAnything_WhenNoDistinctCodesFound() {
        List<String> distinctCodes = List.of();

        when(nodeRepository.findDistinctCodes()).thenReturn(Flux.fromIterable(distinctCodes));
        // deleteByParentCodeNotIn should NEVER be called due to safety check
        // No need to mock it because it won't be invoked

        StepVerifier.create(contentNodeCleanupScheduler.cleanNodesWithInvalidParentCodesReactive())
                .expectNext(0L)
                .verifyComplete();

        verify(nodeRepository).findDistinctCodes();
        // Verify that deleteByParentCodeNotIn is NEVER called (safety check prevents
        // it)
        verify(contentNodeRepository, never()).deleteByParentCodeNotIn(anyList());
    }

    /**
     * Test: Handles error when Node repository fails (e.g., database connection
     * issue).
     */
    @Test
    void cleanNodesWithInvalidParentCodes_ShouldHandleError_WhenNodeRepositoryFails() {
        RuntimeException error = new RuntimeException("Database connection failed");

        when(nodeRepository.findDistinctCodes()).thenReturn(Flux.error(error));

        StepVerifier.create(contentNodeCleanupScheduler.cleanNodesWithInvalidParentCodesReactive())
                .verifyErrorMatches(
                        e -> e instanceof RuntimeException && e.getMessage().equals("Database connection failed"));

        verify(nodeRepository).findDistinctCodes();
        verify(contentNodeRepository, never()).deleteByParentCodeNotIn(any());
    }

    /**
     * Test: Handles error when ContentNode repository delete operation fails.
     */
    @Test
    void cleanNodesWithInvalidParentCodes_ShouldHandleError_WhenContentNodeRepositoryFails() {
        List<String> distinctCodes = List.of("CODE1", "CODE2");
        RuntimeException error = new RuntimeException("Delete operation failed");

        when(nodeRepository.findDistinctCodes()).thenReturn(Flux.fromIterable(distinctCodes));
        when(contentNodeRepository.deleteByParentCodeNotIn(distinctCodes)).thenReturn(Mono.error(error));

        StepVerifier.create(contentNodeCleanupScheduler.cleanNodesWithInvalidParentCodesReactive())
                .verifyErrorMatches(
                        e -> e instanceof RuntimeException && e.getMessage().equals("Delete operation failed"));

        verify(nodeRepository).findDistinctCodes();
        verify(contentNodeRepository).deleteByParentCodeNotIn(distinctCodes);
    }

    /**
     * Test: Handles null/empty scenario from Node repository.
     * The safety check should prevent deletion and return 0.
     */
    @Test
    void cleanNodesWithInvalidParentCodes_ShouldNotDeleteAnything_WhenNullDistinctCodes() {
        when(nodeRepository.findDistinctCodes()).thenReturn(Flux.empty());
        // deleteByParentCodeNotIn should NEVER be called due to safety check
        // No need to mock it because it won't be invoked

        StepVerifier.create(contentNodeCleanupScheduler.cleanNodesWithInvalidParentCodesReactive())
                .expectNext(0L)
                .verifyComplete();

        verify(nodeRepository).findDistinctCodes();
        // Verify that deleteByParentCodeNotIn is NEVER called (safety check prevents
        // it)
        verify(contentNodeRepository, never()).deleteByParentCodeNotIn(anyList());
    }
}