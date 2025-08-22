package com.itexpert.content.core.helpers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.itexpert.content.core.utils.CodesUtils;
import com.itexpert.content.lib.models.ContentNode;
import com.itexpert.content.lib.models.Node;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@AllArgsConstructor
public class RenameNodeCodesHelper {

    public Flux<Node> changeNodesCodesAndReturnFlux(List<Node> nodes, String parentCodeOrigin, Boolean fromFile) {
        // Initialisation de Gson
        Gson gson = new GsonBuilder().create();
        return Flux.fromIterable(nodes).map(node -> {
                    if (StringUtils.isEmpty(parentCodeOrigin)) {
                        if (ObjectUtils.isEmpty(node.getParentCode())) {
                            node.setParentCode(null);
                            node.setParentCodeOrigin(null);
                        }

                    }
                    // Générer un nouvel ID pour les contenus du nœud
                    if (ObjectUtils.isNotEmpty(node.getContents())) {
                        for (ContentNode contentNode : node.getContents()) {
                            contentNode.setId(UUID.randomUUID());
                        }
                    }
                    node.setId(UUID.randomUUID());

                    return node;
                }).collectList()
                .map(nodesList -> {
                    return CodesUtils.changeCodes(gson.toJson(nodesList), parentCodeOrigin, fromFile);
                })
                .map(modifiedJson -> {
                    return gson.fromJson(modifiedJson, new TypeToken<List<Node>>() {
                    }.getType());
                }).flatMapMany(o -> Flux.fromIterable((List<Node>) o)); // Convertir la liste de nœuds en Flux<Node>
    }

    public Mono<String> changeCodesAndReturnJson(List<Node> nodes, String parentCodeOrigin, Boolean fromFile) {
        Gson gson = new GsonBuilder().create();

        return Flux.fromIterable(nodes)
                .map(node -> {
                    if (ObjectUtils.isEmpty(parentCodeOrigin)) {
                        node.setId(UUID.randomUUID());
                        for (ContentNode contentNode : node.getContents()) {
                            contentNode.setId(UUID.randomUUID());
                        }
                    }
                    return node;
                })
                .collectList()
                .map(gson::toJson)
                .map(jsons -> CodesUtils.changeCodes(jsons, parentCodeOrigin, fromFile));
    }

}
