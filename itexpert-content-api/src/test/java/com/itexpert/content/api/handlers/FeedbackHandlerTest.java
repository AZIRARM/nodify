package com.itexpert.content.api.handlers;

import com.itexpert.content.api.mappers.FeedbackMapper;
import com.itexpert.content.api.repositories.ContentNodeRepository;
import com.itexpert.content.api.repositories.FeedbackRepository;
import com.itexpert.content.lib.entities.ContentNode;
import com.itexpert.content.lib.entities.Feedback;
import com.itexpert.content.lib.enums.StatusEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeedbackHandlerTest {

    @Mock
    private FeedbackRepository feedbackRepository;

    @Mock
    private ContentNodeRepository contentNodeRepository;

    @Mock
    private FeedbackMapper feedbackMapper;

    @InjectMocks
    private FeedbackHandler feedbackHandler;

    private Feedback entityFeedback;
    private com.itexpert.content.lib.models.Feedback modelFeedback;
    private ContentNode contentNode;
    private UUID feedbackId;

    @BeforeEach
    void setUp() {
        feedbackId = UUID.randomUUID();

        entityFeedback = new Feedback();
        entityFeedback.setId(feedbackId);
        entityFeedback.setContentCode("test-code");
        entityFeedback.setEvaluation(5);
        entityFeedback.setMessage("Great content!");
        entityFeedback.setUserId("user123");
        entityFeedback.setVerified(true);
        entityFeedback.setCreatedDate(System.currentTimeMillis());
        entityFeedback.setModifiedDate(System.currentTimeMillis());

        modelFeedback = new com.itexpert.content.lib.models.Feedback();
        modelFeedback.setId(feedbackId);
        modelFeedback.setContentCode("test-code");
        modelFeedback.setEvaluation(5);
        modelFeedback.setMessage("Great content!");
        modelFeedback.setUserId("user123");
        modelFeedback.setVerified(true);
        modelFeedback.setCreatedDate(System.currentTimeMillis());
        modelFeedback.setModifiedDate(System.currentTimeMillis());

        contentNode = new ContentNode();
        contentNode.setCode("test-code");
        contentNode.setStatus(StatusEnum.PUBLISHED);
    }

    @Test
    void findAllShouldReturnAllFeedbacks() {
        when(feedbackRepository.findAll()).thenReturn(Flux.just(entityFeedback));
        when(feedbackMapper.fromEntity(entityFeedback)).thenReturn(modelFeedback);

        StepVerifier.create(feedbackHandler.findAll())
                .expectNext(modelFeedback)
                .verifyComplete();
    }

    @Test
    void findAllShouldReturnEmptyWhenNoFeedbacks() {
        when(feedbackRepository.findAll()).thenReturn(Flux.empty());

        StepVerifier.create(feedbackHandler.findAll())
                .verifyComplete();
    }

    @Test
    void findByContentCodeShouldReturnFeedbacks() {
        int currentPage = 0;
        int limit = 10;
        PageRequest pageRequest = PageRequest.of(currentPage, limit, Sort.by(Sort.Order.by("creationDate")).descending());

        when(feedbackRepository.findByContentCode("test-code", pageRequest)).thenReturn(Flux.just(entityFeedback));
        when(feedbackMapper.fromEntity(entityFeedback)).thenReturn(modelFeedback);

        StepVerifier.create(feedbackHandler.findByContentCode("test-code", currentPage, limit))
                .expectNext(modelFeedback)
                .verifyComplete();
    }

    @Test
    void findByUserIdShouldReturnFeedbacks() {
        int currentPage = 0;
        int limit = 10;
        PageRequest pageRequest = PageRequest.of(currentPage, limit, Sort.by(Sort.Order.by("creationDate")).descending());

        when(feedbackRepository.findByUserId("user123", pageRequest)).thenReturn(Flux.just(entityFeedback));
        when(feedbackMapper.fromEntity(entityFeedback)).thenReturn(modelFeedback);

        StepVerifier.create(feedbackHandler.findByUserId("user123", currentPage, limit))
                .expectNext(modelFeedback)
                .verifyComplete();
    }

    @Test
    void findByEvaluationShouldReturnFeedbacks() {
        int currentPage = 0;
        int limit = 10;
        PageRequest pageRequest = PageRequest.of(currentPage, limit, Sort.by(Sort.Order.by("creationDate")).descending());

        when(feedbackRepository.findByEvaluation(5, pageRequest)).thenReturn(Flux.just(entityFeedback));
        when(feedbackMapper.fromEntity(entityFeedback)).thenReturn(modelFeedback);

        StepVerifier.create(feedbackHandler.findByEvaluation(5, currentPage, limit))
                .expectNext(modelFeedback)
                .verifyComplete();
    }

    @Test
    void findByVerifiedShouldReturnFeedbacks() {
        int currentPage = 0;
        int limit = 10;
        PageRequest pageRequest = PageRequest.of(currentPage, limit, Sort.by(Sort.Order.by("creationDate")).descending());

        when(feedbackRepository.findByVerified(true, pageRequest)).thenReturn(Flux.just(entityFeedback));
        when(feedbackMapper.fromEntity(entityFeedback)).thenReturn(modelFeedback);

        StepVerifier.create(feedbackHandler.findByVerified(true, currentPage, limit))
                .expectNext(modelFeedback)
                .verifyComplete();
    }

    @Test
    void saveShouldCreateNewFeedback() {
        com.itexpert.content.lib.models.Feedback newFeedback = new com.itexpert.content.lib.models.Feedback();
        newFeedback.setContentCode("new-code");
        newFeedback.setEvaluation(4);
        newFeedback.setMessage("Nice!");

        when(feedbackMapper.fromModel(any(com.itexpert.content.lib.models.Feedback.class))).thenReturn(entityFeedback);
        when(feedbackRepository.save(any(Feedback.class))).thenReturn(Mono.just(entityFeedback));
        when(feedbackMapper.fromEntity(entityFeedback)).thenReturn(modelFeedback);

        StepVerifier.create(feedbackHandler.save(newFeedback))
                .expectNextMatches(saved -> saved.getContentCode().equals("test-code"))
                .verifyComplete();
    }

    @Test
    void saveShouldSetDefaultVerifiedFalse() {
        com.itexpert.content.lib.models.Feedback newFeedback = new com.itexpert.content.lib.models.Feedback();
        newFeedback.setContentCode("new-code");

        when(feedbackMapper.fromModel(any(com.itexpert.content.lib.models.Feedback.class))).thenReturn(entityFeedback);
        when(feedbackRepository.save(any(Feedback.class))).thenReturn(Mono.just(entityFeedback));
        when(feedbackMapper.fromEntity(entityFeedback)).thenReturn(modelFeedback);

        StepVerifier.create(feedbackHandler.save(newFeedback))
                .expectNextMatches(saved -> saved.isVerified() == true)
                .verifyComplete();
    }

    @Test
    void saveShouldSetDefaultEvaluationZero() {
        com.itexpert.content.lib.models.Feedback newFeedback = new com.itexpert.content.lib.models.Feedback();
        newFeedback.setContentCode("new-code");
        newFeedback.setVerified(true);

        when(feedbackMapper.fromModel(any(com.itexpert.content.lib.models.Feedback.class))).thenReturn(entityFeedback);
        when(feedbackRepository.save(any(Feedback.class))).thenReturn(Mono.just(entityFeedback));
        when(feedbackMapper.fromEntity(entityFeedback)).thenReturn(modelFeedback);

        StepVerifier.create(feedbackHandler.save(newFeedback))
                .expectNextMatches(saved -> saved.getEvaluation() == 5)
                .verifyComplete();
    }

    @Test
    void saveAllShouldReturnCount() {
        List<com.itexpert.content.lib.models.Feedback> feedbacks = Arrays.asList(modelFeedback, modelFeedback);

        when(feedbackMapper.fromModel(any(com.itexpert.content.lib.models.Feedback.class))).thenReturn(entityFeedback);
        when(feedbackRepository.saveAll(any(Iterable.class))).thenReturn(Flux.just(entityFeedback, entityFeedback));

        StepVerifier.create(feedbackHandler.saveAll(feedbacks))
                .expectNext(2L)
                .verifyComplete();
    }

    @Test
    void deleteShouldReturnTrueWhenSuccessful() {
        when(feedbackRepository.deleteById(feedbackId)).thenReturn(Mono.just(1L).flatMap(l -> Mono.empty()));

        StepVerifier.create(feedbackHandler.delete(feedbackId))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void deleteShouldReturnFalseWhenError() {
        when(feedbackRepository.deleteById(feedbackId)).thenReturn(Mono.error(new RuntimeException("DB error")));

        StepVerifier.create(feedbackHandler.delete(feedbackId))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void deleteAllByContentNodeCodeShouldReturnMono() {
        when(feedbackRepository.deleteAllByContentCode("test-code")).thenReturn(Mono.empty());

        StepVerifier.create(feedbackHandler.deleteAllByContentNodeCode("test-code"))
                .verifyComplete();
    }
}