package com.itexpert.content.core.runners;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.itexpert.content.core.handlers.NodeHandler;
import com.itexpert.content.core.handlers.UserHandler;
import com.itexpert.content.lib.enums.StatusEnum;
import com.itexpert.content.lib.models.Node;
import com.itexpert.content.lib.models.Value;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.List;

@Slf4j
@Component
@AllArgsConstructor
public class DefaultEnvironmentCommandLineRunner implements CommandLineRunner {

    private final NodeHandler nodeHandler;
    private final UserHandler userHandler;

    public void run(String... args) {
        this.start();
    }

    private void start() {
        Value baseUrl = new Value();
        baseUrl.setKey("BASE_URL");
        baseUrl.setValue("/api");

        Value baseUrlDev = new Value();
        baseUrlDev.setKey("BASE_URL");
        baseUrlDev.setValue("/v0");

        List<Node> environments = List.of(
                createNode("Development", "Development environment", "DEV-01", baseUrlDev),
                createNode("Integration", "Integration environment", "INT-02", baseUrl),
                createNode("Staging", "Staging environment", "INT-03", baseUrl),
                createNode("PreProduction", "Pre-Production environment", "PREP-04", baseUrl),
                createNode("Production", "Production environment", "PROD-05", baseUrl)
        );

        nodeHandler.findAll()
                .collectList()
                .flatMap(existingNodes -> {
                    if (existingNodes.isEmpty()) {
                        log.info("No environments found, creating defaults...");
                        return nodeHandler.saveAll(environments)
                                .doOnNext(node -> log.info("Node saved, code: {}", node.getCode()))
                                .then(Mono.just(environments));
                    } else {
                        log.info("Environments already exist, skipping creation.");
                        return Mono.empty();
                    }
                })
                .flatMapMany(nodes -> Flux.fromIterable(nodes)
                        .flatMap(node -> userHandler.findByEmail("admin")
                                .flatMap(userPost -> nodeHandler.publish(node.getId(), userPost.getId()))
                                .doOnNext(savedNode -> log.info("Published node: {}", savedNode.getCode()))
                        )
                )
                .filter(node -> node.getCode().equals("DEV-01"))
                .flatMap(node ->
                        Flux.merge(
                                this.importTemplate(node, "templates/Nodify-Blog.json"),
                                this.importTemplate(node, "templates/Nodify-Landingpage.json")
                        )
                )
                .subscribe(
                        importedNode -> log.info("Imported node: {}", importedNode.getCode()),
                        error -> log.error("Error initializing environments", error),
                        () -> log.info("Defaults environments initialized successfully")
                );


    }

    private Node createNode(String name, String description, String code, Value baseUrl) {
        Node node = new Node();
        node.setName(name);
        node.setDescription(description);
        node.setVersion("1");
        node.setDefaultLanguage("EN");
        node.setCode(code);
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
