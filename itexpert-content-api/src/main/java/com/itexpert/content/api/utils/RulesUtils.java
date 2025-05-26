package com.itexpert.content.api.utils;

import com.itexpert.content.lib.enums.TypeEnum;
import com.itexpert.content.lib.models.ContentNode;
import com.itexpert.content.lib.models.Node;
import com.itexpert.content.lib.models.Rule;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.List;

@Slf4j
public class RulesUtils {

    // DateTimeFormatter for parsing date strings, defined as a constant to avoid re-initialization
    private static final DateTimeFormatter DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
            .append(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
            .toFormatter();

    /**
     * Evaluates the rules of a ContentNode.
     * If all rules are satisfied, returns Mono<true>, otherwise Mono<false>.
     *
     * @param contentNode The ContentNode to evaluate.
     * @return Mono<Boolean> indicating whether all rules are satisfied.
     */
    public static Mono<Boolean> evaluateContentNode(ContentNode contentNode) {
        return evaluateRules(contentNode.getRules(), contentNode.getCode(), "content node");
    }

    /**
     * Evaluates the rules of a Node.
     * If all rules are satisfied, returns Mono<true>, otherwise Mono<false>.
     *
     * @param node The Node to evaluate.
     * @return Mono<Boolean> indicating whether all rules are satisfied.
     */
    public static Mono<Boolean> evaluateNode(Node node) {
        return evaluateRules(node.getRules(), node.getCode(), "node");
    }

    /**
     * Generic method to evaluate a list of rules.
     * If all rules are satisfied, returns Mono<true>, otherwise Mono<false>.
     *
     * @param rules The list of rules to evaluate.
     * @param code  The code of the entity being evaluated (for logging purposes).
     * @param type  The type of the entity being evaluated (for logging purposes).
     * @return Mono<Boolean> indicating whether all rules are satisfied.
     */
    private static Mono<Boolean> evaluateRules(List<Rule> rules, String code, String type) {
        if (ObjectUtils.isEmpty(rules)) {
            return Mono.just(true); // No rules to evaluate, return true
        }

        return Flux.fromIterable(rules)
                .filter(rule -> !verifyRuleCondition(rule)) // Filter out rules that are not satisfied
                .collectList()
                .map(List::size)
                .doOnNext(size -> log.info("Evaluated {} rules for {} [{}]", size, type, code))
                .filter(size -> size == rules.size()) // Check if all rules were not satisfied
                .hasElement()
                .doOnNext(result -> log.info("Evaluating {} [{}] with value [{}]", type, code, result));
    }

    /**
     * Verifies if a single rule condition is satisfied.
     *
     * @param rule The rule to verify.
     * @return boolean indicating whether the rule condition is satisfied.
     */
    private static boolean verifyRuleCondition(Rule rule) {
        if (!rule.getEnable()) {
            return false; // Rule is disabled, consider it as not satisfied
        }

        boolean evaluation = false;

        if (rule.getType().equals(TypeEnum.BOOL)) {
            evaluation = true; // Boolean rules are always considered as satisfied if enabled
        } else if (rule.getType().equals(TypeEnum.DATE)) {
            Instant ruleInstant = parseDate(rule.getValue());
            Instant now = Instant.now();

            switch (rule.getOperator()) {
                case EQ -> evaluation = now.equals(ruleInstant);
                case DIF -> evaluation = !now.equals(ruleInstant);
                case LOW -> evaluation = now.isBefore(ruleInstant);
                case LOW_EQ -> evaluation = now.isBefore(ruleInstant) || now.equals(ruleInstant);
                case SUP -> evaluation = now.isAfter(ruleInstant);
                case SUP_EQ -> evaluation = now.isAfter(ruleInstant) || now.equals(ruleInstant);
            }
        }

        // Apply behavior logic: if behavior is true, invert the evaluation
        return rule.getBehavior() ? !evaluation : evaluation;
    }

    /**
     * Parses a date string into an Instant.
     *
     * @param dateString The date string to parse.
     * @return Instant representing the parsed date.
     */
    private static Instant parseDate(String dateString) {
        String formattedDateString = dateString.replace("T", " "); // Replace 'T' with space for parsing
        LocalDateTime localDateTime = LocalDateTime.parse(formattedDateString, DATE_TIME_FORMATTER);
        return localDateTime.toInstant(ZoneOffset.UTC);
    }
}