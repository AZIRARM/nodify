package com.itexpert.content.api.endpoints;

import com.itexpert.content.api.handlers.FeedbackHandler;
import com.itexpert.content.lib.models.Feedback;
import com.itexpert.content.lib.models.FeedbackCharts;
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

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeedbackEndPointTest {

    @Mock
    private FeedbackHandler feedbackHandler;

    @InjectMocks
    private FeedbackEndPoint feedbackEndPoint;

    private Feedback feedback;
    private FeedbackCharts feedbackCharts;
    private UUID id;
    private String contentCode;
    private String userId;
    private int evaluation;
    private boolean verified;

    @BeforeEach
    void setUp() {
        id = UUID.randomUUID();
        contentCode = "test-content-code";
        userId = "test-user-id";
        evaluation = 5;
        verified = true;

        feedback = new Feedback();
        feedback.setId(id);
        feedback.setContentCode(contentCode);
        feedback.setUserId(userId);
        feedback.setEvaluation(evaluation);
        feedback.setVerified(verified);
        feedback.setMessage("Great content!");
        feedback.setCreatedDate(System.currentTimeMillis());
        feedback.setModifiedDate(System.currentTimeMillis());

        feedbackCharts = new FeedbackCharts();
        feedbackCharts.setContentCode(contentCode);
    }

    @Test
    void findByContentCodeShouldReturnFeedbacks() {
        when(feedbackHandler.findByContentCode(contentCode, 0, 50)).thenReturn(Flux.just(feedback));

        StepVerifier.create(feedbackEndPoint.findByContentCode(contentCode, 0, 50))
                .expectNext(feedback)
                .verifyComplete();
    }

    @Test
    void findByContentCodeShouldReturnEmpty() {
        when(feedbackHandler.findByContentCode(contentCode, 0, 50)).thenReturn(Flux.empty());

        StepVerifier.create(feedbackEndPoint.findByContentCode(contentCode, 0, 50))
                .verifyComplete();
    }

    @Test
    void findByUserIdShouldReturnFeedbacks() {
        when(feedbackHandler.findByUserId(userId, 0, 50)).thenReturn(Flux.just(feedback));

        StepVerifier.create(feedbackEndPoint.findByUserId(userId, 0, 50))
                .expectNext(feedback)
                .verifyComplete();
    }

    @Test
    void findByUserIdShouldReturnEmpty() {
        when(feedbackHandler.findByUserId(userId, 0, 50)).thenReturn(Flux.empty());

        StepVerifier.create(feedbackEndPoint.findByUserId(userId, 0, 50))
                .verifyComplete();
    }

    @Test
    void findByEvaluationShouldReturnFeedbacks() {
        when(feedbackHandler.findByEvaluation(evaluation, 0, 50)).thenReturn(Flux.just(feedback));

        StepVerifier.create(feedbackEndPoint.findByEvaluation(evaluation, 0, 50))
                .expectNext(feedback)
                .verifyComplete();
    }

    @Test
    void findByEvaluationShouldReturnEmpty() {
        when(feedbackHandler.findByEvaluation(evaluation, 0, 50)).thenReturn(Flux.empty());

        StepVerifier.create(feedbackEndPoint.findByEvaluation(evaluation, 0, 50))
                .verifyComplete();
    }

    @Test
    void findByVerifiedShouldReturnFeedbacks() {
        when(feedbackHandler.findByVerified(verified, 0, 50)).thenReturn(Flux.just(feedback));

        StepVerifier.create(feedbackEndPoint.findByVerified(verified, 0, 50))
                .expectNext(feedback)
                .verifyComplete();
    }

    @Test
    void findByVerifiedShouldReturnEmpty() {
        when(feedbackHandler.findByVerified(verified, 0, 50)).thenReturn(Flux.empty());

        StepVerifier.create(feedbackEndPoint.findByVerified(verified, 0, 50))
                .verifyComplete();
    }

    @Test
    void saveShouldReturnSavedFeedback() {
        when(feedbackHandler.save(feedback)).thenReturn(Mono.just(feedback));

        StepVerifier.create(feedbackEndPoint.save(feedback))
                .expectNext(ResponseEntity.ok(feedback))
                .verifyComplete();
    }

    @Test
    void deleteAllByContentNodeCodeShouldReturnTrue() {
        when(feedbackHandler.deleteAllByContentNodeCode(contentCode)).thenReturn(Mono.just(true));

        StepVerifier.create(feedbackEndPoint.deleteAllByContentNodeCode(contentCode))
                .expectNext(ResponseEntity.ok(true))
                .verifyComplete();
    }

    @Test
    void deleteShouldReturnTrue() {
        when(feedbackHandler.delete(id)).thenReturn(Mono.just(true));

        StepVerifier.create(feedbackEndPoint.delete(id.toString()))
                .expectNext(ResponseEntity.ok(true))
                .verifyComplete();
    }

    @Test
    void getChartsShouldReturnFeedbackCharts() {
        when(feedbackHandler.getCharts()).thenReturn(Flux.just(feedbackCharts));

        StepVerifier.create(feedbackEndPoint.getCharts())
                .expectNext(feedbackCharts)
                .verifyComplete();
    }

    @Test
    void getChartsShouldReturnEmpty() {
        when(feedbackHandler.getCharts()).thenReturn(Flux.empty());

        StepVerifier.create(feedbackEndPoint.getCharts())
                .verifyComplete();
    }
}