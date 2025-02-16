package com.itexpert.content.api.endpoints;

import com.itexpert.content.api.handlers.FeedbackHandler;
import com.itexpert.content.lib.models.Feedback;
import com.itexpert.content.lib.models.FeedbackCharts;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * REST controller for managing feedback.
 * Provides endpoints to retrieve, create, and delete feedback entries.
 */
@RestController
@RequestMapping(value = "/v0/feedbacks")
@AllArgsConstructor
@Tag(name = "Feedback Endpoint", description = "APIs for managing feedback")
public class FeedbackEndPoint {

    private final FeedbackHandler feedbackHandler;

    /**
     * Retrieves all feedback entries.
     *
     * @return a Flux of feedback
     */
    @Operation(summary = "Retrieve all feedback entries")
    @GetMapping
    public Flux<Feedback> findAll() {
        return feedbackHandler.findAll();
    }

    /**
     * Retrieves feedback by its unique ID.
     *
     * @param id the unique identifier of the feedback
     * @return a Mono containing the feedback if found, or a NOT FOUND response
     */
    @Operation(summary = "Retrieve feedback by ID")
    @GetMapping(value = "/id/{id}")
    public Mono<ResponseEntity<Feedback>> findById(@PathVariable UUID id) {
        return feedbackHandler.findById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * Retrieves feedback by content code.
     *
     * @param code the unique content code
     * @return a Flux of feedback associated with the content code
     */
    @Operation(summary = "Retrieve feedback by content code")
    @GetMapping(value = "/contentCode/{code}")
    public Flux<Feedback> findByContentCode(@PathVariable String code) {
        return feedbackHandler.findByContentCode(code);
    }

    /**
     * Retrieves feedback by user ID.
     *
     * @param userId the unique identifier of the user
     * @return a Flux of feedback associated with the user ID
     */
    @Operation(summary = "Retrieve feedback by user ID")
    @GetMapping(value = "/userId/{userId}")
    public Flux<Feedback> findByUserId(@PathVariable String userId) {
        return feedbackHandler.findByUserId(userId);
    }

    /**
     * Retrieves feedback by evaluation score.
     *
     * @param evaluation the evaluation score
     * @return a Flux of feedback with the specified evaluation score
     */
    @Operation(summary = "Retrieve feedback by evaluation score")
    @GetMapping(value = "/evaluation/{evaluation}")
    public Flux<Feedback> findByEvaluation(@PathVariable int evaluation) {
        return feedbackHandler.findByEvaluation(evaluation);
    }

    /**
     * Retrieves feedback by verification status.
     *
     * @param verified the verification status (true or false)
     * @return a Flux of feedback with the specified verification status
     */
    @Operation(summary = "Retrieve feedback by verification status")
    @GetMapping(value = "/verified/{verified}")
    public Flux<Feedback> findByVerified(@PathVariable boolean verified) {
        return feedbackHandler.findByVerified(verified);
    }

    /**
     * Saves a new feedback entry.
     *
     * @param feedback the feedback object to save
     * @return a Mono containing the saved feedback response entity
     */
    @Operation(summary = "Save a new feedback entry")
    @PostMapping
    public Mono<ResponseEntity<Feedback>> save(@RequestBody(required = true) Feedback feedback) {
        return feedbackHandler.save(feedback)
                .map(ResponseEntity::ok);
    }

    /**
     * Deletes a feedback entry by its unique ID.
     *
     * @param id the unique identifier of the feedback to delete
     * @return a Mono containing the response entity indicating success or failure
     */
    @Operation(summary = "Delete a feedback entry by ID", security = @SecurityRequirement(name = "ADMIN"))
    @DeleteMapping(value = "/id/{id}")
    public Mono<ResponseEntity<Boolean>> delete(@PathVariable String id) {
        return feedbackHandler.delete(UUID.fromString(id))
                .map(ResponseEntity::ok);
    }

    /**
     * Retrieves feedback statistics in the form of charts.
     *
     * @return a Flux of feedback chart data
     */
    @Operation(summary = "Retrieve feedback statistics")
    @GetMapping(value = "/charts")
    public Flux<FeedbackCharts> getCharts() {
        return feedbackHandler.getCharts();
    }
}
