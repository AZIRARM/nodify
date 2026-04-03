package com.itexpert.content.api.handlers;

import com.itexpert.content.api.mappers.ContentDisplayMapper;
import com.itexpert.content.api.repositories.ContentDisplayRepository;
import com.itexpert.content.api.repositories.ContentNodeRepository;
import com.itexpert.content.lib.enums.StatusEnum;
import com.itexpert.content.lib.models.ContentDisplay;
import com.itexpert.content.lib.models.ContentDisplayCharts;
import org.apache.commons.lang3.ObjectUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.*;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ContentDisplayHandlerTest {

    @Mock
    private ContentDisplayRepository contentDisplayRepository;

    @Mock
    private ContentNodeRepository contentNodeRepository;

    @Mock
    private ContentDisplayMapper contentDisplayMapper;

    @InjectMocks
    private ContentDisplayHandler contentDisplayHandler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testFindAll() {
        List<com.itexpert.content.lib.entities.ContentDisplay> entities = Arrays.asList(
                new com.itexpert.content.lib.entities.ContentDisplay(),
                new com.itexpert.content.lib.entities.ContentDisplay());
        List<ContentDisplay> models = Arrays.asList(
                new ContentDisplay(), new ContentDisplay());

        when(contentDisplayRepository.findAll()).thenReturn(Flux.fromIterable(entities));
        when(contentDisplayMapper.fromEntity(any(com.itexpert.content.lib.entities.ContentDisplay.class)))
                .thenReturn(models.get(0), models.get(1));

        StepVerifier.create(contentDisplayHandler.findAll())
                .expectNext(models.get(0))
                .expectNext(models.get(1))
                .verifyComplete();

        verify(contentDisplayRepository).findAll();
        verify(contentDisplayMapper, times(2)).fromEntity(any(com.itexpert.content.lib.entities.ContentDisplay.class));
    }

    @Test
    void testFindByContentCode() {
        String code = "code123";
        com.itexpert.content.lib.entities.ContentDisplay entity = new com.itexpert.content.lib.entities.ContentDisplay();
        ContentDisplay model = new ContentDisplay();

        when(contentDisplayRepository.findByContentCode(code)).thenReturn(Mono.just(entity));
        when(contentDisplayMapper.fromEntity(entity)).thenReturn(model);

        StepVerifier.create(contentDisplayHandler.findByContentCode(code))
                .expectNext(model)
                .verifyComplete();

        verify(contentDisplayRepository).findByContentCode(code);
        verify(contentDisplayMapper).fromEntity(entity);
    }

    @Test
    void testFindById() {
        UUID id = UUID.randomUUID();
        com.itexpert.content.lib.entities.ContentDisplay entity = new com.itexpert.content.lib.entities.ContentDisplay();
        ContentDisplay model = new ContentDisplay();

        when(contentDisplayRepository.findById(id)).thenReturn(Mono.just(entity));
        when(contentDisplayMapper.fromEntity(entity)).thenReturn(model);

        StepVerifier.create(contentDisplayHandler.findById(id))
                .expectNext(model)
                .verifyComplete();

        verify(contentDisplayRepository).findById(id);
        verify(contentDisplayMapper).fromEntity(entity);
    }

    @Test
    void testAddDisplay_NewContentDisplay() {
        String code = "newCode";
        com.itexpert.content.lib.entities.ContentDisplay emptyEntity = new com.itexpert.content.lib.entities.ContentDisplay();
        // Simulate empty id to trigger creation
        emptyEntity.setId(null);
        emptyEntity.setDisplays(null);

        com.itexpert.content.lib.entities.ContentDisplay savedEntity = new com.itexpert.content.lib.entities.ContentDisplay();
        savedEntity.setId(UUID.randomUUID());
        savedEntity.setDisplays(1L);
        savedEntity.setContentCode(code);

        when(contentDisplayRepository.findByContentCode(code)).thenReturn(Mono.empty());
        when(contentDisplayRepository.save(any(com.itexpert.content.lib.entities.ContentDisplay.class)))
                .thenReturn(Mono.just(savedEntity));

        StepVerifier.create(contentDisplayHandler.addDisplay(code))
                .expectNext(true)
                .verifyComplete();

        verify(contentDisplayRepository).findByContentCode(code);
        verify(contentDisplayRepository).save(any(com.itexpert.content.lib.entities.ContentDisplay.class));
    }

    @Test
    void testAddDisplay_ExistingContentDisplay() {
        String code = "existingCode";
        com.itexpert.content.lib.entities.ContentDisplay existingEntity = new com.itexpert.content.lib.entities.ContentDisplay();
        existingEntity.setId(UUID.randomUUID());
        existingEntity.setDisplays(5L);
        existingEntity.setContentCode(code);

        com.itexpert.content.lib.entities.ContentDisplay savedEntity = new com.itexpert.content.lib.entities.ContentDisplay();
        savedEntity.setId(existingEntity.getId());
        savedEntity.setDisplays(6L);
        savedEntity.setContentCode(code);

        when(contentDisplayRepository.findByContentCode(code)).thenReturn(Mono.just(existingEntity));
        when(contentDisplayRepository.save(any(com.itexpert.content.lib.entities.ContentDisplay.class)))
                .thenReturn(Mono.just(savedEntity));

        StepVerifier.create(contentDisplayHandler.addDisplay(code))
                .expectNext(true)
                .verifyComplete();

        verify(contentDisplayRepository).findByContentCode(code);
        verify(contentDisplayRepository).save(any(com.itexpert.content.lib.entities.ContentDisplay.class));
    }

    @Test
    void testSaveAll() {
        List<ContentDisplay> models = Arrays.asList(new ContentDisplay(), new ContentDisplay());
        List<com.itexpert.content.lib.entities.ContentDisplay> entities = models.stream()
                .map(m -> new com.itexpert.content.lib.entities.ContentDisplay())
                .collect(Collectors.toList());

        when(contentDisplayMapper.fromModel(any(ContentDisplay.class)))
                .thenReturn(new com.itexpert.content.lib.entities.ContentDisplay());
        when(contentDisplayRepository.saveAll(anyList())).thenReturn(Flux.fromIterable(entities));

        StepVerifier.create(contentDisplayHandler.saveAll(models))
                .expectNext((long) entities.size())
                .verifyComplete();

        verify(contentDisplayMapper, times(models.size())).fromModel(any(ContentDisplay.class));
        verify(contentDisplayRepository).saveAll(anyList());
    }

    @Test
    void testDelete_Success() {
        UUID id = UUID.randomUUID();

        when(contentDisplayRepository.deleteById(id)).thenReturn(Mono.empty());

        StepVerifier.create(contentDisplayHandler.delete(id))
                .verifyComplete();

        verify(contentDisplayRepository).deleteById(id);
    }

    @Test
    void testDelete_Error() {
        UUID id = UUID.randomUUID();

        when(contentDisplayRepository.deleteById(id)).thenReturn(Mono.error(new RuntimeException("fail")));

        StepVerifier.create(contentDisplayHandler.delete(id))
                .expectNext(false)
                .verifyComplete();

        verify(contentDisplayRepository).deleteById(id);
    }
}
