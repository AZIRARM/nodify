package com.itexpert.content.api.endpoints;

import com.itexpert.content.api.handlers.PluginHandler;
import com.itexpert.content.lib.models.PluginFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Base64;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PluginEndPointTest {

    @Mock
    private PluginHandler pluginHandler;

    @InjectMocks
    private PluginEndPoint pluginEndPoint;

    private PluginFile pluginFile;
    private UUID id;
    private String pluginName;
    private String fileName;
    private String fileContent;

    @BeforeEach
    void setUp() {
        id = UUID.randomUUID();
        pluginName = "test-plugin";
        fileName = "test.js";
        fileContent = "console.log('test');";

        pluginFile = new PluginFile();
        pluginFile.setId(id);
        pluginFile.setPluginId(id);
        pluginFile.setFileName(fileName);
        pluginFile.setData(Base64.getEncoder().encodeToString(fileContent.getBytes()));
    }

    @Test
    void getContentAsFileDataShouldReturnFile() {
        when(pluginHandler.findResourceByPluginNameAndFileName(eq(pluginName), eq(fileName)))
                .thenReturn(Mono.just(pluginFile));

        StepVerifier.create(pluginEndPoint.getContentAsFileData(pluginName, fileName))
                .expectNextMatches(response -> {
                    byte[] body = response.getBody();
                    String decoded = new String(body);
                    return response.getStatusCode().is2xxSuccessful() &&
                            response.getHeaders().getContentType().toString().equals("application/javascript") &&
                            decoded.equals(fileContent);
                })
                .verifyComplete();
    }

    @Test
    void getContentAsFileDataShouldReturnEmpty() {
        when(pluginHandler.findResourceByPluginNameAndFileName(eq(pluginName), eq(fileName)))
                .thenReturn(Mono.empty());

        StepVerifier.create(pluginEndPoint.getContentAsFileData(pluginName, fileName))
                .verifyComplete();
    }

    @Test
    void getContentAsFileDataShouldReturnCssFileWithCorrectMediaType() {
        String cssFileName = "style.css";
        String cssContent = "body { color: red; }";

        PluginFile cssFile = new PluginFile();
        cssFile.setId(UUID.randomUUID());
        cssFile.setPluginId(id);
        cssFile.setFileName(cssFileName);
        cssFile.setData(Base64.getEncoder().encodeToString(cssContent.getBytes()));

        when(pluginHandler.findResourceByPluginNameAndFileName(eq(pluginName), eq(cssFileName)))
                .thenReturn(Mono.just(cssFile));

        StepVerifier.create(pluginEndPoint.getContentAsFileData(pluginName, cssFileName))
                .expectNextMatches(response -> {
                    byte[] body = response.getBody();
                    String decoded = new String(body);
                    return response.getStatusCode().is2xxSuccessful() &&
                            response.getHeaders().getContentType().toString().equals("text/css") &&
                            decoded.equals(cssContent);
                })
                .verifyComplete();
    }

    @Test
    void getContentAsFileDataShouldReturnHtmlFileWithCorrectMediaType() {
        String htmlFileName = "page.html";
        String htmlContent = "<html><body>Hello</body></html>";

        PluginFile htmlFile = new PluginFile();
        htmlFile.setId(UUID.randomUUID());
        htmlFile.setPluginId(id);
        htmlFile.setFileName(htmlFileName);
        htmlFile.setData(Base64.getEncoder().encodeToString(htmlContent.getBytes()));

        when(pluginHandler.findResourceByPluginNameAndFileName(eq(pluginName), eq(htmlFileName)))
                .thenReturn(Mono.just(htmlFile));

        StepVerifier.create(pluginEndPoint.getContentAsFileData(pluginName, htmlFileName))
                .expectNextMatches(response -> {
                    byte[] body = response.getBody();
                    String decoded = new String(body);
                    return response.getStatusCode().is2xxSuccessful() &&
                            response.getHeaders().getContentType().toString().equals("text/html") &&
                            decoded.equals(htmlContent);
                })
                .verifyComplete();
    }

    @Test
    void getContentAsFileDataShouldReturnJsonFileWithCorrectMediaType() {
        String jsonFileName = "data.json";
        String jsonContent = "{\"key\": \"value\"}";

        PluginFile jsonFile = new PluginFile();
        jsonFile.setId(UUID.randomUUID());
        jsonFile.setPluginId(id);
        jsonFile.setFileName(jsonFileName);
        jsonFile.setData(Base64.getEncoder().encodeToString(jsonContent.getBytes()));

        when(pluginHandler.findResourceByPluginNameAndFileName(eq(pluginName), eq(jsonFileName)))
                .thenReturn(Mono.just(jsonFile));

        StepVerifier.create(pluginEndPoint.getContentAsFileData(pluginName, jsonFileName))
                .expectNextMatches(response -> {
                    byte[] body = response.getBody();
                    String decoded = new String(body);
                    return response.getStatusCode().is2xxSuccessful() &&
                            response.getHeaders().getContentType().toString().equals("application/json") &&
                            decoded.equals(jsonContent);
                })
                .verifyComplete();
    }

    @Test
    void getContentAsFileDataShouldReturnOctetStreamForUnknownFileType() {
        String unknownFileName = "unknown.bin";
        String unknownContent = "binary data";

        PluginFile unknownFile = new PluginFile();
        unknownFile.setId(UUID.randomUUID());
        unknownFile.setPluginId(id);
        unknownFile.setFileName(unknownFileName);
        unknownFile.setData(Base64.getEncoder().encodeToString(unknownContent.getBytes()));

        when(pluginHandler.findResourceByPluginNameAndFileName(eq(pluginName), eq(unknownFileName)))
                .thenReturn(Mono.just(unknownFile));

        StepVerifier.create(pluginEndPoint.getContentAsFileData(pluginName, unknownFileName))
                .expectNextMatches(response -> {
                    byte[] body = response.getBody();
                    String decoded = new String(body);
                    return response.getStatusCode().is2xxSuccessful() &&
                            response.getHeaders().getContentType().toString().equals("application/octet-stream") &&
                            decoded.equals(unknownContent);
                })
                .verifyComplete();
    }

    @Test
    void getContentAsFileDataShouldHandleBase64WithSeparator() {
        String base64WithSeparator = "data:application/javascript;base64," + Base64.getEncoder().encodeToString(fileContent.getBytes());
        pluginFile.setData(base64WithSeparator);

        when(pluginHandler.findResourceByPluginNameAndFileName(eq(pluginName), eq(fileName)))
                .thenReturn(Mono.just(pluginFile));

        StepVerifier.create(pluginEndPoint.getContentAsFileData(pluginName, fileName))
                .expectNextMatches(response -> {
                    byte[] body = response.getBody();
                    String decoded = new String(body);
                    return response.getStatusCode().is2xxSuccessful() &&
                            decoded.equals(fileContent);
                })
                .verifyComplete();
    }
}