package com.itexpert.content.api.endpoints;

import com.itexpert.content.api.handlers.DataHandler;
import com.itexpert.content.lib.models.Data;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataEndPointTest {

    @Mock
    private DataHandler dataHandler;

    @InjectMocks
    private DataEndPoint dataEndPoint;

    private Data data;
    private UUID id;
    private String code;
    private String key;
    private String name;
    private String dataType;
    private String user;

    @BeforeEach
    void setUp() {
        id = UUID.randomUUID();
        code = "test-code";
        key = "test-key";
        name = "test-name";
        dataType = "string";
        user = "test-user";

        data = new Data();
        data.setId(id);
        data.setContentNodeCode(code);
        data.setKey(key);
        data.setName(name);
        data.setDataType(dataType);
        data.setUser(user);
        data.setValue("test-value");
        data.setCreationDate(123456789L);
        data.setModificationDate(987654321L);
    }

    @Test
    void findByContentCodeAndKeyShouldReturnData() {
        when(dataHandler.findByContentNodeCodeAndKey(code, key)).thenReturn(Mono.just(data));

        StepVerifier.create(dataEndPoint.findByContentCodeAndKey(code, key))
                .expectNext(data)
                .verifyComplete();
    }

    @Test
    void findByContentCodeAndKeyShouldReturnEmpty() {
        when(dataHandler.findByContentNodeCodeAndKey(code, key)).thenReturn(Mono.empty());

        StepVerifier.create(dataEndPoint.findByContentCodeAndKey(code, key))
                .verifyComplete();
    }

    @Test
    void findByContentCodeAndNameShouldReturnData() {
        when(dataHandler.findByContentNodeCodeAndName(code, name)).thenReturn(Flux.just(data));

        StepVerifier.create(dataEndPoint.findByContentCodeAndName(code, name))
                .expectNext(data)
                .verifyComplete();
    }

    @Test
    void findByContentCodeAndNameShouldReturnEmpty() {
        when(dataHandler.findByContentNodeCodeAndName(code, name)).thenReturn(Flux.empty());

        StepVerifier.create(dataEndPoint.findByContentCodeAndName(code, name))
                .verifyComplete();
    }

    @Test
    void findByContentCodeShouldReturnData() {
        when(dataHandler.findByContentCode(code, 0, 50)).thenReturn(Flux.just(data));

        StepVerifier.create(dataEndPoint.findByContentCode(code, 0, 50))
                .expectNext(data)
                .verifyComplete();
    }

    @Test
    void findByContentCodeShouldReturnEmpty() {
        when(dataHandler.findByContentCode(code, 0, 50)).thenReturn(Flux.empty());

        StepVerifier.create(dataEndPoint.findByContentCode(code, 0, 50))
                .verifyComplete();
    }

    @Test
    void countByContentCodeShouldReturnCount() {
        when(dataHandler.countByContentCode(code)).thenReturn(Mono.just(5L));

        StepVerifier.create(dataEndPoint.countByContentCode(code))
                .expectNext(5L)
                .verifyComplete();
    }

    @Test
    void countAllShouldReturnTotalCount() {
        when(dataHandler.countAll()).thenReturn(Mono.just(100L));

        StepVerifier.create(dataEndPoint.countAll())
                .expectNext(100L)
                .verifyComplete();
    }

    @Test
    void countByContentCodeAndDataTypeShouldReturnCount() {
        when(dataHandler.countByContentNodeCodeAndDataType(code, dataType)).thenReturn(Mono.just(3L));

        StepVerifier.create(dataEndPoint.countByContentCodeAndDataType(code, dataType))
                .expectNext(3L)
                .verifyComplete();
    }

    @Test
    void countByContentCodeAndUserShouldReturnCount() {
        when(dataHandler.countByContentNodeCodeAndUser(code, user)).thenReturn(Mono.just(2L));

        StepVerifier.create(dataEndPoint.countByContentCodeAndUser(code, user))
                .expectNext(2L)
                .verifyComplete();
    }

    @Test
    void countByDataTypeShouldReturnCount() {
        when(dataHandler.countByDataType(dataType)).thenReturn(Mono.just(10L));

        StepVerifier.create(dataEndPoint.countByDataType(dataType))
                .expectNext(10L)
                .verifyComplete();
    }

    @Test
    void countByUserShouldReturnCount() {
        when(dataHandler.countByUser(user)).thenReturn(Mono.just(4L));

        StepVerifier.create(dataEndPoint.countByUser(user))
                .expectNext(4L)
                .verifyComplete();
    }

    @Test
    void findByIdShouldReturnData() {
        when(dataHandler.findById(id)).thenReturn(Mono.just(data));

        StepVerifier.create(dataEndPoint.findById(id.toString()))
                .expectNext(data)
                .verifyComplete();
    }

    @Test
    void findByIdShouldReturnEmpty() {
        when(dataHandler.findById(id)).thenReturn(Mono.empty());

        StepVerifier.create(dataEndPoint.findById(id.toString()))
                .verifyComplete();
    }

    @Test
    void findAllShouldReturnData() {
        when(dataHandler.findAll(0, 50)).thenReturn(Flux.just(data));

        StepVerifier.create(dataEndPoint.findAll(0, 50))
                .expectNext(data)
                .verifyComplete();
    }

    @Test
    void findAllShouldReturnEmpty() {
        when(dataHandler.findAll(0, 50)).thenReturn(Flux.empty());

        StepVerifier.create(dataEndPoint.findAll(0, 50))
                .verifyComplete();
    }

    @Test
    void findByDataTypeShouldReturnData() {
        when(dataHandler.findByDataType(dataType, 0, 50)).thenReturn(Flux.just(data));

        StepVerifier.create(dataEndPoint.findByDataType(dataType, 0, 50))
                .expectNext(data)
                .verifyComplete();
    }

    @Test
    void findByDataTypeShouldReturnEmpty() {
        when(dataHandler.findByDataType(dataType, 0, 50)).thenReturn(Flux.empty());

        StepVerifier.create(dataEndPoint.findByDataType(dataType, 0, 50))
                .verifyComplete();
    }

    @Test
    void findByUserShouldReturnData() {
        when(dataHandler.findByUser(user, 0, 50)).thenReturn(Flux.just(data));

        StepVerifier.create(dataEndPoint.findByUser(user, 0, 50))
                .expectNext(data)
                .verifyComplete();
    }

    @Test
    void findByUserShouldReturnEmpty() {
        when(dataHandler.findByUser(user, 0, 50)).thenReturn(Flux.empty());

        StepVerifier.create(dataEndPoint.findByUser(user, 0, 50))
                .verifyComplete();
    }

    @Test
    void searchByKeywordShouldReturnData() {
        when(dataHandler.searchByKeyword(code, "keyword", 0, 50)).thenReturn(Flux.just(data));

        StepVerifier.create(dataEndPoint.searchByKeyword(code, "keyword", 0, 50))
                .expectNext(data)
                .verifyComplete();
    }

    @Test
    void searchByKeywordShouldReturnEmpty() {
        when(dataHandler.searchByKeyword(code, "keyword", 0, 50)).thenReturn(Flux.empty());

        StepVerifier.create(dataEndPoint.searchByKeyword(code, "keyword", 0, 50))
                .verifyComplete();
    }

    @Test
    void updateShouldReturnUpdatedData() {
        when(dataHandler.update(id, data)).thenReturn(Mono.just(data));

        StepVerifier.create(dataEndPoint.update(id.toString(), data))
                .expectNext(ResponseEntity.ok(data))
                .verifyComplete();
    }

    @Test
    void saveShouldReturnSavedData() {
        when(dataHandler.save(data)).thenReturn(Mono.just(data));

        StepVerifier.create(dataEndPoint.save(data))
                .expectNext(ResponseEntity.ok(data))
                .verifyComplete();
    }

    @Test
    void deleteAllByContentNodeCodeShouldReturnTrue() {
        when(dataHandler.deleteAllByContentNodeCode(code)).thenReturn(Mono.just(true));

        StepVerifier.create(dataEndPoint.deleteAllByContentNodeCode(code))
                .expectNext(ResponseEntity.ok(true))
                .verifyComplete();
    }

    @Test
    void deleteByContentCodeAndKeyShouldReturnTrue() {
        when(dataHandler.deleteByContentNodeCodeAndKey(code, key)).thenReturn(Mono.just(true));

        StepVerifier.create(dataEndPoint.deleteByContentCodeAndKey(code, key))
                .expectNext(ResponseEntity.ok(true))
                .verifyComplete();
    }

    @Test
    void deleteShouldReturnTrue() {
        when(dataHandler.delete(id)).thenReturn(Mono.just(true));

        StepVerifier.create(dataEndPoint.delete(id.toString()))
                .expectNext(ResponseEntity.ok(true))
                .verifyComplete();
    }
}