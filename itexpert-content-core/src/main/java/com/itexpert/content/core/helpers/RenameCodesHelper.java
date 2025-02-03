package com.itexpert.content.core.helpers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.itexpert.content.lib.models.ContentNode;
import com.itexpert.content.lib.models.Node;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class RenameCodesHelper {

    public Flux<Node> changeNodesCodesAndReturnFlux(List<Node> nodes, String environment) {
        // Initialisation de Gson
        Gson gson = new GsonBuilder().create();
        return Flux.fromIterable(nodes)
                .map(node -> {
                    if (StringUtils.isEmpty(environment)) {
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
                })
                .collectList() // Collecte tous les nœuds dans une liste
                .map(nodesList -> {
                    // Convertir la liste de nœuds en JSON
                    String json = gson.toJson(nodesList);
                    // Appliquer la transformation (changeIdsAndCodes) sur le JSON
                    return this.changeIdsAndCodes(json, environment);
                })
                .map(modifiedJson -> {
                    // Désérialiser le JSON modifié en une liste de nœuds
                    List list = gson.fromJson(modifiedJson, new TypeToken<List<Node>>() {
                    }.getType());

                    return list;
                })
                .flatMapMany(o -> Flux.fromIterable((List<Node>) o)); // Convertir la liste de nœuds en Flux<Node>
    }


    public Mono<String> changeCodesAndReturnJson(List<Node> nodes, String environment) {
        // Initialisation de Gson
        Gson gson = new GsonBuilder().create();

        return Flux.fromIterable(nodes).map(node -> {
                    if (ObjectUtils.isEmpty(environment)) {
                        node.setId(UUID.randomUUID());
                        for (ContentNode contentNode : node.getContents()) {
                            contentNode.setId(UUID.randomUUID());
                        }
                    }
                    return node;
                })
                .map(gson::toJson)
                .collectList()
                .map(gson::toJson)
                .map(jsons -> this.changeIdsAndCodes(jsons, environment));
    }

    private String changeIdsAndCodes(String jsons, String environment) {
        // Expression régulière pour capturer les codes de type "code": "exemple_1234"

        String contentToReturn = cleanJson(jsons);

        // final String regex = "\"code\"[ ]*:[ ]*\"[a-zA-Z-_]*[0-9]*\"";
        final String regex = "[\\\\]*\"code[\\\\]*\"[ ]*:[ ]*[\\\\]*\"[a-zA-Z-_]*[0-9]*[\\\\]*\"";

        // Création du pattern et matcher
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(contentToReturn);

        // Tant que l'on trouve une correspondance
        while (matcher.find()) {
            // Récupération du code trouvé
            String code = matcher.group();  // Cela récupère la partie après `"code": "`
            code = code.split(":")[1];

            System.out.println("----> " + code);

            // On divise le code par le "-" (si présent) et on récupère la partie après le "-"
            int lenght = code.split("-").length;

            String codeBegin = code.split("-")[0];
            String codeEnding = code.split("-")[lenght - 1];
            String newCode = null;
            if (code.split("-").length <= 1) {
                newCode = code;
            } else {
                newCode = codeBegin + '-' + environment.split("-")[0] + "-" + codeEnding;


                newCode = newCode.replaceAll("\"", "");
                newCode = newCode.replaceAll("\\\\", "");

                code = code.replaceAll("\"", "");
                code = code.replaceAll("\\\\", "");
            }

            // Remplacer
            contentToReturn = contentToReturn.replaceAll(code, newCode);


        }

        return contentToReturn;
    }


    public Mono<ContentNode> changeCodesAndReturnJson(ContentNode content, String environment) {
        // Initialisation de Gson
        Gson gson = new GsonBuilder().create();

        return Mono.just(content).map(contentNode -> {
                    if (ObjectUtils.isEmpty(environment)) {
                        contentNode.setId(UUID.randomUUID());
                    }
                    return contentNode;
                })
                .map(gson::toJson)
                .map(jsons -> this.changeIdsAndCodes(jsons, environment))
                .map(json -> {
                    // Désérialiser le JSON modifié en une liste de nœuds
                    ContentNode model = gson.fromJson(json, new TypeToken<ContentNode>() {
                    }.getType());
                    return model;
                });
    }

    public String cleanJson(String json) {
        StringBuilder result = new StringBuilder(json.length());
        boolean inQuotes = false;
        boolean escapeMode = false;
        for (char character : json.toCharArray()) {
            if (escapeMode) {
                result.append(character);
                escapeMode = false;
            } else if (character == '"') {
                inQuotes = !inQuotes;
                result.append(character);
            } else if (character == '\\') {
                escapeMode = true;
                result.append(character);
            } else if (!inQuotes && character == ' ') {
                continue;
            } else {
                result.append(character);
            }
        }
        return result.toString();
    }
}
