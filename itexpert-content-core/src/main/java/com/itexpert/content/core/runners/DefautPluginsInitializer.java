package com.itexpert.content.core.runners;

import com.google.gson.Gson;
import com.itexpert.content.core.handlers.PluginHandler;
import com.itexpert.content.lib.models.Plugin;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.InputStreamReader;

@Slf4j
@Component
@AllArgsConstructor
public class DefautPluginsInitializer {

    private final PluginHandler pluginHandler;


    public Mono<Void> init() {
        return Flux.merge(
                this.importPlugin("plugins/jquery3.7.1.json"),
                this.importPlugin("plugins/bootstrap5.0.2.json"),
                this.importPlugin("plugins/feedbacks.json"),
                this.importPlugin("plugins/clicks.json"),
                this.importPlugin("plugins/comments.json"),
                this.importPlugin("plugins/epubjs.json")
        ).collectList().then();

    }

    private Mono<Plugin> importPlugin(String template) {
        return Mono.fromCallable(() -> {
                    ClassPathResource resource = new ClassPathResource(template);
                    InputStreamReader reader = new InputStreamReader(resource.getInputStream());
                    return new Gson().fromJson(reader, Plugin.class);
                })
                .flatMap(plugin -> {
                    plugin.setEditable(false);
                    return pluginHandler.findByName(plugin.getName())
                            .switchIfEmpty(pluginHandler.importPlugin(plugin))
                            .doOnNext(existing -> {
                                if (existing.getId() != null) {
                                    log.info("Plugin already exists : {}", plugin.getName());
                                }
                            });
                })
                .doOnError(e -> log.warn("Error importing plugin: {}", e.getMessage()));
    }


}
