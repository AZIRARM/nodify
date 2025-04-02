package com.itexpert.content.api.helpers;

import com.itexpert.content.api.handlers.ContentDisplayHandler;
import com.itexpert.content.api.mappers.ContentNodeMapper;
import com.itexpert.content.api.repositories.ContentNodeRepository;
import com.itexpert.content.api.repositories.NodeRepository;
import com.itexpert.content.lib.entities.ContentNode;
import com.itexpert.content.lib.entities.Node;
import com.itexpert.content.lib.enums.StatusEnum;
import com.itexpert.content.lib.models.ContentDisplay;
import com.itexpert.content.lib.models.Translation;
import com.itexpert.content.lib.models.Value;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@AllArgsConstructor
@Slf4j
public class ContentHelper {

    private final ContentNodeMapper contentNodeMapper;
    private final ContentDisplayHandler contentDisplayHandler;
    private final ContentNodeRepository contentNodeRepository;
    private final NodeRepository nodeRepository;

    /**
     * Fills the content of a given ContentNode, handling nested content, translations, and values.
     *
     * @param element     The ContentNode to process.
     * @param status      The status of the content.
     * @param translation The translation language.
     * @return A Mono emitting the processed ContentNode.
     */
    public Mono<ContentNode> fillContents(ContentNode element, StatusEnum status, String translation) {
        return this.fillContentFactory(element, status, translation)
                .onErrorReturn(element); // Returns original element on error.
    }

    /**
     * Recursive factory method to fill content, handling nested content references.
     *
     * @param element     The ContentNode to process.
     * @param status      The status of the content.
     * @param translation The translation language.
     * @return A Mono emitting the processed ContentNode.
     */
    private Mono<ContentNode> fillContentFactory(ContentNode element, StatusEnum status, String translation) {
        if (ObjectUtils.isNotEmpty(element) && ObjectUtils.isNotEmpty(element.getContent()) && element.getContent().contains("$content(")) {
            return Flux.fromIterable(getContentCodes(element))
                    .collectList()
                    .flatMapMany(contentCodes -> this.getContents(contentCodes, status))
                    .collectList()
                    .flatMap(contentNodes ->
                            this.getNodes(contentNodes, status)
                                    .collectList()
                                    .map(nodes -> Tuples.of(contentNodes, nodes))
                    )
                    .map(tuplesContentsAndNodes -> {
                        while (element.getContent().contains("$content(")) {
                            tuplesContentsAndNodes.getT1().forEach(contentNode -> {
                                Node parentNode = this.getParentNode(tuplesContentsAndNodes.getT2(), contentNode.getParentCode());
                                if (parentNode != null) {
                                    contentNode.setContent(this.translate(contentNode, contentNode.getTranslations(), ObjectUtils.isNotEmpty(translation) ? translation : parentNode.getDefaultLanguage()));
                                    contentNode.setContent(this.fillValues(contentNode, contentNode.getValues()));

                                    element.setContent(element.getContent().replace("$content(" + contentNode.getCode() + ")", contentNode.getContent()));

                                    element.setContent(this.translate(element, element.getTranslations(), ObjectUtils.isNotEmpty(translation) ? translation : parentNode.getDefaultLanguage()));
                                    element.setContent(this.fillValues(element, element.getValues()));

                                    tuplesContentsAndNodes.getT2().forEach(node -> {
                                        element.setContent(this.translate(element, node.getTranslations(), ObjectUtils.isNotEmpty(translation) ? translation : parentNode.getDefaultLanguage()));
                                        element.setContent(this.fillValues(element, node.getValues()));
                                    });
                                }
                            });
                        }
                        return element;
                    });
        } else if (ObjectUtils.isNotEmpty(element) && ObjectUtils.isNotEmpty(element.getContent())) {
            return this.getNodes(element, status)
                    .collectList()
                    .map(nodes -> {
                        Node parentNode = this.getParentNode(nodes, element.getParentCode());
                        if (parentNode != null) {
                            element.setContent(this.translate(element, element.getTranslations(), ObjectUtils.isNotEmpty(translation) ? translation : parentNode.getDefaultLanguage()));
                            element.setContent(this.fillValues(element, element.getValues()));

                            nodes.forEach(node -> {
                                element.setContent(this.translate(element, node.getTranslations(), ObjectUtils.isNotEmpty(translation) ? translation : parentNode.getDefaultLanguage()));
                                element.setContent(this.fillValues(element, node.getValues()));
                            });
                        }
                        return element;
                    });
        }
        return Mono.just(element);
    }

    /**
     * Translates content based on given translations and default language.
     *
     * @param content         The ContentNode to translate.
     * @param translations    List of translations.
     * @param defaultLanguage Default language for translation.
     * @return The translated content string.
     */
    private String translate(ContentNode content, List<Translation> translations, String defaultLanguage) {
        if (ObjectUtils.isNotEmpty(translations) && ObjectUtils.isNotEmpty(content)) {
            for (Translation translation : translations) {
                if (translation.getLanguage().trim().toUpperCase().equals(defaultLanguage.trim().toUpperCase())) {
                    content.setContent(content.getContent().replace("$translate(" + translation.getKey().trim() + ")", translation.getValue()));
                }
            }
        }
        return content.getContent();
    }

    /**
     * Fills content with values based on given values.
     *
     * @param content The ContentNode to fill values in.
     * @param values  List of values.
     * @return The content string with filled values.
     */
    private String fillValues(ContentNode content, List<Value> values) {
        if (ObjectUtils.isNotEmpty(values) && ObjectUtils.isNotEmpty(content)) {
            for (Value value : values) {
                content.setContent(content.getContent().replace("$value(" + value.getKey() + ")", value.getValue()));
            }
        }
        return content.getContent();
    }

    /**
     * Retrieves the parent Node from a list of Nodes based on the parent code.
     *
     * @param nodes List of Nodes.
     * @param code  Parent code to search for.
     * @return The parent Node, or null if not found.
     */
    private Node getParentNode(List<Node> nodes, String code) {
        if (ObjectUtils.isNotEmpty(nodes) && ObjectUtils.isNotEmpty(code)) {
            for (Node node : nodes) {
                if (node.getCode().equals(code)) {
                    return node;
                }
            }
        }
        return null;
    }

    /**
     * Retrieves parent Nodes for a list of ContentNodes.
     *
     * @param contentNodes List of ContentNodes.
     * @param status       The status of the Nodes.
     * @return A Flux emitting parent Nodes.
     */
    private Flux<Node> getNodes(List<ContentNode> contentNodes, StatusEnum status) {
        return Flux.fromIterable(contentNodes)
                .flatMap(contentNode -> this.findParentsRecursive(contentNode.getParentCode(), status))
                .distinct(Node::getCode);
    }

    /**
     * Retrieves parent Nodes for a single ContentNode.
     *
     * @param element The ContentNode.
     * @param status  The status of the Nodes.
     * @return A Flux emitting parent Nodes.
     */
    private Flux<Node> getNodes(ContentNode element, StatusEnum status) {
        return Mono.just(element)
                .flatMapMany(contentNode -> this.findParentsRecursive(contentNode.getParentCode(), status))
                .distinct(Node::getCode);
    }

    /**
     * Retrieves ContentNodes based on a list of content codes.
     *
     * @param contentCodes List of content codes.
     * @param status       The status of the ContentNodes.
     * @return A Flux emitting ContentNodes.
     */
    private Flux<ContentNode> getContents(List<String> contentCodes, StatusEnum status) {
        return Flux.fromIterable(contentCodes)
                .flatMap(contentCode -> this.contentNodeRepository.findByCodeAndStatus(contentCode, status.name()))
                .flatMap(this::addDisplay)
                .flatMap(contentNode -> {
                    if (ObjectUtils.isNotEmpty(contentNode) && ObjectUtils.isNotEmpty(contentNode.getContent()) && contentNode.getContent().contains("$content(")) {
                        return Flux.concat(Flux.just(contentNode), this.getContents(getContentCodes(contentNode), status));
                    }
                    return Flux.just(contentNode);
                })
                .distinct(ContentNode::getCode);
    }

    /**
     * Extracts content codes from a ContentNode's content.
     *
     * @param element The ContentNode.
     * @return List of extracted content codes.
     */
    private static List<String> getContentCodes(ContentNode element) {
        Matcher matcher = Pattern.compile("\\$content\\(.*\\)").matcher(element.getContent());
        List<String> codes = new LinkedList<>();
        while (matcher.find()) {
            String fragment = matcher.group();
            String code = fragment.replace("$content(", "").replace(")", "");
            codes.add(code);
        }
        return codes;
    }

    /**
     * Adds display information to a ContentNode.
     *
     * @param contentNode The ContentNode.
     * @return A Mono emitting the ContentNode with display information.
     */
    private Mono<ContentNode> addDisplay(ContentNode contentNode) {
        return this.contentDisplayHandler.findByContentCode(contentNode.getCode())
                .switchIfEmpty(Mono.just(new ContentDisplay()))
                .map(contentDisplay -> {
                    if (ObjectUtils.isEmpty(contentDisplay.getContentCode())) {
                        contentDisplay.setContentCode(contentNode.getCode());
                    }
                    return this.contentDisplayHandler.addDisplay(contentDisplay.getContentCode())
                            .map(contentDisplayMono -> contentNode);
                }).flatMap(Mono::from);
    }

    /**
     * Recursively finds parent Nodes based on parent code.
     *
     * @param parentCode The parent code.
     * @param status     The status of the Nodes.
     * @return A Flux emitting parent Nodes.
     */
    protected Flux<Node> findParentsRecursive(String parentCode, StatusEnum status) {
        if (ObjectUtils.isEmpty(parentCode)) {
            return Flux.empty();
        }

        return this.nodeRepository.findByCodeAndStatus(parentCode, status.name())
                .flatMapMany(node -> {
                    if (ObjectUtils.isNotEmpty(node.getParentCode())) {
                        return Flux.concat(
                                Flux.just(node),
                                findParentsRecursive(node.getParentCode(), status)
                        );
                    } else {
                        return Flux.just(node);
                    }
                });
    }
}