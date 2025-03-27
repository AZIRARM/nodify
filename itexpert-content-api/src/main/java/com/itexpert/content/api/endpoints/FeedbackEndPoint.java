package com.itexpert.content.api.endpoints;

import com.itexpert.content.api.handlers.FeedbackHandler;
import com.itexpert.content.lib.models.Feedback;
import com.itexpert.content.lib.models.FeedbackCharts;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
     * Retrieves feedback by content code with pagination.
     *
     * @param code        The unique content code.
     * @param currentPage The current page index (optional, default: 0).
     * @param limit       The number of items per page (optional, default: 50).
     * @return A Flux of feedback associated with the content code.
     */
    @Operation(summary = "Retrieve feedback by content code", description = "Fetches feedback entries based on a given content code with optional pagination.")
    @GetMapping(value = "/contentCode/{code}")
    public Flux<Feedback> findByContentCode(@PathVariable String code,
                                            @Parameter(description = "Current page index", example = "0") @RequestParam(name = "currentPage", required = false, defaultValue = "0") Integer currentPage,
                                            @Parameter(description = "Limit per page", example = "50") @RequestParam(name = "limit", required = false, defaultValue = "50") Integer limit) {
        return feedbackHandler.findByContentCode(code, currentPage, limit);
    }

    /**
     * Retrieves feedback by user ID with pagination.
     *
     * @param userId      The unique identifier of the user.
     * @param currentPage The current page index (optional, default: 0).
     * @param limit       The number of items per page (optional, default: 50).
     * @return A Flux of feedback associated with the user ID.
     */
    @Operation(summary = "Retrieve feedback by user ID", description = "Fetches feedback based on the user's unique identifier with optional pagination.")
    @GetMapping(value = "/userId/{userId}")
    public Flux<Feedback> findByUserId(@PathVariable String userId,
                                       @Parameter(description = "Current page index", example = "0") @RequestParam(name = "currentPage", required = false, defaultValue = "0") Integer currentPage,
                                       @Parameter(description = "Limit per page", example = "50") @RequestParam(name = "limit", required = false, defaultValue = "50") Integer limit) {
        return feedbackHandler.findByUserId(userId, currentPage, limit);
    }

    /**
     * Retrieves feedback by evaluation score with pagination.
     *
     * @param evaluation  The evaluation score.
     * @param currentPage The current page index (optional, default: 0).
     * @param limit       The number of items per page (optional, default: 50).
     * @return A Flux of feedback with the specified evaluation score.
     */
    @Operation(summary = "Retrieve feedback by evaluation score", description = "Fetches feedback based on evaluation score with optional pagination.")
    @GetMapping(value = "/evaluation/{evaluation}")
    public Flux<Feedback> findByEvaluation(@PathVariable int evaluation,
                                           @Parameter(description = "Current page index", example = "0") @RequestParam(name = "currentPage", required = false, defaultValue = "0") Integer currentPage,
                                           @Parameter(description = "Limit per page", example = "50") @RequestParam(name = "limit", required = false, defaultValue = "50") Integer limit) {
        return feedbackHandler.findByEvaluation(evaluation, currentPage, limit);
    }

    /**
     * Retrieves feedback by verification status with pagination.
     *
     * @param verified    The verification status (true or false).
     * @param currentPage The current page index (optional, default: 0).
     * @param limit       The number of items per page (optional, default: 50).
     * @return A Flux of feedback with the specified verification status.
     */
    @Operation(summary = "Retrieve feedback by verification status", description = "Fetches feedback based on verification status with optional pagination.")
    @GetMapping(value = "/verified/{verified}")
    public Flux<Feedback> findByVerified(@PathVariable boolean verified,
                                         @Parameter(description = "Current page index", example = "0") @RequestParam(name = "currentPage", required = false, defaultValue = "0") Integer currentPage,
                                         @Parameter(description = "Limit per page", example = "50") @RequestParam(name = "limit", required = false, defaultValue = "50") Integer limit) {
        return feedbackHandler.findByVerified(verified, currentPage, limit);
    }

    /**
     * Saves a new feedback entry.
     *
     * @param feedback The feedback object to save.
     * @return A Mono containing the saved feedback response entity.
     */
    @Operation(summary = "Save a new feedback entry", description = "Creates a new feedback entry in the system.")
    @PostMapping("/")
    public Mono<ResponseEntity<Feedback>> save(@RequestBody Feedback feedback) {
        return feedbackHandler.save(feedback)
                .map(ResponseEntity::ok);
    }

    /**
     * Deletes all feedback entries by content code.
     *
     * @param code The content code.
     * @return A Mono containing the ResponseEntity with a boolean indicating success.
     */
    @Operation(summary = "Delete feedback by content code", description = "Deletes all feedback associated with the given content code.")
    @DeleteMapping(value = "/contentCode/{code}")
    public Mono<ResponseEntity<Boolean>> deleteAllByContentNodeCode(@PathVariable String code) {
        return feedbackHandler.deleteAllByContentNodeCode(code)
                .map(ResponseEntity::ok);
    }

    /**
     * Deletes a feedback entry by its unique ID.
     *
     * @param id The unique identifier of the feedback to delete.
     * @return A Mono containing the ResponseEntity indicating success or failure.
     */
    @Operation(summary = "Delete feedback by ID", description = "Deletes a feedback entry by its unique identifier.", security = @SecurityRequirement(name = "ADMIN"))
    @DeleteMapping(value = "/id/{id}")
    public Mono<ResponseEntity<Boolean>> delete(@PathVariable String id) {
        return feedbackHandler.delete(UUID.fromString(id))
                .map(ResponseEntity::ok);
    }

    /**
     * Retrieves feedback statistics in chart format.
     *
     * @return A Flux of feedback chart data.
     */
    @Operation(summary = "Retrieve feedback statistics", description = "Fetches aggregated feedback statistics in chart format.")
    @GetMapping(value = "/charts")
    public Flux<FeedbackCharts> getCharts() {
        return feedbackHandler.getCharts();
    }
}

