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
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
public class DefaultEnvironmentCommandLineRunner implements CommandLineRunner {

    @Value("${app.api-url}")
    private String apiUrl;

    private final NodeHandler nodeHandler;
    private final UserHandler userHandler;

    public DefaultEnvironmentCommandLineRunner(NodeHandler nodeHandler, UserHandler userHandler) {
        this.nodeHandler = nodeHandler;
        this.userHandler = userHandler;
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

    private final AtomicBoolean importInProgress = new AtomicBoolean(false);

    private Flux<Node> importTemplate(Node environment, String template) {
        if (!importInProgress.compareAndSet(false, true)) {
            log.info("Import déjà en cours, skipping.");
            return Flux.empty();
        }

        return realImportTemplate(environment, template)
                .doFinally(signal -> importInProgress.set(false));
    }

    private Flux<Node> realImportTemplate(Node environment, String template) {
        return Mono.fromCallable(() -> {
                    ClassPathResource resource = new ClassPathResource(template);
                    try (InputStreamReader reader = new InputStreamReader(resource.getInputStream())) {
                        Type listType = new TypeToken<List<Node>>() {}.getType();
                        // Désérialisation explicite avec cast
                        List<Node> nodes = new Gson().fromJson(reader, listType);
                        return nodes;
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                })
                .flatMapMany(nodes ->
                        nodeHandler.findChildrenByCodeAndStatus("DEV-01", StatusEnum.SNAPSHOT.name())
                                // ... suite du code ...
                                .collectList()
                                .flatMapMany(existingNodes -> {
                                    if (existingNodes.isEmpty()) {
                                        log.info("No template nodes found, importing...");
                                        return nodeHandler.importNodes(nodes, "DEV-01", true)
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
