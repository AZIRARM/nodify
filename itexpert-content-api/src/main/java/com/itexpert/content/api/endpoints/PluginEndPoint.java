package com.itexpert.content.api.endpoints;

import com.itexpert.content.api.handlers.PluginHandler;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
@RestController
@RequestMapping(value = "/plugins")
@AllArgsConstructor
public class PluginEndPoint {

    private final PluginHandler pluginHandler;

    @GetMapping(value = "/name/{pluginName}/file/{fileName}")
    public Mono<ResponseEntity<byte[]>> getContentAsFileData(
            @PathVariable String pluginName,
            @PathVariable String fileName) {

        return pluginHandler.findResourceByPluginNameAndFileName(pluginName, fileName)
                .map(pluginFile -> {
                    byte[] bytes;
                    String data = pluginFile.getData();
                    String partSeparator = ",";

                    if (data.contains(partSeparator)) {
                        String encoded = data.split(partSeparator)[1];
                        bytes = Base64.getDecoder().decode(encoded.getBytes(StandardCharsets.UTF_8));
                    } else {
                        bytes = Base64.getDecoder().decode(data);
                    }

                    // Détection du type MIME selon l'extension
                    MediaType mediaType = guessMediaType(fileName);

                    return ResponseEntity.ok()
                            .contentLength(bytes.length)
                            .contentType(mediaType)
                            .body(bytes);
                });
    }

    // Méthode utilitaire
    private MediaType guessMediaType(String fileName) {
        if (fileName.endsWith(".css")) {
            return MediaType.valueOf("text/css");
        } else if (fileName.endsWith(".js")) {
            return MediaType.valueOf("application/javascript");
        } else if (fileName.endsWith(".json")) {
            return MediaType.APPLICATION_JSON;
        } else if (fileName.endsWith(".html")) {
            return MediaType.TEXT_HTML;
        }
        // Par défaut : binaire
        return MediaType.APPLICATION_OCTET_STREAM;
    }


}
