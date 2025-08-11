package com.itexpert.content.core.helpers;

import com.google.gson.Gson;
import com.itexpert.content.lib.models.ContentNode;
import com.itexpert.content.lib.models.Node;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

class RenameNodeCodesHelperTest {

    private RenameNodeCodesHelper helper;

    @BeforeEach
    void setUp() {
        helper = new RenameNodeCodesHelper();
    }

    @Test
    void changeNodesCodesAndReturnFlux_shouldGenerateIdsAndModifyJson() {
        List<Node> nodes = new ArrayList<>();
        Node node1 = new Node();
        node1.setParentCode(null);
        ContentNode content1 = new ContentNode();
        content1.setId(null);
        content1.setContent("original content 1");
        node1.setContents(List.of(content1));
        nodes.add(node1);

        Node node2 = new Node();
        node2.setParentCode("parentCode");
        ContentNode content2 = new ContentNode();
        content2.setId(null);
        content2.setContent("original content 2");
        node2.setContents(List.of(content2));
        nodes.add(node2);

        try (MockedStatic<com.itexpert.content.core.utils.CodesUtils> codesUtilsMock = Mockito.mockStatic(com.itexpert.content.core.utils.CodesUtils.class)) {
            codesUtilsMock.when(() -> com.itexpert.content.core.utils.CodesUtils.changeCodes(anyString(), eq(""), eq(false)))
                    .thenAnswer(invocation -> {
                        String jsonInput = invocation.getArgument(0);
                        // Remplacer une partie du JSON par une chaîne fixe
                        return jsonInput.replace("original content", "changed content");
                    });

            StepVerifier.create(helper.changeNodesCodesAndReturnFlux(nodes, "", false))
                    .recordWith(ArrayList::new)
                    .expectNextCount(2)
                    .consumeRecordedWith(list -> {
                        for (Node n : list) {
                            assertNotNull(n.getId(), "Node id should be generated");
                            if (n.getContents() != null) {
                                for (ContentNode c : n.getContents()) {
                                    assertNotNull(c.getId(), "ContentNode id should be generated");
                                    assertTrue(c.getContent().contains("changed content"));
                                }
                            }
                            if (n.getParentCode() == null) {
                                assertNull(n.getParentCodeOrigin());
                            }
                        }
                    })
                    .verifyComplete();

            codesUtilsMock.verify(() -> com.itexpert.content.core.utils.CodesUtils.changeCodes(anyString(), eq(""), eq(false)));
        }
    }

    @Test
    void changeCodesAndReturnJson_shouldGenerateIdsAndReturnModifiedJson() {
        List<Node> nodes = new ArrayList<>();
        Node node = new Node();
        ContentNode content = new ContentNode();
        content.setId(null);
        content.setContent("original content");
        node.setContents(List.of(content));
        nodes.add(node);

        try (MockedStatic<com.itexpert.content.core.utils.CodesUtils> codesUtilsMock = Mockito.mockStatic(com.itexpert.content.core.utils.CodesUtils.class)) {
            codesUtilsMock.when(() -> com.itexpert.content.core.utils.CodesUtils.changeCodes(anyString(), eq(null), eq(false)))
                    .thenAnswer(invocation -> {
                        String jsonInput = invocation.getArgument(0);
                        return jsonInput.replace("original content", "changed content");
                    });

            StepVerifier.create(helper.changeCodesAndReturnJson(nodes, null, false))
                    .assertNext(json -> {
                        assertTrue(json.contains("changed content"));
                        // On peut aussi vérifier que l'id a été généré dans le JSON, par exemple avec Gson:
                        Gson gson = new Gson();
                        Node[] parsedNodes = gson.fromJson(json, Node[].class);
                        for (Node n : parsedNodes) {
                            assertNotNull(n.getId());
                            for (ContentNode c : n.getContents()) {
                                assertNotNull(c.getId());
                            }
                        }
                    })
                    .verifyComplete();

            codesUtilsMock.verify(() -> com.itexpert.content.core.utils.CodesUtils.changeCodes(anyString(), eq(null), eq(false)));
        }
    }
}

