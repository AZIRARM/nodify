package com.itexpert.content.api.endpoints;

import com.itexpert.content.api.handlers.ContentDisplayHandler;
import com.itexpert.content.lib.models.ContentDisplay;
import com.itexpert.content.lib.models.ContentDisplayCharts;
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
class ContentDisplayEndPointTest {

    @Mock
    private ContentDisplayHandler contentDisplayHandler;

    @InjectMocks
    private ContentDisplayEndPoint contentDisplayEndPoint;

    private ContentDisplay contentDisplay;
    private ContentDisplayCharts contentDisplayCharts;
    private UUID id;
    private String contentCode;

    @BeforeEach
    void setUp() {
        id = UUID.randomUUID();
        contentCode = "test-content-code";

        contentDisplay = new ContentDisplay();
        contentDisplay.setId(id);
        contentDisplay.setContentCode(contentCode);
        contentDisplay.setDisplays(10L);

        contentDisplayCharts = new ContentDisplayCharts("test-content-code", "10");
    }

    @Test
    void findAllShouldReturnAllContentDisplays() {
        when(contentDisplayHandler.findAll()).thenReturn(Flux.just(contentDisplay));

        StepVerifier.create(contentDisplayEndPoint.findAll())
                .expectNext(contentDisplay)
                .verifyComplete();
    }

    @Test
    void findAllShouldReturnEmptyWhenNoContentDisplays() {
        when(contentDisplayHandler.findAll()).thenReturn(Flux.empty());

        StepVerifier.create(contentDisplayEndPoint.findAll())
                .verifyComplete();
    }

    @Test
    void findByIdShouldReturnContentDisplayWhenFound() {
        when(contentDisplayHandler.findById(id)).thenReturn(Mono.just(contentDisplay));

        StepVerifier.create(contentDisplayEndPoint.findById(id))
                .expectNext(ResponseEntity.ok(contentDisplay))
                .verifyComplete();
    }

    @Test
    void findByIdShouldReturnNotFoundWhenNotFound() {
        when(contentDisplayHandler.findById(id)).thenReturn(Mono.empty());

        StepVerifier.create(contentDisplayEndPoint.findById(id))
                .expectNext(new ResponseEntity<>(HttpStatus.NOT_FOUND))
                .verifyComplete();
    }

    @Test
    void findByContentCodeShouldReturnContentDisplay() {
        when(contentDisplayHandler.findByContentCode(contentCode)).thenReturn(Mono.just(contentDisplay));

        StepVerifier.create(contentDisplayEndPoint.findByContentCode(contentCode))
                .expectNext(contentDisplay)
                .verifyComplete();
    }

    @Test
    void findByContentCodeShouldReturnEmptyWhenNotFound() {
        when(contentDisplayHandler.findByContentCode(contentCode)).thenReturn(Mono.empty());

        StepVerifier.create(contentDisplayEndPoint.findByContentCode(contentCode))
                .verifyComplete();
    }

    @Test
    void addDisplayShouldIncrementDisplayCount() {
        when(contentDisplayHandler.addDisplay(contentCode)).thenReturn(Mono.just(true));

        StepVerifier.create(contentDisplayEndPoint.addDisplay(contentCode))
                .expectNext(ResponseEntity.ok(true))
                .verifyComplete();
    }

    @Test
    void deleteShouldDeleteContentDisplay() {
        when(contentDisplayHandler.delete(id)).thenReturn(Mono.just(true));

        StepVerifier.create(contentDisplayEndPoint.delete(id.toString()))
                .expectNext(ResponseEntity.ok(true))
                .verifyComplete();
    }

    @Test
    void getChartsShouldReturnContentDisplayCharts() {
        when(contentDisplayHandler.getCharts()).thenReturn(Flux.just(contentDisplayCharts));

        StepVerifier.create(contentDisplayEndPoint.getCharts())
                .expectNext(contentDisplayCharts)
                .verifyComplete();
    }

    @Test
    void getChartsShouldReturnEmptyWhenNoCharts() {
        when(contentDisplayHandler.getCharts()).thenReturn(Flux.empty());

        StepVerifier.create(contentDisplayEndPoint.getCharts())
                .verifyComplete();
    }
}