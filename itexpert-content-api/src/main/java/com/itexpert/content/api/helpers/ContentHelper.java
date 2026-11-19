package com.itexpert.content.api.helpers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itexpert.content.api.handlers.ContentDisplayHandler;
import com.itexpert.content.api.mappers.ContentNodeMapper;
import com.itexpert.content.api.repositories.ContentNodeRepository;
import com.itexpert.content.api.repositories.NodeRepository;
import com.itexpert.content.api.repositories.PluginRepository;
import com.itexpert.content.api.utils.RulesUtils;
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

import java.util.ArrayList;
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
    private final PluginRepository pluginRepository;
    private final PluginHelper pluginHelper;
    private static final Pattern CONTENT_PATTERN = Pattern.compile("\\$content\\(([^)]+)\\)");
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    public Mono<ContentNode> fillContents(ContentNode element, StatusEnum status, String translation) {
        return this.fillContentFactory(element, status, translation)
                .onErrorReturn(element);
    }

    Mono<ContentNode> fillContentFactory(ContentNode element, StatusEnum status, String translation) {
        List<String> codes = extractContentCodes(element.getContent());

        if (codes.isEmpty()) {
            return finalizeContent(element, status, translation)
                    .flatMap(finalized ->
                            RulesUtils.evaluateContentNode(contentNodeMapper.fromEntity(finalized))
                                    .filter(Boolean::booleanValue)
                                    .map(keep -> finalized)
                    );
        }

        return getContents(codes, status)
                .collectList()
                .flatMap(children -> {
                    if (children.isEmpty()) {
                        log.warn("No ContentNode resolved for codes={} with status={} (parent code={})",
                                codes, status, element.getCode());
                    }
                    return Flux.fromIterable(children)
                            .flatMap(child -> fillContentFactory(child, status, translation))
                            .collectList()
                            .flatMap(resolvedChildren -> {
                                String newContent = element.getContent();
                                for (ContentNode child : resolvedChildren) {
                                    newContent = newContent.replace(
                                            "$content(" + child.getCode() + ")",
                                            renderAsJsonValue(child.getContent())
                                    );
                                }
                                element.setContent(newContent);

                                return finalizeContent(element, status, translation)
                                        .flatMap(finalized ->
                                                RulesUtils.evaluateContentNode(contentNodeMapper.fromEntity(finalized))
                                                        .filter(Boolean::booleanValue)
                                                        .map(keep -> finalized)
                                        );
                            });
                });
    }

    private List<String> extractContentCodes(String content) {
        List<String> codes = new ArrayList<>();
        if (content == null) return codes;
        Matcher matcher = CONTENT_PATTERN.matcher(content);
        while (matcher.find()) {
            codes.add(matcher.group(1).trim());
        }
        return codes;
    }

    private Mono<ContentNode> finalizeContent(ContentNode element, StatusEnum status, String translation) {
        return this.getNodes(element, status)
                .collectList()
                .map(nodes -> {
                    Node parentNode = this.getParentNode(nodes, element.getParentCode());
                    if (parentNode == null) {
                        log.warn("No parent Node found for ContentNode code={} parentCode={} status={} " +
                                 "— $value()/$translate() will NOT be resolved",
                                element.getCode(), element.getParentCode(), status);
                    }
                    if (parentNode != null) {
                        element.setContent(this.translate(element, element.getTranslations(),
                                ObjectUtils.isNotEmpty(translation) ? translation : parentNode.getDefaultLanguage()));
                        element.setContent(this.fillValues(element, element.getValues()));

                        nodes.forEach(node -> {
                            element.setContent(this.translate(element, node.getTranslations(),
                                    ObjectUtils.isNotEmpty(translation) ? translation : parentNode.getDefaultLanguage()));
                            element.setContent(this.fillValues(element, node.getValues()));
                        });
                    }
                    return element;
                })
                .flatMap(contentNode -> this.pluginHelper.fillPlugin(contentNode)
                        .thenReturn(element));
    }

    private String translate(ContentNode content, List<Translation> translations, String defaultLanguage) {
        if (ObjectUtils.isNotEmpty(translations) && ObjectUtils.isNotEmpty(content)) {
            for (Translation translation : translations) {
                if (translation.getLanguage().trim().toUpperCase().equals(defaultLanguage.trim().toUpperCase())) {
                    content.setContent(content.getContent().replace(
                            "$translate(" + translation.getKey().trim() + ")",
                            jsonEscape(translation.getValue())
                    ));
                }
            }
        }
        return content.getContent();
    }

    private String fillValues(ContentNode content, List<Value> values) {
        if (ObjectUtils.isNotEmpty(values) && ObjectUtils.isNotEmpty(content)) {
            for (Value value : values) {
                content.setContent(content.getContent().replace(
                        "$value(" + value.getKey() + ")",
                        jsonEscape(value.getValue())
                ));
            }
        }
        return content.getContent();
    }

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

    private Flux<Node> getNodes(List<ContentNode> contentNodes, StatusEnum status) {
        return Flux.fromIterable(contentNodes)
                .flatMap(contentNode -> this.findParentsRecursive(contentNode.getParentCode(), status))
                .distinct(Node::getCode);
    }

    private Flux<Node> getNodes(ContentNode element, StatusEnum status) {
        return Mono.just(element)
                .flatMapMany(contentNode -> this.findParentsRecursive(contentNode.getParentCode(), status))
                .distinct(Node::getCode);
    }

    private Flux<ContentNode> getContents(List<String> contentCodes, StatusEnum status) {
        return Flux.fromIterable(contentCodes)
                .flatMap(contentCode -> this.contentNodeRepository.findByCodeAndStatus(contentCode, status.name()))
                .flatMap(this::addDisplay)
                .flatMap(contentNode -> {
                    if (ObjectUtils.isNotEmpty(contentNode)
                            && ObjectUtils.isNotEmpty(contentNode.getContent())
                            && contentNode.getContent().contains("$content(")) {
                        return Flux.concat(Flux.just(contentNode), this.getContents(getContentCodes(contentNode), status));
                    }
                    return Flux.just(contentNode);
                })
                .distinct(ContentNode::getCode);
    }

    private static List<String> getContentCodes(ContentNode element) {
        List<String> codes = new LinkedList<>();
        if (element == null || element.getContent() == null) return codes;
        Matcher matcher = CONTENT_PATTERN.matcher(element.getContent());
        while (matcher.find()) {
            codes.add(matcher.group(1).trim());
        }
        return codes;
    }

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

    public Mono<com.itexpert.content.lib.models.ContentNode> evaluateContent(String code, StatusEnum status) {
        return this.findByCodeAndStatus(code, status)
                .switchIfEmpty(Mono.empty());
    }

    public Mono<com.itexpert.content.lib.models.ContentNode> findByCodeAndStatus(String code, StatusEnum status) {
        return contentNodeRepository.findByCodeAndStatus(code, status.name())
                .map(contentNodeMapper::fromEntity)
                .flatMap(content -> RulesUtils.evaluateContentNode(content)
                        .filter(aBoolean -> aBoolean)
                        .map(aBoolean -> content)
                );
    }

    /**
     * Échappe une valeur brute pour insertion DANS une chaîne JSON existante
     * (cas des $value(...) et $translate(...) qui sont déjà entre guillemets).
     */
    private String jsonEscape(String raw) {
        if (raw == null) return null;
        return raw.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * Rend le contenu d'un ContentNode enfant comme une VALEUR JSON valide,
     * à injecter à la place d'un $content(...) qui est en position de valeur.
     *
     * Deux cas :
     *  - contenu enfant = JSON valide (objet, tableau, scalaire) → injecté tel quel
     *  - contenu enfant = texte brut (Python, SQL, HTML…) → injecté comme string JSON échappée
     */
    private String renderAsJsonValue(String raw) {
        if (raw == null) return "null";
        try {
            JsonNode node = JSON_MAPPER.readTree(raw);
            return JSON_MAPPER.writeValueAsString(node);
        } catch (Exception e) {
            log.debug("Child content is not JSON — rendering as JSON string value");
            try {
                return JSON_MAPPER.writeValueAsString(raw);
            } catch (Exception ex) {
                log.error("Cannot serialize child content as JSON string", ex);
                return "\"\"";
            }
        }
    }
}