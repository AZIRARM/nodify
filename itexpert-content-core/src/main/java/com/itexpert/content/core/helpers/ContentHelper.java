package com.itexpert.content.core.helpers;

import com.itexpert.content.core.mappers.ContentNodeMapper;
import com.itexpert.content.core.repositories.ContentNodeRepository;
import com.itexpert.content.core.repositories.NodeRepository;
import com.itexpert.content.core.utils.RulesUtils;
import com.itexpert.content.lib.entities.ContentNode;
import com.itexpert.content.lib.entities.Node;
import com.itexpert.content.lib.enums.StatusEnum;
import com.itexpert.content.lib.models.Translation;
import com.itexpert.content.lib.models.Value;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@AllArgsConstructor
@Slf4j
public class ContentHelper {

    private final NodeRepository nodeRepository;
    private final ContentNodeRepository contentNodeRepository;
    private final ContentNodeMapper contentNodeMapper;

    public Mono<ContentNode> translate(ContentNode element, String translation, StatusEnum status) {
        return Mono.just(element)
                .map(contentNode ->
                        this.findAllByNodeChildreenCode(contentNode.getParentCode(), status)
                                .flatMap(node ->
                                        this.translateFactory(element,
                                                node.getTranslations(),
                                                ObjectUtils.isNotEmpty(translation) ? translation : node.getDefaultLanguage())
                                ).collectList().thenReturn(element)
                ).flatMap(Mono::from);
    }


    private Mono<ContentNode> translateFactory(ContentNode element, List<Translation> translations, String translation) {

        List<Translation> allTranslations = new LinkedList<>();
        if (ObjectUtils.isNotEmpty(element.getTranslations())) {
            allTranslations.addAll(element.getTranslations());
        }
        if (ObjectUtils.isNotEmpty(translations)) {
            allTranslations.addAll(translations);
        }
        return Flux.fromIterable(allTranslations)
                .map(tran -> this.setTranslation(element, translation, tran))
                .collectList().thenReturn(element);
    }

    private Translation setTranslation(ContentNode element, String language, Translation tr) {
        if (language.equalsIgnoreCase(tr.getLanguage())) {
            element.setContent(element.getContent().replace("$trans(" + tr.getKey().trim() + ")", tr.getValue()));
        }
        return tr;
    }


    public Mono<ContentNode> fillValues(ContentNode element, StatusEnum status) {
        return Mono.just(element)
                .map(contentNode ->
                        this.findAllByNodeChildreenCode(contentNode.getParentCode(), status)
                                .flatMap(node ->
                                        this.fillValuesFactroy(element,
                                                node.getValues())
                                ).collectList().thenReturn(element)
                ).flatMap(Mono::from);

    }

    private Mono<ContentNode> fillValuesFactroy(ContentNode element, List<Value> values) {

        List<Value> allValues = new LinkedList<>();
        if (ObjectUtils.isNotEmpty(element.getValues())) {
            allValues.addAll(element.getValues());
        }
        if (ObjectUtils.isNotEmpty(values)) {
            allValues.addAll(values);
        }
        return Flux.fromIterable(allValues)
                .map(val -> this.setValue(element, val))
                .collectList()
                .map(ignored -> element);
    }

    private ContentNode setValue(ContentNode element, Value val) {
        element.setContent(element.getContent().replace("$val(" + val.getKey() + ")", val.getValue()));
        return element;
    }


    public Mono<ContentNode> fillContents(ContentNode element, StatusEnum status) {

        return this.fillContentFactory(element, status)
                .onErrorReturn(element);
    }

    private Mono<ContentNode> fillContentFactory(ContentNode element, StatusEnum status) {
        if (ObjectUtils.isNotEmpty(element) && ObjectUtils.isNotEmpty(element.getContent()) && element.getContent().contains("$content(")) {
            return Flux.fromIterable(getContentCodes(element))
                    .map(contentCode -> contentCode.replace("$content(", "").replace(")", ""))
                    .flatMap(contentCode -> fillContent(element, contentCode, status)
                            .filter(filtred -> filtred.getContent().contains("$content("))
                            .flatMap(contentNode -> fillContentFactory(element, status))
                            .switchIfEmpty(Mono.just(element))
                    )
                    .last().map(ContentHelper::cleanJson);
        }
        return Mono.just(element);
    }

    private Mono<ContentNode> fillContent(ContentNode element, String contentCode, StatusEnum status) {
        return this.contentNodeRepository.findByCodeAndStatus(contentCode, status.name())
                .map(contentNodeMapper::fromEntity)
                .flatMap(contentNode -> RulesUtils.evaluateContentNode(contentNode)
                        .filter(aBoolean -> aBoolean)
                        .map(aBoolean -> contentNode)
                )
                .map(contentNode -> {
                    String elementContent = ObjectUtils.isNotEmpty(contentNode.getContent()) ? contentNode.getContent() : "";
                    String content = element.getContent().replace("$content(" + contentCode + ")", elementContent);
                    element.setContent(content);
                    return element;
                });
    }


    private static ContentNode cleanJson(ContentNode content) {
        final String regex = "\\\"\\{[\\s\\S]*?\\}\\\"";

        Matcher matcher = Pattern.compile(regex).matcher(content.getContent());
        List<String> contentCodes = new LinkedList<>();
        while (matcher.find()) {
            String token = matcher.group();
            content.setContent(
                    content.getContent().replace(token, token.substring(1, token.length() - 1))
            );
        }
        return content;
    }

    private static List<String> getContentCodes(ContentNode element) {
        Matcher matcher = Pattern.compile("\\$content\\(.*\\)").matcher(element.getContent());
        List<String> contentCodes = new LinkedList<>();
        while (matcher.find()) {
            contentCodes.add(matcher.group());
        }
        return contentCodes;
    }


    private Flux<Node> findAllByNodeChildreenCode(String codeChildreen, StatusEnum status) {
        // Démarre la recherche récursive avec un accumulateur vide
        return Flux.defer(() -> findParentsRecursive(codeChildreen, status, Flux.empty()))
                .collectList()
                .doOnNext(list -> {
                    log.info(list.toString());
                })
                .flatMapIterable(list -> list);
    }

    private Flux<Node> findParentsRecursive(String code, StatusEnum status, Flux<Node> accumulated) {
        return this.nodeRepository.findByCodeAndStatus(code, status.name())
                .flatMapMany(node -> {
                    // Ajoute le nœud actuel à l'accumulateur
                    Flux<Node> updatedAccumulated = Flux.concat(accumulated, Flux.just(node));

                    String parentCode = node.getParentCode();
                    if (ObjectUtils.isNotEmpty(parentCode)) {
                        // Appelle récursivement pour les parents
                        return findParentsRecursive(parentCode, status, updatedAccumulated);
                    } else {
                        // Retourne l'accumulateur final si pas de parent
                        return updatedAccumulated;
                    }
                })
                .switchIfEmpty(accumulated); // Si le code initial n'existe pas ou n'a pas de correspondance
    }


}
