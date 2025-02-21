package com.itexpert.content.api.handlers;

import com.itexpert.content.api.mappers.FeedbackMapper;
import com.itexpert.content.api.repositories.ContentNodeRepository;
import com.itexpert.content.api.repositories.FeedbackRepository;
import com.itexpert.content.lib.enums.StatusEnum;
import com.itexpert.content.lib.models.Chart;
import com.itexpert.content.lib.models.Feedback;
import com.itexpert.content.lib.models.FeedbackCharts;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@AllArgsConstructor
@Service
public class FeedbackHandler {
    private final FeedbackRepository feedbackRepository;
    private final ContentNodeRepository contentNodeRepository;
    private final FeedbackMapper feedbackMapper;

    public Flux<Feedback> findAll() {
        return feedbackRepository.findAll().map(feedbackMapper::fromEntity
        );
    }

    public Flux<Feedback> findByContentCode(String code, Integer currentPage, Integer limit) {
        return feedbackRepository.findByContentCode(code, PageRequest.of(currentPage, limit, Sort.by(Sort.Order.by("creationDate")).descending())).map(feedbackMapper::fromEntity
        );
    }

    public Flux<Feedback> findByUserId(String userId, Integer currentPage, Integer limit) {
        return feedbackRepository.findByUserId(userId, PageRequest.of(currentPage, limit, Sort.by(Sort.Order.by("creationDate")).descending())).map(feedbackMapper::fromEntity
        );
    }

    public Flux<Feedback> findByEvaluation(int evaluation, Integer currentPage, Integer limit) {
        return feedbackRepository.findByEvaluation(evaluation, PageRequest.of(currentPage, limit, Sort.by(Sort.Order.by("creationDate")).descending())).map(feedbackMapper::fromEntity
        );
    }

    public Flux<Feedback> findByVerified(boolean verified, Integer currentPage, Integer limit) {
        return feedbackRepository.findByVerified(verified, PageRequest.of(currentPage, limit, Sort.by(Sort.Order.by("creationDate")).descending())).map(feedbackMapper::fromEntity
        );
    }


    public Mono<Feedback> save(Feedback feedback) {
        feedback.setId(UUID.randomUUID());
        if (ObjectUtils.isEmpty(feedback.isVerified())) {
            feedback.setVerified(false);
        }
        if (ObjectUtils.isEmpty(feedback.getEvaluation())) {
            feedback.setEvaluation(0);
        }
        return feedbackRepository.save(feedbackMapper.fromModel(feedback))
                .map(feedbackMapper::fromEntity);
    }


    public Mono<Long> saveAll(List<Feedback> feedbacks) {
        return feedbackRepository.saveAll(feedbacks.stream().map(feedbackMapper::fromModel).collect(Collectors.toList())).count();

    }

    public Mono<Boolean> delete(UUID uuid) {
        return feedbackRepository.deleteById(uuid)
                .map(unused -> Boolean.TRUE)
                .onErrorReturn(Boolean.FALSE);
    }


    public Flux<FeedbackCharts> getCharts() {
        return this.getDistinctContentCodesWithEvaluations()
                .groupBy(Feedback::getEvaluation)
                .flatMap(group -> group.reduce((o1, o2) -> o1.getEvaluation() > o2.getEvaluation() ? o1 : o2))
                .map(feedback -> {
                    List<Chart> charts = this.getAllChartsForContentCodeByEvaluations(feedback.getContentCode(), feedback.getEvaluation());
                    return new FeedbackCharts(feedback.getContentCode(), charts, null, null);
                });

    }

    private List<Chart> getAllChartsForContentCodeByEvaluations(String contentCode, Integer maxEvaluation) {
        List<Chart> list = Flux.range(0, maxEvaluation)
                .map(evaluation ->
                        this.feedbackRepository.countByContentCodeAndEvaluation(contentCode, evaluation)
                                .map(count -> {
                                    return new Chart(evaluation.toString(), count.toString(), false);
                                })
                ).flatMap(Mono::from).collectList().block();


        return list;
    }

    private Flux<Feedback> getDistinctContentCodesWithEvaluations() {
        return this.contentNodeRepository.findAllByStatus(StatusEnum.PUBLISHED.name())
                .flatMap(
                        contentNode -> this.feedbackRepository.findFirstByOrderByEvaluationDesc()
                ).map(feedbackMapper::fromEntity);
    }

    public Mono<Boolean> deleteAllByContentNodeCode(String code) {
        return this.feedbackRepository.deleteAllByContentCode(code);
    }
}

