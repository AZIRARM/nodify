package com.itexpert.content.api.handlers;

import com.itexpert.content.api.helpers.ContentHelper;
import com.itexpert.content.api.helpers.NodeHelper;
import com.itexpert.content.api.helpers.PluginHelper;
import com.itexpert.content.api.mappers.ContentNodeMapper;
import com.itexpert.content.api.repositories.ContentNodeRepository;
import com.itexpert.content.api.utils.ContentNodeView;
import com.itexpert.content.api.utils.RulesUtils;
import com.itexpert.content.lib.enums.ContentTypeEnum;
import com.itexpert.content.lib.enums.StatusEnum;
import com.itexpert.content.lib.models.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@AllArgsConstructor
@Service
public class ContentNodeHandler {
    private final ContentNodeRepository contentNodeRepository;
    private final ContentNodeMapper contentNodeMapper;
    private final ContentDisplayHandler contentDisplayHandler;
    private final ContentHelper contentHelper;
    private final PluginHelper pluginHelper;

    private final NodeHelper nodeHelper;

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

    public Flux<ContentNodeView> findAllByNodeCode(String code,
                                                   StatusEnum status,
                                                   String translation,
                                                   boolean fillValues) {
        return
                this.nodeHelper.evaluateNode(code, status).map(node -> code)
                        .map(nodeCode ->
                                this.contentNodeRepository.findAllByNodeCodeAndStatus(nodeCode, status.name())
                                        .map(contentNode -> this.findContentNodeByCode(contentNode.getCode(), status, translation))
                                        .flatMap(Mono::from)
                                        .map(contentNode -> RulesUtils.evaluateContentNode(contentNode)
                                                .filter(aBoolean -> aBoolean)
                                                .map(aBoolean -> contentNode)
                                        )
                                        .flatMap(Mono::from)
                                        .map(this.contentNodeMapper::fromModel)
                                        .flatMap(contentNode -> this.contentHelper.fillContents(contentNode, status, translation))
                                        .map(this.contentNodeMapper::fromEntity)
                                        .flatMap(this::addDisplay)
                                        .map(this.contentNodeMapper::toView).collectList()
                        ).flatMap(Mono::from).flatMapIterable(contentNodeViews -> contentNodeViews);
    }


    private Mono<Node> evaluateNodeByCodeContent(String codeContent, StatusEnum status) {
        return
                this.contentNodeRepository.findByCodeAndStatus(codeContent, status.name())
                        .map(contentNode -> this.nodeHelper.evaluateNode(contentNode.getParentCode(), status))
                        .flatMap(Mono::from);
    }

    public Mono<ContentNodeView> findByCodeAndStatus(String code,
                                                     StatusEnum status,
                                                     String translation) {
        return this.evaluateNodeByCodeContent(code, status)
                .map(node ->
                        this.findContentNodeByCode(code, status, translation)
                                .flatMap(this::addDisplay)
                                .map(this.contentNodeMapper::toView)
                ).flatMap(Mono::from);
    }

    public Mono<ContentNodeView> findBySlugAndStatus(String slug,
                                                     StatusEnum status,
                                                     String translation) {
        return
                this.contentNodeRepository.findBySlugAndStatus(slug, status.name())
                        .map(contentNode -> contentNode.getCode())
                        .flatMap(code ->
                                this.evaluateNodeByCodeContent(code, status)
                                        .map(node ->
                                                this.findContentNodeByCode(code, status, translation)
                                                        .flatMap(this::addDisplay)
                                                        .map(this.contentNodeMapper::toView)
                                        )).flatMap(Mono::from);
    }

    private Mono<ContentNode> findContentNodeByCode(String code,
                                                    StatusEnum status,
                                                    String translation) {
        return this.contentNodeRepository.findByCodeAndStatus(code, status.name())
                .filter(contentNode -> !contentNode.getType().equals(ContentTypeEnum.FILE) && !contentNode.getType().equals(ContentTypeEnum.PICTURE))
                .flatMap(contentNode ->
                        this.contentHelper.fillContents(contentNode, status, translation)
                                .flatMap(this.pluginHelper::fillPlugin)
                )
                .filter(ObjectUtils::isNotEmpty)
                .map(contentNodeMapper::fromEntity)
                .flatMap(contentNode -> RulesUtils.evaluateContentNode(contentNode)
                        .filter(aBoolean -> aBoolean)
                        .map(aBoolean -> contentNode)
                        .map(aBoolean -> addStatusParamIfNeeded(contentNode))
                );
    }

    private ContentNode addStatusParamIfNeeded(ContentNode contentNode) {
        String html = contentNode.getContent();
        String status = contentNode.getStatus().name();

        // Regex: capture src="...contents/(code/.../file | file/.../code)..."
        Pattern urlPattern = Pattern.compile(
                "(src|href)=\"([^\"]*?/contents/(?:code/[^/]+/file|file/[^/]+/code))(.*?)\""
        );
        Matcher matcher = urlPattern.matcher(html);

        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String before = matcher.group(2); // l’URL de base
            String after = matcher.group(3);  // éventuels query params

            String newUrl = before + after;
            if (!newUrl.contains("status=")) {
                if (newUrl.contains("?")) {
                    newUrl = newUrl + "&status=" + status;
                } else {
                    newUrl = newUrl + "?status=" + status;
                }
            }

            matcher.appendReplacement(
                    sb,
                    matcher.group(1) + "=\"" + Matcher.quoteReplacement(newUrl) + "\""
            );
        }
        matcher.appendTail(sb);

        contentNode.setContent(sb.toString());
        return contentNode;
    }


    private Integer getDatasIndexByKey(List<Value> datas, String key) {
        for (int i = 0; i < datas.size(); i++) {
            if (datas.get(i).getKey().equals(key))
                return i;
        }
        return -1;
    }

    public Mono<ContentFile> findResourceByCode(String code, StatusEnum status) {
        return
                this.evaluateNodeByCodeContent(code, status)
                        .map(node ->
                                this.contentNodeRepository.findByCodeAndStatus(code, status.name())
                                        .map(contentNodeMapper::fromEntity)
                                        .flatMap(contentNode -> RulesUtils.evaluateContentNode(contentNode)
                                                .filter(aBoolean -> aBoolean)
                                                .map(aBoolean -> contentNode)
                                        )
                                        .filter(contentNode -> ObjectUtils.isNotEmpty(contentNode.getFile()))
                                        .flatMap(this::addDisplay)
                                        .map(ContentNode::getFile)
                        ).flatMap(Mono::from);
    }

    public Mono<ContentFile> findResourceBySlug(String slug, StatusEnum status) {
        return
                this.contentNodeRepository.findBySlugAndStatus(slug, status.name())
                        .map(contentNode -> contentNode.getCode())
                        .flatMap(code ->
                                this.evaluateNodeByCodeContent(code, status)
                                        .map(node ->
                                                this.contentNodeRepository.findByCodeAndStatus(node.getCode(), status.name())
                                                        .map(contentNodeMapper::fromEntity)
                                                        .flatMap(contentNode -> RulesUtils.evaluateContentNode(contentNode)
                                                                .filter(aBoolean -> aBoolean)
                                                                .map(aBoolean -> contentNode)
                                                        )
                                                        .filter(contentNode -> ObjectUtils.isNotEmpty(contentNode.getFile()))
                                                        .flatMap(this::addDisplay)
                                                        .map(ContentNode::getFile)
                                        )).flatMap(Mono::from);
    }
}

