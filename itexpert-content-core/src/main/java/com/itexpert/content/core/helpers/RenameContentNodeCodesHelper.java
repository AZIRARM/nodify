package com.itexpert.content.core.helpers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.itexpert.content.core.utils.CodesUtils;
import com.itexpert.content.lib.models.ContentNode;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@Component
public class RenameContentNodeCodesHelper {
    public Mono<ContentNode> changeCodesAndReturnJson(ContentNode content, String environment, Boolean fromFile) {
        // Initialisation de Gson
        Gson gson = new GsonBuilder().create();

        return Mono.just(content).map(contentNode -> {
            if (ObjectUtils.isEmpty(environment)) {
                contentNode.setId(UUID.randomUUID());
            }
            return contentNode;
        }).map(gson::toJson).map(jsons -> CodesUtils.changeCodes(jsons, environment, fromFile)).map(json -> {
            ContentNode model = gson.fromJson(json, new TypeToken<ContentNode>() {
            }.getType());
            return model;
        });
    }

}
