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
        if (ObjectUtils.isNotEmpty(content)
                && ObjectUtils.isNotEmpty(plugin)
                && ObjectUtils.isNotEmpty(plugin.getCode())
                && ObjectUtils.isNotEmpty(content.getContent())) {

            String withSimple = "$with(" + plugin.getName() + ")";
            String code = plugin.getCode();

            if (plugin.isEnabled()) {
                content.setContent(
                        content.getContent().replace(
                                withSimple,
                                "\n<script>\n" + code + "\n</script>\n"
                        )
                );
            } else {
                content.setContent(content.getContent().replace(withSimple, ""));
            }
        }
        return content;
    }

    private Flux<String> getContentPlugin(ContentNode element) {
        Matcher matcher = Pattern.compile("\\$with\\(([^)]*)\\)").matcher(element.getContent());
        List<String> plugins = new LinkedList<>();
        while (matcher.find()) {
            String fragment = matcher.group(1); // contenu entre parenthèses
            plugins.add(fragment.trim()); // plus besoin de split sur ","
        }
        return Flux.fromIterable(plugins);
    }


}