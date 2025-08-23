package com.itexpert.content.core.runners;

import com.itexpert.content.core.handlers.NodeHandler;
import com.itexpert.content.lib.enums.StatusEnum;
import com.itexpert.content.lib.models.Node;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Component
public class DefaultEnvironmentInitializer {

    private NodeHandler nodeHandler;
    private String apiUrl;

    public DefaultEnvironmentInitializer(NodeHandler nodeHandler,
                                         @Value("${app.api-url}") String apiUrl) {
        this.apiUrl = apiUrl;
        this.nodeHandler = nodeHandler;
    }

    public Mono<Void> init() {
        final String status = StatusEnum.SNAPSHOT.name();

        return nodeHandler.hasNodes()
                .flatMapMany(exists -> {
                    if (exists) {
                        log.info("DEV-01 existe déjà — aucune initialisation à faire.");
                        return Flux.empty();
                    }

                    Node dev = createNode("Development", "Development environment", "DEV-01", "development");
                    List<Node> others = List.of(
                            createNode("Integration", "Integration environment", "INT-01", "integration"),
                            createNode("Staging", "Staging environment", "STG-01", "staging"),
                            createNode("PreProduction", "Pre-Production environment", "PREP-01", "pre-production"),
                            createNode("Production", "Production environment", "PROD-01", "production")
                    );

                    return nodeHandler.save(dev)
                            .concatWith(Flux.fromIterable(others).concatMap(env -> saveIfAbsent(env, status)));
                })
                .then();
    }

    private Mono<Node> saveIfAbsent(Node env, String status) {
        return nodeHandler.findByCodeAndStatus(env.getCode(), status)
                .switchIfEmpty(nodeHandler.save(env));
    }

    private Node createNode(String name, String description, String code, String slug) {
        com.itexpert.content.lib.models.Value baseUrl = new com.itexpert.content.lib.models.Value();
        baseUrl.setValue(apiUrl);
        baseUrl.setKey("BASE_URL");

        Node node = new Node();
        node.setName(name);
        node.setDescription(description);
        node.setVersion("0");
        node.setDefaultLanguage("EN");
        node.setCode(code);
        node.setSlug(slug);
        node.setValues(List.of(baseUrl));

        if ("DEV-01".equals(code)) {
            node.setFavorite(true);
        }

        return node;
    }
}
