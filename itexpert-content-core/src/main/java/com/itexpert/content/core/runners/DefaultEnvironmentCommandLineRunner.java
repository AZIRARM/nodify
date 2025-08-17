package com.itexpert.content.core.runners;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.itexpert.content.core.handlers.NodeHandler;
import com.itexpert.content.core.handlers.UserHandler;
import com.itexpert.content.lib.enums.StatusEnum;
import com.itexpert.content.lib.models.ContentNode;
import com.itexpert.content.lib.models.Node;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.List;

@Slf4j
@Component
public class DefaultEnvironmentCommandLineRunner implements CommandLineRunner {

    private final Environment environment;
    @Value("${app.api-url}")
    private String apiUrl;

    private final NodeHandler nodeHandler;
    private final UserHandler userHandler;

    public DefaultEnvironmentCommandLineRunner(NodeHandler nodeHandler, UserHandler userHandler, Environment environment) {
        this.nodeHandler = nodeHandler;
        this.userHandler = userHandler;
        this.environment = environment;
    }

    public void run(String... args) {
        this.start();
    }

    private void start() {
        List<Node> environmentsToInitialize = List.of(
                createNode("Development", "Development environment", "DEV-01", "development"),
                createNode("Integration", "Integration environment", "INT-01", "integration"),
                createNode("Staging", "Staging environment", "STG-01", "staging"),
                createNode("PreProduction", "Pre-Production environment", "PREP-01", "pre-production"),
                createNode("Production", "Production environment", "PROD-01", "production")
        );

        Flux.fromIterable(environmentsToInitialize)
                .flatMap(this::initEnvironment)
                .subscribe(
                        importedNode -> log.info("Imported node: {}", importedNode.getCode()),
                        error -> log.error("Error initializing environments", error),
                        () -> log.info("All environments and templates initialized successfully")
                );
    }

    private Flux<Node> initEnvironment(Node env) {
        return nodeHandler.findByCodeAndStatus(env.getCode(), StatusEnum.SNAPSHOT.name())
                .flatMapMany(existing -> {
                    if ("DEV-01".equals(existing.getCode())) {
                        // Si DEV-01 existe déjà → on tente quand même d’importer les templates enfants
                        return importDevEnvironment(existing);
                    } else {
                        return Flux.just(existing);
                    }
                })
                .switchIfEmpty(
                        nodeHandler.save(env)
                                .flatMapMany(saved -> {
                                    if ("DEV-01".equals(saved.getCode())) {
                                        return importDevEnvironment(saved);
                                    } else {
                                        return Flux.just(saved);
                                    }
                                })
                );
    }

    private Flux<Node> importDevEnvironment(Node devEnv) {
        return userHandler.findByEmail("admin")
                .flatMap(user -> nodeHandler.publish(devEnv.getId(), user.getId())
                        .thenReturn(devEnv))
                .flatMapMany(publishedDevEnv -> Flux.fromIterable(List.of(
                                        "templates/Nodify-Blog.json",
                                        "templates/Nodify-Landingpage.json"
                                ))
                                .flatMap(template -> importTemplateChildrenOnly(publishedDevEnv, template))
                );
    }

    private Flux<Node> importTemplateChildrenOnly(Node parent, String templatePath) {
        return Mono.fromCallable(() -> {
                    ClassPathResource resource = new ClassPathResource(templatePath);
                    InputStreamReader reader = new InputStreamReader(resource.getInputStream());
                    Type listType = new TypeToken<List<Node>>() {}.getType();
                    return (List<Node>) new Gson().fromJson(reader, listType);
                })
                .flatMapMany(nodes -> nodeHandler.findChildrenByCodeAndStatus(parent.getCode(), StatusEnum.SNAPSHOT.name())
                        .collectList()
                        .flatMapMany(existingNodes -> {
                            if (existingNodes.isEmpty()) {
                                log.info("Importing template nodes from {}", templatePath);
                                return nodeHandler.importNodes(nodes, parent.getCode(), true);
                            } else {
                                log.info("Template nodes already exist for {}", parent.getCode());
                                return Flux.empty();
                            }
                        })
                );
    }

    private Node createNode(String name, String description, String code, String slug) {
        com.itexpert.content.lib.models.Value baseUrl = new com.itexpert.content.lib.models.Value();
        baseUrl.setValue(apiUrl);
        baseUrl.setKey("BASE_URL");

        Node node = new Node();
        node.setName(name);
        node.setDescription(description);
        node.setVersion("1");
        node.setDefaultLanguage("EN");
        node.setCode(code);
        node.setSlug(slug);
        node.setValues(List.of(baseUrl));

        if (code.equals("DEV-01")) {
            node.setFavorite(true);
        }

        return node;
    }
}
