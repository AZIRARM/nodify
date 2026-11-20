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
                                            renderAsJsonValue(child.getContent()) // <-- C'est ici que la magie opère
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
     * Escapes a raw value for insertion INTO an existing JSON string
     * (used for $value(...) and $translate(...) which are already inside quotes).
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
     * Renders the content of a child ContentNode as a valid JSON value.
     * 
     * Three cases handled:
     * 1. Child is a valid JSON object/array -> injected natively (no extra quotes).
     * 2. Child is a stringified JSON in DB (e.g., "{\"name\":\"fetch\"}") -> unwrapped and injected natively.
     * 3. Child is plain text/code (e.g., Python) -> injected as a properly escaped JSON string.
     * 
     * Additionally, it strips HTML entities (\u003E, \u003C, \u0026) that Jackson 
     * or Spring might inject by default, which would break Python code (e.g., >, <, &).
     */
    private String renderAsJsonValue(String raw) {
        if (raw == null) return "null";
        
        try {
            // First parsing attempt
            JsonNode node = JSON_MAPPER.readTree(raw);
            
            // CRITICAL FIX: If the parsed node is a TextNode, it means the content 
            // in the database is a stringified JSON (e.g., "{\"name\":\"fetch\"}").
            // We must try to parse the inner string to get the actual JSON object/array.
            if (node.isTextual()) {
                String innerText = node.asText();
                try {
                    // Second parsing attempt on the inner text
                    JsonNode innerNode = JSON_MAPPER.readTree(innerText);
                    // Successfully parsed inner JSON! Return it without surrounding quotes.
                    return unescapeHtmlEntities(JSON_MAPPER.writeValueAsString(innerNode));
                } catch (Exception innerEx) {
                    // Inner text is NOT JSON (e.g., it's your Python code). 
                    // We MUST return it as a valid JSON string (with quotes and escaped).
                    try {
                        return unescapeHtmlEntities(JSON_MAPPER.writeValueAsString(innerText));
                    } catch (Exception ex) {
                        // Ultimate fallback: manual escaping
                        return "\"" + jsonEscape(innerText) + "\"";
                    }
                }
            }
            
            // It's already a valid JSON object, array, or primitive (number, boolean).
            // Return it as-is (no extra quotes added).
            return unescapeHtmlEntities(JSON_MAPPER.writeValueAsString(node));
            
        } catch (Exception e) {
            // The raw content is not JSON at all (e.g., a plain Python script).
            // We MUST return it as a valid JSON string (with quotes and escaped).
            try {
                return unescapeHtmlEntities(JSON_MAPPER.writeValueAsString(raw));
            } catch (Exception ex) {
                // Ultimate fallback: manual escaping if Jackson somehow fails on a simple string
                return "\"" + jsonEscape(raw) + "\"";
            }
        }
    }

    /**
     * Helper method to strip HTML entities injected by Jackson or Spring Security.
     * Jackson escapes <, >, and & to \u003C, \u003E, and \u0026 by default to prevent XSS.
     * This breaks Python code (e.g., 'if len(x) > 10'), so we revert them to their raw characters.
     */
    private String unescapeHtmlEntities(String json) {
        if (json == null) return null;
        return json.replace("\\u003E", ">")
                   .replace("\\u003C", "<")
                   .replace("\\u0026", "&");
    }
}