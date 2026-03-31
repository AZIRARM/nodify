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

    @Test
    void cleanNodesWithInvalidParentCodes_ShouldDeleteNothing_WhenNoDistinctCodesFound() {
        List<String> distinctCodes = List.of();

        when(nodeRepository.findDistinctCodes()).thenReturn(Flux.fromIterable(distinctCodes));
        when(contentNodeRepository.deleteByParentCodeNotIn(distinctCodes)).thenReturn(Mono.just(0L));

        StepVerifier.create(contentNodeCleanupScheduler.cleanNodesWithInvalidParentCodesReactive())
                .expectNext(0L)
                .verifyComplete();

        verify(nodeRepository).findDistinctCodes();
        verify(contentNodeRepository).deleteByParentCodeNotIn(distinctCodes);
    }

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

    @Test
    void cleanNodesWithInvalidParentCodes_ShouldHandleNullDistinctCodes() {
        when(nodeRepository.findDistinctCodes()).thenReturn(Flux.empty());
        when(contentNodeRepository.deleteByParentCodeNotIn(List.of())).thenReturn(Mono.just(0L));

        StepVerifier.create(contentNodeCleanupScheduler.cleanNodesWithInvalidParentCodesReactive())
                .expectNext(0L)
                .verifyComplete();

        verify(nodeRepository).findDistinctCodes();
        verify(contentNodeRepository).deleteByParentCodeNotIn(List.of());
    }
}