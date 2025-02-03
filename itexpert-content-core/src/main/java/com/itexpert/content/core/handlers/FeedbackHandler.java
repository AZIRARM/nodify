package com.itexpert.content.core.handlers;

import com.itexpert.content.core.mappers.FeedbackMapper;
import com.itexpert.content.core.repositories.ContentNodeRepository;
import com.itexpert.content.core.repositories.FeedbackRepository;
import com.itexpert.content.lib.entities.ContentNode;
import com.itexpert.content.lib.enums.NotificationEnum;
import com.itexpert.content.lib.enums.StatusEnum;
import com.itexpert.content.lib.models.Chart;
import com.itexpert.content.lib.models.Feedback;
import com.itexpert.content.lib.models.FeedbackCharts;
import com.itexpert.content.lib.models.UserPost;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
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
    private final NodeHandler nodeHandler;
    private final FeedbackMapper feedbackMapper;
    private final NotificationHandler notificationHandler;


    private final UserHandler userHandler;

    public Flux<Feedback> findAll() {
        return feedbackRepository.findAll().map(feedbackMapper::fromEntity);
    }

    public Flux<Feedback> findByContentCode(String code) {
        return feedbackRepository.findByContentCode(code).map(feedbackMapper::fromEntity
        );
    }

    public Flux<Feedback> findByUserId(String userId) {
        return feedbackRepository.findByUserId(userId).map(feedbackMapper::fromEntity
        );
    }

    public Flux<Feedback> findByEvaluation(int evaluation) {
        return feedbackRepository.findByEvaluation(evaluation).map(feedbackMapper::fromEntity
        );
    }

    public Flux<Feedback> findByVerified(boolean verified) {
        return feedbackRepository.findByVerified(verified).map(feedbackMapper::fromEntity
        );
    }

    public Mono<Feedback> findById(UUID uuid) {
        return feedbackRepository.findById(uuid).map(feedbackMapper::fromEntity);
    }

    public Flux<FeedbackCharts> getContentCharts() {
        return this.feedbackRepository.findDistinctEvaluations()
                .flatMap(evaluation ->
                        this.feedbackRepository.countAllVerifiedsByEvaluation(evaluation.intValue())
                                .map(total -> new Chart(evaluation.toString(), total.toString(), true))
                                .flatMapMany(verifiedChart ->
                                        this.feedbackRepository.countAllNotVerifiedsByEvaluation(evaluation.intValue())
                                                .map(total -> new Chart(evaluation.toString(), total.toString(), false))
                                                .flux()
                                                .startWith(verifiedChart) // Combine verifiedChart avec le flux des non vérifiés
                                )
                )
                .collectList() // Collecte tous les charts dans une liste
                .map(charts -> {
                    // Sépare les charts en catégories
                    List<Chart> verifieds = charts.stream().filter(Chart::isVerified).toList();
                    List<Chart> notVerifieds = charts.stream().filter(chart -> !chart.isVerified()).toList();
                    return new FeedbackCharts("ALL", charts, verifieds, notVerifieds); // Crée un objet FeedbackCharts
                })
                .flux(); // Transforme le Mono en Flux pour un flux réactif.
    }


    private Flux<Feedback> getDistinctContentCodesWithEvaluations() {
        return this.contentNodeRepository.findAllByStatus(StatusEnum.PUBLISHED.name())
                .flatMap(
                        contentNode -> {
                            return this.feedbackRepository.findFirstByContentCodeOrderByEvaluationDesc(contentNode.getCode());
                        }
                ).map(feedbackMapper::fromEntity).doOnNext(feedback -> {
                    log.info(feedback.getContentCode() + " : " + feedback.getEvaluation());
                });
    }


    public Mono<Feedback> save(Feedback feedback) {
        if (ObjectUtils.isEmpty(feedback.getId())) {
            feedback.setId(UUID.randomUUID());
        }
        return feedbackRepository.save(feedbackMapper.fromModel(feedback))
                .map(feedbackMapper::fromEntity)
                .flatMap(model -> this.notify(model, NotificationEnum.CREATION));
    }


    public Mono<Long> saveAll(List<Feedback> feedbacks) {
        return feedbackRepository.saveAll(feedbacks.stream().map(feedbackMapper::fromModel).collect(Collectors.toList())).count();

    }

    public Mono<Boolean> delete(UUID uuid) {
        return this.feedbackRepository.findById(uuid)
                .flatMap(entity ->
                        this.notify(this.feedbackMapper.fromEntity(entity), NotificationEnum.DELETION)
                                .flatMap(notification ->
                                        this.feedbackRepository.deleteById(uuid)
                                                .thenReturn(Boolean.TRUE)
                                )
                                .onErrorReturn(Boolean.FALSE)
                );
    }


    private Flux<Feedback> getDistinctContentCodesWithEvaluations(UserPost userPost) {
        return this.contentNodeRepository.findAllByStatus(StatusEnum.PUBLISHED.name())
                .filter(contentNode -> userPost.getProjects().contains(contentNode.getParentCodeOrigin()))
                .flatMap(
                        contentNode -> {
                            return this.feedbackRepository.findFirstByContentCodeOrderByEvaluationDesc(contentNode.getCode());
                        }
                ).map(feedbackMapper::fromEntity).doOnNext(feedback -> {
                    log.info(feedback.getContentCode() + " : " + feedback.getEvaluation());
                });
    }

    public Mono<Feedback> notify(Feedback model, NotificationEnum type) {
        return Mono.just(model).flatMap(feedback -> {
            return notificationHandler
                    .create(type, feedback.getContentCode() + ", evaluation : " + feedback.getEvaluation(), null, "FEEDBACK", model.getContentCode(), null)
                    .map(notification -> model);
        });
    }


    public Flux<FeedbackCharts> getContentChartsByNode(String code) {
        return
                this.nodeHandler.findAllChildren(code)
                        .flatMap(node -> this.contentNodeRepository.findByNodeCodeAndStatus(node.getCode(), StatusEnum.PUBLISHED.name()))
                        .groupBy(ContentNode::getCode)
                        .flatMap(g -> g.reduce((a, b) -> a.getCode().compareTo(b.getCode()) > 0 ? a : b))
                        .map(ContentNode::getCode)
                        .collectList()
                        .map(this.feedbackRepository::findDistinctEvaluationsByContentCode)
                        .flatMap(Flux::collectList)
                        .flatMapIterable(list -> list)
                        .flatMap(evaluation ->
                                this.feedbackRepository.countAllVerifiedsByEvaluation(evaluation.intValue())
                                        .map(total -> new Chart(evaluation.toString(), total.toString(), true))
                                        .flatMapMany(verifiedChart ->
                                                this.feedbackRepository.countAllNotVerifiedsByEvaluation(evaluation.intValue())
                                                        .map(total -> new Chart(evaluation.toString(), total.toString(), false))
                                                        .flux()
                                                        .startWith(verifiedChart) // Combine verifiedChart avec le flux des non vérifiés
                                        )
                        )
                        .collectList() // Collecte tous les charts dans une liste
                        .map(charts -> {
                            // Sépare les charts en catégories
                            List<Chart> verifieds = charts.stream().filter(Chart::isVerified).toList();
                            List<Chart> notVerifieds = charts.stream().filter(chart -> !chart.isVerified()).toList();
                            return new FeedbackCharts("ALL", charts, verifieds, notVerifieds); // Crée un objet FeedbackCharts
                        })
                        .flux(); // Transforme le Mono en Flux pour un flux réactif.

    }


    public Flux<FeedbackCharts> getContentChartsByContent(String code) {
        return
                Mono.just(List.of(code))
                        .map(this.feedbackRepository::findDistinctEvaluationsByContentCode)
                        .flatMap(Flux::collectList)
                        .flatMapIterable(list -> list)
                        .flatMap(evaluation ->
                                this.feedbackRepository.countAllVerifiedsByEvaluation(evaluation.intValue())
                                        .map(total -> new Chart(evaluation.toString(), total.toString(), true))
                                        .flatMapMany(verifiedChart ->
                                                this.feedbackRepository.countAllNotVerifiedsByEvaluation(evaluation.intValue())
                                                        .map(total -> new Chart(evaluation.toString(), total.toString(), false))
                                                        .flux()
                                                        .startWith(verifiedChart) // Combine verifiedChart avec le flux des non vérifiés
                                        )
                        )
                        .collectList() // Collecte tous les charts dans une liste
                        .map(charts -> {
                            // Sépare les charts en catégories
                            List<Chart> verifieds = charts.stream().filter(Chart::isVerified).toList();
                            List<Chart> notVerifieds = charts.stream().filter(chart -> !chart.isVerified()).toList();
                            return new FeedbackCharts("ALL", charts, verifieds, notVerifieds); // Crée un objet FeedbackCharts
                        })
                        .flux(); // Transforme le Mono en Flux pour un flux réactif.
    }
}

