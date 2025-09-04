package com.itexpert.content.core.runners;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.itexpert.content.core.handlers.NodeHandler;
import com.itexpert.content.core.handlers.UserHandler;
import com.itexpert.content.lib.enums.StatusEnum;
import com.itexpert.content.lib.models.Node;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.List;

@Slf4j
@Component
public class DevTemplatesInitializer {

    private final NodeHandler nodeHandler;
    private final UserHandler userHandler;

    public DevTemplatesInitializer(NodeHandler nodeHandler, UserHandler userHandler) {
        this.nodeHandler = nodeHandler;
        this.userHandler = userHandler;
    }

    public Mono<Void> init() {
        return nodeHandler.findByCodeAndStatus("DEV-01", StatusEnum.SNAPSHOT.name())
                .flatMap(devEnv ->
                        userHandler.findByEmail("admin")
                                .flatMap(user ->
                                        Flux.fromIterable(List.of(
                                                        "templates/Nodify-Blog.json",
                                                        "templates/Nodify-Landingpage.json",
                                                        "templates/Nodify-News.json",
                                                        "templates/Nodify-EMarket.json",
                                                        "templates/Nodify-Ebooks.json",
                                                        "templates/Nodify-Gallery.json",
                                                        "templates/Nodify-Music.json"
                                                ))
                                                .concatMap(template -> importTemplateChildrenOnly(devEnv, template))
                                                .collectList() // collecte tous les résultats des imports
                                                .flatMap(importedNodes -> {
                                                    if (!importedNodes.isEmpty()) {
                                                        log.info("Publishing DEV-01 after importing templates...");
                                                        return nodeHandler.publish(devEnv.getCode(), "Nodify").then();
                                                    } else {
                                                        log.info("No templates were imported, skipping publish.");
                                                        return Mono.empty();
                                                    }
                                                })
                                )
                );
    }

    private Flux<Node> importTemplateChildrenOnly(Node parent, String templatePath) {
        return Mono.fromCallable(() -> {
                    ClassPathResource resource = new ClassPathResource(templatePath);
                    InputStreamReader reader = new InputStreamReader(resource.getInputStream());
                    Type listType = new TypeToken<List<Node>>() {
                    }.getType();
                    return (List<Node>) new Gson().fromJson(reader, listType);
                })
                .flatMapMany(nodes ->
                        nodeHandler.findChildrenByCodeAndStatus(parent.getCode(), StatusEnum.SNAPSHOT.name())
                                .collectList()
                                .flatMapMany(existingNodes -> {
                                    if (existingNodes.size() <= 6 && parent.getVersion().equals("0")) {
                                        log.info("Importing template nodes from {}", templatePath);
                                        return nodeHandler.importNodes(nodes, parent.getCode(), true);
                                    } else {
                                        log.info("Template nodes already exist for {}", parent.getCode());
                                        return Flux.empty();
                                    }
                                })
                );
    }

}

