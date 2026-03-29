package com.itexpert.content.api.handlers;

import com.itexpert.content.api.mappers.DataMapper;
import com.itexpert.content.api.repositories.ContentNodeRepository;
import com.itexpert.content.api.repositories.DataRepository;
import com.itexpert.content.lib.models.Data;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataHandlerTest {

    @Mock
    private DataRepository dataRepository;

    @Mock
    private ContentNodeRepository contentNodeRepository;

    @Mock
    private DataMapper dataMapper;

    @InjectMocks
    private DataHandler dataHandler;

    private Data data;
    private com.itexpert.content.lib.entities.Data dataEntity;

    @BeforeEach
    void setUp() {
        data = new Data();
        data.setId(UUID.randomUUID());
        data.setKey("sample-key");
        data.setContentNodeCode("sample-code");

        dataEntity = new com.itexpert.content.lib.entities.Data();
        dataEntity.setId(data.getId());
        dataEntity.setKey("sample-key");
        dataEntity.setContentNodeCode("sample-code");
    }

    @Test
    void testFindById() {
        when(dataRepository.findById(data.getId())).thenReturn(Mono.just(dataEntity));
        when(dataMapper.fromEntity(dataEntity)).thenReturn(data);

        StepVerifier.create(dataHandler.findById(data.getId()))
                .expectNext(data)
                .verifyComplete();
    }

    @Test
    void testSaveNewData() {
        Data newData = new Data();
        when(dataMapper.fromModel(newData)).thenReturn(dataEntity);
        when(dataRepository.save(dataEntity)).thenReturn(Mono.just(dataEntity));
        when(dataMapper.fromEntity(dataEntity)).thenReturn(data);

        StepVerifier.create(dataHandler.save(newData))
                .expectNext(data)
                .verifyComplete();
    }

    @Test
    void testCountAll() {
        when(dataRepository.count()).thenReturn(Mono.just(10L));

        StepVerifier.create(dataHandler.countAll())
                .expectNext(10L)
                .verifyComplete();
    }

    @Test
    void testDelete() {
        when(dataRepository.deleteById(data.getId())).thenReturn(Mono.empty());

        StepVerifier.create(dataHandler.delete(data.getId()))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void testFindByContentCode() {
        when(dataRepository.findByContentNodeCode(eq("sample-code"), any(PageRequest.class)))
                .thenReturn(Flux.just(dataEntity));
        when(dataMapper.fromEntity(dataEntity)).thenReturn(data);

        StepVerifier.create(dataHandler.findByContentCode("sample-code", 0, 10))
                .expectNext(data)
                .verifyComplete();
    }

}
