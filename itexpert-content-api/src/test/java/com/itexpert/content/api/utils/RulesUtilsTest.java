package com.itexpert.content.api.utils;

import com.itexpert.content.lib.enums.OperatorEnum;
import com.itexpert.content.lib.enums.TypeEnum;
import com.itexpert.content.lib.models.ContentNode;
import com.itexpert.content.lib.models.Node;
import com.itexpert.content.lib.models.Rule;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;

class RulesUtilsTest {

    @Test
    void evaluateContentNodeShouldReturnTrueWhenNoRules() {
        ContentNode contentNode = new ContentNode();
        contentNode.setCode("test-code");
        contentNode.setRules(null);

        StepVerifier.create(RulesUtils.evaluateContentNode(contentNode))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void evaluateContentNodeShouldReturnTrueWhenRulesEmpty() {
        ContentNode contentNode = new ContentNode();
        contentNode.setCode("test-code");
        contentNode.setRules(Collections.emptyList());

        StepVerifier.create(RulesUtils.evaluateContentNode(contentNode))
                .expectNext(true)
                .verifyComplete();
    }


    @Test
    void evaluateNodeShouldReturnTrueWhenNoRules() {
        Node node = new Node();
        node.setCode("test-code");
        node.setRules(null);

        StepVerifier.create(RulesUtils.evaluateNode(node))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void evaluateNodeShouldReturnTrueWhenRulesEmpty() {
        Node node = new Node();
        node.setCode("test-code");
        node.setRules(Collections.emptyList());

        StepVerifier.create(RulesUtils.evaluateNode(node))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void evaluateContentNodeWithDateRuleEqShouldReturnTrue() {
        Instant now = Instant.now();
        LocalDateTime dateTime = LocalDateTime.ofInstant(now, ZoneId.systemDefault());
        String formattedDate = dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        Rule rule = new Rule();
        rule.setEnable(true);
        rule.setType(TypeEnum.DATE);
        rule.setOperator(OperatorEnum.EQ);
        rule.setBehavior(false);
        rule.setValue(formattedDate.replace(" ", "T"));

        ContentNode contentNode = new ContentNode();
        contentNode.setCode("test-code");
        contentNode.setRules(Collections.singletonList(rule));

        StepVerifier.create(RulesUtils.evaluateContentNode(contentNode))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void evaluateContentNodeWithDateRuleLowShouldReturnTrue() {
        Instant past = Instant.now().minusSeconds(3600);
        LocalDateTime dateTime = LocalDateTime.ofInstant(past, ZoneId.systemDefault());
        String formattedDate = dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        Rule rule = new Rule();
        rule.setEnable(true);
        rule.setType(TypeEnum.DATE);
        rule.setOperator(OperatorEnum.LOW);
        rule.setBehavior(false);
        rule.setValue(formattedDate.replace(" ", "T"));

        ContentNode contentNode = new ContentNode();
        contentNode.setCode("test-code");
        contentNode.setRules(Collections.singletonList(rule));

        StepVerifier.create(RulesUtils.evaluateContentNode(contentNode))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void evaluateContentNodeWithDateRuleSupShouldReturnTrue() {
        Instant future = Instant.now().plusSeconds(3600);
        LocalDateTime dateTime = LocalDateTime.ofInstant(future, ZoneId.systemDefault());
        String formattedDate = dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        Rule rule = new Rule();
        rule.setEnable(true);
        rule.setType(TypeEnum.DATE);
        rule.setOperator(OperatorEnum.SUP);
        rule.setBehavior(false);
        rule.setValue(formattedDate.replace(" ", "T"));

        ContentNode contentNode = new ContentNode();
        contentNode.setCode("test-code");
        contentNode.setRules(Collections.singletonList(rule));

        StepVerifier.create(RulesUtils.evaluateContentNode(contentNode))
                .expectNext(true)
                .verifyComplete();
    }


    @Test
    void evaluateContentNodeWithDateRuleLowEqShouldReturnTrue() {
        Instant past = Instant.now().minusSeconds(3600);
        LocalDateTime dateTime = LocalDateTime.ofInstant(past, ZoneId.systemDefault());
        String formattedDate = dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        Rule rule = new Rule();
        rule.setEnable(true);
        rule.setType(TypeEnum.DATE);
        rule.setOperator(OperatorEnum.LOW_EQ);
        rule.setBehavior(false);
        rule.setValue(formattedDate.replace(" ", "T"));

        ContentNode contentNode = new ContentNode();
        contentNode.setCode("test-code");
        contentNode.setRules(Collections.singletonList(rule));

        StepVerifier.create(RulesUtils.evaluateContentNode(contentNode))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void evaluateContentNodeWithDateRuleSupEqShouldReturnTrue() {
        Instant future = Instant.now().plusSeconds(3600);
        LocalDateTime dateTime = LocalDateTime.ofInstant(future, ZoneId.systemDefault());
        String formattedDate = dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        Rule rule = new Rule();
        rule.setEnable(true);
        rule.setType(TypeEnum.DATE);
        rule.setOperator(OperatorEnum.SUP_EQ);
        rule.setBehavior(false);
        rule.setValue(formattedDate.replace(" ", "T"));

        ContentNode contentNode = new ContentNode();
        contentNode.setCode("test-code");
        contentNode.setRules(Collections.singletonList(rule));

        StepVerifier.create(RulesUtils.evaluateContentNode(contentNode))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void evaluateContentNodeWithDisabledRuleShouldReturnTrue() {
        Rule rule = new Rule();
        rule.setEnable(false);
        rule.setType(TypeEnum.BOOL);
        rule.setBehavior(false);
        rule.setValue("true");

        ContentNode contentNode = new ContentNode();
        contentNode.setCode("test-code");
        contentNode.setRules(Collections.singletonList(rule));

        StepVerifier.create(RulesUtils.evaluateContentNode(contentNode))
                .expectNext(true)
                .verifyComplete();
    }
}