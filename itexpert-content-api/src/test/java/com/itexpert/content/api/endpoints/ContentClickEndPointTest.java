package com.itexpert.content.api.endpoints;

import com.itexpert.content.api.handlers.ContentClickHandler;
import com.itexpert.content.lib.models.ContentClick;
import com.itexpert.content.lib.models.ContentClickCharts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContentClickEndPointTest {

    @Mock
    private ContentClickHandler contentClickHandler;

    @InjectMocks
    private ContentClickEndPoint contentClickEndPoint;

    private ContentClick contentClick;
    private ContentClickCharts contentClickCharts;
    private UUID id;
    private String contentCode;

    @BeforeEach
    void setUp() {
        id = UUID.randomUUID();
        contentCode = "test-content-code";

        contentClick = new ContentClick();
        contentClick.setId(id);
        contentClick.setContentCode(contentCode);
        contentClick.setClicks(10L);

        contentClickCharts = new ContentClickCharts("test-content-code", "10");
    }

    @Test
    void findAllShouldReturnAllContentClicks() {
        when(contentClickHandler.findAll()).thenReturn(Flux.just(contentClick));

        StepVerifier.create(contentClickEndPoint.findAll())
                .expectNext(contentClick)
                .verifyComplete();
    }

    @Test
    void findAllShouldReturnEmptyWhenNoContentClicks() {
        when(contentClickHandler.findAll()).thenReturn(Flux.empty());

        StepVerifier.create(contentClickEndPoint.findAll())
                .verifyComplete();
    }

    @Test
    void findByIdShouldReturnContentClickWhenFound() {
        when(contentClickHandler.findById(id)).thenReturn(Mono.just(contentClick));

        StepVerifier.create(contentClickEndPoint.findById(id))
                .expectNext(ResponseEntity.ok(contentClick))
                .verifyComplete();
    }

    @Test
    void findByIdShouldReturnNotFoundWhenNotFound() {
        when(contentClickHandler.findById(id)).thenReturn(Mono.empty());

        StepVerifier.create(contentClickEndPoint.findById(id))
                .expectNext(new ResponseEntity<>(HttpStatus.NOT_FOUND))
                .verifyComplete();
    }

    @Test
    void findByContentCodeShouldReturnContentClick() {
        when(contentClickHandler.findByContentCode(contentCode)).thenReturn(Mono.just(contentClick));

        StepVerifier.create(contentClickEndPoint.findByContentCode(contentCode))
                .expectNext(contentClick)
                .verifyComplete();
    }

    @Test
    void findByContentCodeShouldReturnEmptyWhenNotFound() {
        when(contentClickHandler.findByContentCode(contentCode)).thenReturn(Mono.empty());

        StepVerifier.create(contentClickEndPoint.findByContentCode(contentCode))
                .verifyComplete();
    }

    @Test
    void saveShouldRecordContentClick() {
        when(contentClickHandler.addClick(contentCode)).thenReturn(Mono.just(true));

        StepVerifier.create(contentClickEndPoint.save(contentCode))
                .expectNext(ResponseEntity.ok(true))
                .verifyComplete();
    }

    @Test
    void deleteShouldDeleteContentClick() {
        when(contentClickHandler.delete(id)).thenReturn(Mono.just(true));

        StepVerifier.create(contentClickEndPoint.delete(id.toString()))
                .expectNext(ResponseEntity.ok(true))
                .verifyComplete();
    }

    @Test
    void getChartsShouldReturnContentClickCharts() {
        when(contentClickHandler.getCharts()).thenReturn(Flux.just(contentClickCharts));

        StepVerifier.create(contentClickEndPoint.getCharts())
                .expectNext(contentClickCharts)
                .verifyComplete();
    }

    @Test
    void getChartsShouldReturnEmptyWhenNoCharts() {
        when(contentClickHandler.getCharts()).thenReturn(Flux.empty());

        StepVerifier.create(contentClickEndPoint.getCharts())
                .verifyComplete();
    }
}