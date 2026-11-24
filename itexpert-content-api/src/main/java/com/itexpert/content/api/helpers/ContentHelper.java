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
    private static final ObjectMapper JSON_MAPPER;
    static {
        JSON_MAPPER = new ObjectMapper();
        JSON_MAPPER.configure(com.fasterxml.jackson.core.JsonGenerator.Feature.ESCAPE_NON_ASCII, false);
    }

    public Mono<ContentNode> fillContents(ContentNode element, StatusEnum status, String translation) {
        return this.fillContentFactory(element, status, translation)
                .doOnError(e -> log.error(
                        "fillContents failed for code={} status={} translation={} — returning raw element",
                        element != null ? element.getCode() : null,
                        status,
                        translation,
                        e))
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

                    String lang = ObjectUtils.isNotEmpty(translation)
                            ? translation
                            : (parentNode != null && ObjectUtils.isNotEmpty(parentNode.getDefaultLanguage())
                               ? parentNode.getDefaultLanguage()
                               : "en");

                    if (parentNode == null) {
                        log.warn("No parent Node found for ContentNode code={} parentCode={} status={} — " +
                                        "falling back to language='{}' for $translate()/$value() resolution",
                                element.getCode(), element.getParentCode(), status, lang);
                    }

                    // 1) Toujours résoudre l'élément courant (ses propres Value/Translation)
                    element.setContent(this.translate(element, element.getTranslations(), lang));
                    element.setContent(this.fillValues(element, element.getValues()));

                    // 2) Puis celles héritées des ancêtres
                    for (Node node : nodes) {
                        element.setContent(this.translate(element, node.getTranslations(), lang));
                        element.setContent(this.fillValues(element, node.getValues()));
                    }
                    return element;
                })
                .flatMap(contentNode -> this.pluginHelper.fillPlugin(contentNode)
                        .thenReturn(element));
    }

    private String translate(ContentNode content, List<Translation> translations, String defaultLanguage) {
        if (ObjectUtils.isNotEmpty(translations)
                && ObjectUtils.isNotEmpty(content)
                && ObjectUtils.isNotEmpty(defaultLanguage)) {
            for (Translation translation : translations) {
                if (translation.getKey() == null || translation.getLanguage() == null) {
                    continue;
                }
                if (!translation.getLanguage().trim().equalsIgnoreCase(defaultLanguage.trim())) {
                    continue;
                }
                String replacement = jsonEscape(translation.getValue());
                if (replacement == null) {
                    log.warn("Translation '{}' (lang={}) has null value on ContentNode code={} — $translate({}) left unresolved",
                            translation.getKey(), defaultLanguage, content.getCode(), translation.getKey());
                    continue;
                }
                content.setContent(content.getContent().replace(
                        "$translate(" + translation.getKey().trim() + ")",
                        replacement
                ));
            }
        }
        return content.getContent();
    }

    private String fillValues(ContentNode content, List<Value> values) {
        if (ObjectUtils.isNotEmpty(values) && ObjectUtils.isNotEmpty(content)) {
            for (Value value : values) {
                if (value.getKey() == null) {
                    continue;
                }
                String replacement = jsonEscape(value.getValue());
                if (replacement == null) {
                    log.warn("Value '{}' is null on ContentNode code={} — $value({}) left unresolved",
                            value.getKey(), content.getCode(), value.getKey());
                    continue;
                }
                content.setContent(content.getContent().replace(
                        "$value(" + value.getKey() + ")",
                        replacement
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
                .flatMap(contentCode -> this.contentNodeRepository.findByCodeAndStatus(contentCode, status.name())
                        .doOnNext(c -> log.debug("Resolved child ContentNode code={} status={}", c.getCode(), status))
                        .switchIfEmpty(Mono.fromRunnable(() -> log.warn(
                                "Child ContentNode NOT FOUND code={} status={}", contentCode, status))))
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
     * Escapes a raw value for insertion INTO an existing JSON string.
     * Returns {@code null} when {@code raw} is null — callers MUST check for null
     * (String.replace throws NPE if replacement is null).
     */
    private String jsonEscape(String raw) {
        if (raw == null) return null;
        return raw.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String renderAsJsonValue(String raw) {
        if (raw == null) return "null";

        try {
            JsonNode node = JSON_MAPPER.readTree(raw);

            if (node.isTextual()) {
                String innerText = node.asText();
                try {
                    JsonNode innerNode = JSON_MAPPER.readTree(innerText);
                    return unescapeHtmlEntities(JSON_MAPPER.writeValueAsString(innerNode));
                } catch (Exception innerEx) {
                    try {
                        return unescapeHtmlEntities(JSON_MAPPER.writeValueAsString(innerText));
                    } catch (Exception ex) {
                        return "\"" + jsonEscape(innerText) + "\"";
                    }
                }
            }

            return unescapeHtmlEntities(JSON_MAPPER.writeValueAsString(node));

        } catch (Exception e) {
            try {
                return unescapeHtmlEntities(JSON_MAPPER.writeValueAsString(raw));
            } catch (Exception ex) {
                return "\"" + jsonEscape(raw) + "\"";
            }
        }
    }

    private String unescapeHtmlEntities(String json) {
        if (json == null) return null;
        return json.replace("\\u003E", ">")
                .replace("\\u003C", "<")
                .replace("\\u0026", "&");
    }
}