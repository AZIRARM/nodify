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
        List<Node> environments = List.of(
                createNode("Development", "Development environment", "DEV-01", "development"),
                createNode("Integration", "Integration environment", "INT-01", "integration"),
                createNode("Staging", "Staging environment", "STG-01", "staging"),
                createNode("PreProduction", "Pre-Production environment", "PREP-01", "pre-production"),
                createNode("Production", "Production environment", "PROD-01", "production")
        );

        nodeHandler.findByCode("DEV-01")
                .hasElements()  // renvoie Mono<Boolean> true s'il existe au moins un node avec ce code
                .flatMapMany(exists -> {
                    if (!exists) {
                        log.info("No Development environment found, creating default environments...");
                        return nodeHandler.saveAll(environments)
                                .doOnNext(node -> log.info("Node saved, code: {}", node.getCode()))
                                .flatMap(environment ->
                                        userHandler.findByEmail("admin")
                                                .flatMap(userPost -> nodeHandler.publish(environment.getId(), userPost.getId()))
                                                .doOnNext(published -> log.info("Published environment: {}", environment.getCode()))
                                                .thenReturn(environment)
                                );
                    } else {
                        log.info("Development environment already exists, skipping creation.");
                        return Flux.empty();
                    }
                })
                .filter(environment -> environment.getCode().equals("DEV-01"))
                .flatMap(node -> Flux.fromIterable(List.of(
                                        "templates/Nodify-Blog.json",
                                        "templates/Nodify-Landingpage.json"
                                ))
                                .flatMap(template -> this.importTemplate(node, template))
                )
                .subscribe(
                        importedNode -> log.info("Imported node: {}", importedNode.getCode()),
                        error -> log.error("Error initializing environments", error),
                        () -> log.info("Default environments initialized successfully")
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

    private Flux<Node> importTemplate(Node environment, String template) {
        return Mono.fromCallable(() -> {
                    ClassPathResource resource = new ClassPathResource(template);
                    InputStreamReader reader = new InputStreamReader(resource.getInputStream());
                    Type listType = new TypeToken<List<Node>>() {
                    }.getType();
                    return (List<Node>) new Gson().fromJson(reader, listType); // <-- Ajout du cast ici
                })
                .flatMapMany(nodes -> nodeHandler.findChildrenByCodeAndStatus("DEV-01", StatusEnum.SNAPSHOT.name())
                        .map(node -> {
                            if (ObjectUtils.isEmpty(node.getSlug())) {
                                node.setSlug(node.getCode());
                            }
                            if (ObjectUtils.isNotEmpty(node.getContents())) {
                                for (ContentNode content : node.getContents()) {
                                    if (ObjectUtils.isEmpty(content.getSlug())) {
                                        content.setSlug(content.getCode());
                                    }
                                }
                            }
                            return node;
                        })
                        .collectList()
                        .flatMapMany(existingNodes -> {
                            if (existingNodes.isEmpty()) {
                                log.info("No template nodes found, importing...");
                                return nodeHandler.importNodes(nodes, "DEV-01", true) // nodes est bien une List<Node>
                                        .doOnNext(node -> log.info("Added node: {}, parent: {}", node.getCode(), node.getParentCode()))
                                        .collectList()
                                        .flatMapMany(importedNodes ->
                                                userHandler.findByEmail("admin")
                                                        .flatMap(userPost -> nodeHandler.publish(environment.getId(), userPost.getId()))
                                                        .thenMany(Flux.fromIterable(importedNodes))
                                        );
                            } else {
                                log.info("Template nodes already exist, skipping import.");
                                return Flux.empty();
                            }
                        })
                );
    }

}
