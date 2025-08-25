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
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoUnit;
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
     * @param ruleCondition The rule to verify.
     * @return boolean indicating whether the rule condition is satisfied.
     */
    private static boolean verifyRuleCondition(Rule ruleCondition) {
        boolean evaluation = false;
        if (ruleCondition.getEnable()) {
            if (ruleCondition.getType().equals(TypeEnum.BOOL)) {
                evaluation = true;
            } else if (ruleCondition.getType().equals(TypeEnum.DATE)) {
                DateTimeFormatter format = new DateTimeFormatterBuilder()
                        .append(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                        .toFormatter();
                Instant instant = LocalDateTime.parse(ruleCondition.getValue().replace("T", " "), format)
                        .atZone(ZoneId.systemDefault())
                        .toInstant();

                switch (ruleCondition.getOperator()) {
                    case EQ -> evaluation = Instant.now().toEpochMilli() == instant.toEpochMilli();
                    case DIF -> evaluation = Instant.now().toEpochMilli() != instant.toEpochMilli();
                    case LOW -> evaluation = Instant.now().toEpochMilli() < instant.toEpochMilli();
                    case LOW_EQ -> evaluation = Instant.now().toEpochMilli() <= instant.toEpochMilli();
                    case SUP -> evaluation = Instant.now().toEpochMilli() > instant.toEpochMilli();
                    case SUP_EQ -> evaluation = Instant.now().toEpochMilli() >= instant.toEpochMilli();
                }
            }

            evaluation = ruleCondition.getBehavior() ?
                    !(evaluation && ruleCondition.getEnable() && ruleCondition.getBehavior())
                    :
                    evaluation && ruleCondition.getEnable() && !ruleCondition.getBehavior();
        }
        return evaluation;
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