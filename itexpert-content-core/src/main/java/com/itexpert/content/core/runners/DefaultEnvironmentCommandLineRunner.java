package com.itexpert.content.core.runners;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.itexpert.content.core.handlers.NodeHandler;
import com.itexpert.content.core.handlers.UserHandler;
import com.itexpert.content.lib.enums.StatusEnum;
import com.itexpert.content.lib.models.Node;
import lombok.extern.slf4j.Slf4j;
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
        final String status = StatusEnum.SNAPSHOT.name();

        nodeHandler.hasNodes()
                .flatMapMany(exists -> {
                    if (exists) {
                        log.info("DEV-01 existe déjà — aucune initialisation à faire.");
                        return Flux.empty();
                    }

                    // A partir d’ici, DEV-01 n’existe pas : on initialise tout
                    Node dev = createNode("Development", "Development environment", "DEV-01", "development");
                    List<Node> others = List.of(
                            createNode("Integration", "Integration environment", "INT-01", "integration"),
                            createNode("Staging", "Staging environment", "STG-01", "staging"),
                            createNode("PreProduction", "Pre-Production environment", "PREP-01", "pre-production"),
                            createNode("Production", "Production environment", "PROD-01", "production")
                    );

                    // 1) créer DEV-01, 2) importer ses templates, 3) créer les autres environnements (si absents)
                    return nodeHandler.save(dev)
                            .flatMapMany(savedDev ->
                                    importDevEnvironment(savedDev)
                                            .thenMany(Flux.fromIterable(others)
                                                    .concatMap(env -> saveIfAbsent(env, status)))
                            );
                })
                .subscribe(
                        n -> log.info("Initialisé/importé : {}", n.getCode()),
                        e -> log.error("Erreur lors de l'initialisation des environnements", e),
                        () -> log.info("Initialisation terminée")
                );
    }

    /**
     * Sauvegarde l’environnement uniquement s’il n’existe pas déjà (idempotent).
     */
    private Mono<Node> saveIfAbsent(Node env, String status) {
        return nodeHandler.findByCodeAndStatus(env.getCode(), status)
                .switchIfEmpty(nodeHandler.save(env));
    }


    private Flux<Node> importDevEnvironment(Node devEnv) {
        return userHandler.findByEmail("admin")
                .flatMapMany(user -> Flux.fromIterable(List.of(
                                        "templates/Nodify-Blog.json",
                                        "templates/Nodify-Landingpage.json"
                                ))
                                .flatMap(template -> importTemplateChildrenOnly(devEnv, template))
                                // Quand toutes les imports sont finis, on fait le publish une seule fois
                                .then(Mono.defer(() -> nodeHandler.publish(devEnv.getId(), user.getId())
                                        .thenReturn(devEnv)))
                                .flux()
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
