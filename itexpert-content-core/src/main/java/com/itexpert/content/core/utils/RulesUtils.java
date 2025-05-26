package com.itexpert.content.core.utils;

import com.itexpert.content.lib.enums.OperatorEnum;
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


    public static Mono<Boolean> evaluateContentNode(ContentNode contentNode) {
        if (ObjectUtils.isNotEmpty(contentNode.getRules())) {
            return Flux.fromIterable(contentNode.getRules())
                    .filter(ruleCondition -> !verifyRuleCondition(ruleCondition))
                    .collectList()
                    .map(List::size)
                    .doOnNext(size -> {
                        log.info("Evaluated {} ", size == contentNode.getRules().size());
                    })
                    .filter(size -> size.intValue() == contentNode.getRules().size())
                    .hasElement()
                    .doOnNext(aBoolean -> {
                        log.info("Evaluating content node [{}] with value [{}]", contentNode.getCode(), aBoolean);
                    });
        }
        return Mono.just(true);
    }

    public static Mono<Boolean> evaluateNode(Node node) {
        if (ObjectUtils.isNotEmpty(node.getRules())) {
            return Flux.fromIterable(node.getRules())
                    .filter(ruleCondition -> !verifyRuleCondition(ruleCondition))
                    .collectList()
                    .map(List::size)
                    .doOnNext(size -> {
                        log.info("Evaluated {} ", size == node.getRules().size());
                    })
                    .filter(size -> size.intValue() == node.getRules().size())
                    .hasElement()
                    .doOnNext(aBoolean -> {
                        log.info("Evaluating node [{}] with value [{}]", node.getCode(), aBoolean);
                    });
        }
        return Mono.just(true);
    }

    private static boolean verifyRuleCondition(Rule ruleCondition) {
        boolean evaluation = false;
        if (ruleCondition.getEnable()) {
            if (ruleCondition.getType().equals(TypeEnum.BOOL)) {
                evaluation = true;
            } else if (ruleCondition.getType().equals(TypeEnum.DATE)) {
                DateTimeFormatter format = new DateTimeFormatterBuilder()
                        .append(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                        .toFormatter();
                Instant instant = LocalDateTime.parse(ruleCondition.getValue().replace("T", " "), format).toInstant(ZoneOffset.UTC);

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

    public static List<Rule> getDefaultRules() {
        Rule ruleMaintenance = new Rule();
        ruleMaintenance.setName("MAINTENANCE");
        ruleMaintenance.setType(TypeEnum.BOOL);
        ruleMaintenance.setValue("false");
        ruleMaintenance.setBehavior(Boolean.FALSE);
        ruleMaintenance.setEnable(Boolean.FALSE);
        ruleMaintenance.setEditable(false);
        ruleMaintenance.setErasable(false);


        Rule activationDate = new Rule();
        activationDate.setName("ACTIVATION_DATE");
        activationDate.setOperator(OperatorEnum.SUP_EQ);
        activationDate.setType(TypeEnum.DATE);
        activationDate.setBehavior(Boolean.FALSE);
        activationDate.setEnable(Boolean.FALSE);
        activationDate.setEditable(false);
        activationDate.setErasable(false);

        Rule endDate = new Rule();
        endDate.setName("DEACTIVATION_DATE");
        endDate.setOperator(OperatorEnum.SUP_EQ);
        endDate.setType(TypeEnum.DATE);
        endDate.setBehavior(Boolean.FALSE);
        endDate.setEnable(Boolean.FALSE);
        endDate.setEditable(false);
        endDate.setErasable(false);

        return List.of(ruleMaintenance, activationDate, endDate);
    }

}
