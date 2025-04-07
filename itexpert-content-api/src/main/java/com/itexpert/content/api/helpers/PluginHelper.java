package com.itexpert.content.api.helpers;

import com.itexpert.content.api.repositories.PluginRepository;
import com.itexpert.content.lib.entities.ContentNode;
import com.itexpert.content.lib.entities.Plugin;
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
public class PluginHelper {

    private final PluginRepository pluginRepository;

    public Mono<ContentNode> fillPlugin(ContentNode element) {
        return this.fillPluginFactory(element)
                .onErrorReturn(element); // Returns original element on error.
    }


    private Mono<ContentNode> fillPluginFactory(ContentNode element) {

        return Mono.just(element).flatMap(contentNode -> getContentPlugin(contentNode)
                .flatMap(this.pluginRepository::findByName)
                .map(plugin -> this.fillPlugin(plugin, contentNode))
                .collectList()
                .thenReturn(element));
    }

    private ContentNode fillPlugin(Plugin plugin, ContentNode content) {
        if (ObjectUtils.isNotEmpty(content) && ObjectUtils.isNotEmpty(plugin) && ObjectUtils.isNotEmpty(plugin.getCode()) && ObjectUtils.isNotEmpty(content.getContent())) {
            content.setContent(content.getContent().replace("$with(" + plugin.getName() + ")", "\n<script>\n" + plugin.getCode() + "\n</script>\n"));
        }
        return content;
    }

    private Flux<String> getContentPlugin(ContentNode element) {
        Matcher matcher = Pattern.compile("\\$with\\(.*\\)").matcher(element.getContent());
        List<String> plugins = new LinkedList<>();
        while (matcher.find()) {
            String fragment = matcher.group();
            String code = fragment.replace("$with(", "").replace(")", "");
            plugins.add(code);
        }
        return Flux.fromIterable(plugins);
    }
}