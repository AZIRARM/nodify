package com.itexpert.content.api.endpoints;

import com.itexpert.content.api.handlers.FeedbackHandler;
import com.itexpert.content.lib.models.Feedback;
import com.itexpert.content.lib.models.FeedbackCharts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping(value = "/v0/feedbacks")
public class FeedbackEndPoint {

    private FeedbackHandler feedbackHandler;

    public FeedbackEndPoint(FeedbackHandler feedbackHandler) {
        this.feedbackHandler = feedbackHandler;
    }

    @GetMapping
    public Flux<Feedback> findAll() {
        return feedbackHandler.findAll();
    }

    @GetMapping(value = "/id/{id}")
    public Mono<ResponseEntity<Feedback>> findById(@PathVariable UUID id) {
        return feedbackHandler.findById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(new ResponseEntity<>(HttpStatus.NOT_FOUND));

    }

    @GetMapping(value = "/contentCode/{code}")
    public Flux<Feedback> findByContentCode(@PathVariable String code) {
        return feedbackHandler.findByContentCode(code);
    }

    @GetMapping(value = "/userId/{userId}")
    public Flux<Feedback> findByUserId(@PathVariable String userId) {
        return feedbackHandler.findByUserId(userId);
    }

    @GetMapping(value = "/evaluation/{evaluation}")
    public Flux<Feedback> findByEvaluation(@PathVariable int evaluation) {
        return feedbackHandler.findByEvaluation(evaluation);
    }

    @GetMapping(value = "/verified/{verified}")
    public Flux<Feedback> findByVerified(@PathVariable boolean verified) {
        return feedbackHandler.findByVerified(verified);
    }

    //@RolesAllowed("ADMIN")
    @PostMapping
    public Mono<ResponseEntity<Feedback>> save(@RequestBody(required = true) Feedback feedback) {
        return feedbackHandler.save(feedback)
                .map(ResponseEntity::ok);
    }

    //@RolesAllowed("ADMIN")
    @DeleteMapping(value = "/id/{id}")
    public Mono<ResponseEntity<Boolean>> delete(@PathVariable String id) {
        return feedbackHandler.delete(UUID.fromString(id))
                .map(ResponseEntity::ok);
    }

    @GetMapping(value = "/charts")
    public Flux<FeedbackCharts> getCharts() {
        return feedbackHandler.getCharts();
    }
}
