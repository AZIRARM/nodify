package com.itexpert.content.api.helpers;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itexpert.content.api.handlers.ContentDisplayHandler;
import com.itexpert.content.api.mappers.ContentNodeMapper;
import com.itexpert.content.api.repositories.ContentNodeRepository;
import com.itexpert.content.api.repositories.NodeRepository;
import com.itexpert.content.api.repositories.PluginRepository;
import com.itexpert.content.api.utils.RulesUtils;
import com.itexpert.content.lib.entities.ContentNode;
import com.itexpert.content.lib.entities.Node;
import com.itexpert.content.lib.enums.StatusEnum;
import com.itexpert.content.lib.models.ContentDisplay;
import com.itexpert.content.lib.models.Translation;
import com.itexpert.content.lib.models.Value;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ContentHelperTest {

    @Mock
    private ContentNodeMapper contentNodeMapper;
    @Mock
    private ContentDisplayHandler contentDisplayHandler;
    @Mock
    private ContentNodeRepository contentNodeRepository;
    @Mock
    private NodeRepository nodeRepository;
    @Mock
    private PluginRepository pluginRepository;
    @Mock
    private PluginHelper pluginHelper;

    @InjectMocks
    private ContentHelper contentHelper;

    private ContentNode mockEntity;
    private com.itexpert.content.lib.models.ContentNode mockModel;
    private MockedStatic<RulesUtils> rulesUtilsMockedStatic;

    @BeforeEach
    void setUp() {
        mockEntity = new ContentNode();
        mockEntity.setCode("CONTENT-1");
        mockEntity.setContent("A simple text content");

        mockModel = new com.itexpert.content.lib.models.ContentNode();
        mockModel.setCode("CONTENT-1");

        rulesUtilsMockedStatic = Mockito.mockStatic(RulesUtils.class);
    }

    @AfterEach
    void tearDown() {
        rulesUtilsMockedStatic.close();
    }

    @Test
    void testFindByCodeAndStatus_Success() {
        when(contentNodeRepository.findByCodeAndStatus("CONTENT-1", StatusEnum.PUBLISHED.name()))
                .thenReturn(Mono.just(mockEntity));
        when(contentNodeMapper.fromEntity(mockEntity)).thenReturn(mockModel);

        rulesUtilsMockedStatic.when(() -> RulesUtils.evaluateContentNode(mockModel))
                .thenReturn(Mono.just(true));

        StepVerifier.create(contentHelper.findByCodeAndStatus("CONTENT-1", StatusEnum.PUBLISHED))
                .expectNext(mockModel)
                .verifyComplete();
    }

    @Test
    void testFindByCodeAndStatus_FailsEvaluation() {
        when(contentNodeRepository.findByCodeAndStatus("CONTENT-1", StatusEnum.PUBLISHED.name()))
                .thenReturn(Mono.just(mockEntity));
        when(contentNodeMapper.fromEntity(mockEntity)).thenReturn(mockModel);

        rulesUtilsMockedStatic.when(() -> RulesUtils.evaluateContentNode(mockModel))
                .thenReturn(Mono.just(false));

        StepVerifier.create(contentHelper.findByCodeAndStatus("CONTENT-1", StatusEnum.PUBLISHED))
                .verifyComplete();
    }

    @Test
    void testFillContents_SimpleContent() {
        // When content has no codes
        when(pluginHelper.fillPlugin(mockEntity)).thenReturn(Mono.just(mockEntity));
        when(contentNodeMapper.fromEntity(mockEntity)).thenReturn(mockModel);

        rulesUtilsMockedStatic.when(() -> RulesUtils.evaluateContentNode(mockModel))
                .thenReturn(Mono.just(true));

        StepVerifier.create(contentHelper.fillContents(mockEntity, StatusEnum.PUBLISHED, "fr"))
                .expectNext(mockEntity)
                .verifyComplete();
    }

    @Test
    void testFillContents_PythonCodeInjectedAsEscapedString() {
        String pythonCode = "def hello():\n    print(\"hi\")\n";

        ContentNode parent = new ContentNode();
        parent.setCode("PARENT");
        parent.setContent("{\"python\": $content(PY-CHILD)}");

        ContentNode child = new ContentNode();
        child.setCode("PY-CHILD");
        child.setContent(pythonCode);

        mockCommonFinalization();
        mockChildResolution("PY-CHILD", child);

        StepVerifier.create(contentHelper.fillContents(parent, StatusEnum.PUBLISHED, "fr"))
                .assertNext(result -> {
                    assertFalse(result.getContent().contains("$content("));
                    assertDoesNotThrow(() -> {
                        JsonNode root = new ObjectMapper().readTree(result.getContent());
                        assertEquals(pythonCode, root.get("python").asText());
                    });
                })
                .verifyComplete();
    }

    @Test
    void testFillContents_JavaScriptCodeInjectedAsEscapedString() {
        String jsCode = "function greet(name) {\n    return \"Hello \" + name;\n}\n";

        ContentNode parent = new ContentNode();
        parent.setCode("PARENT");
        parent.setContent("{\"js\": $content(JS-CHILD)}");

        ContentNode child = new ContentNode();
        child.setCode("JS-CHILD");
        child.setContent(jsCode);

        mockCommonFinalization();
        mockChildResolution("JS-CHILD", child);

        StepVerifier.create(contentHelper.fillContents(parent, StatusEnum.PUBLISHED, "fr"))
                .assertNext(result -> assertDoesNotThrow(() -> {
                    JsonNode root = new ObjectMapper().readTree(result.getContent());
                    assertEquals(jsCode, root.get("js").asText());
                }))
                .verifyComplete();
    }

    @Test
    void testFillContents_HtmlCodeInjectedAsEscapedString() {
        String htmlCode = "<div class=\"container\">\n  <p>Hello</p>\n</div>";

        ContentNode parent = new ContentNode();
        parent.setCode("PARENT");
        parent.setContent("{\"html\": $content(HTML-CHILD)}");

        ContentNode child = new ContentNode();
        child.setCode("HTML-CHILD");
        child.setContent(htmlCode);

        mockCommonFinalization();
        mockChildResolution("HTML-CHILD", child);

        StepVerifier.create(contentHelper.fillContents(parent, StatusEnum.PUBLISHED, "fr"))
                .assertNext(result -> assertDoesNotThrow(() -> {
                    JsonNode root = new ObjectMapper().readTree(result.getContent());
                    assertEquals(htmlCode, root.get("html").asText());
                }))
                .verifyComplete();
    }

    @Test
    void testFillContents_SqlCodeInjectedAsEscapedString() {
        String sql = "SELECT * FROM users\nWHERE name = 'John' AND age > 18;";

        ContentNode parent = new ContentNode();
        parent.setCode("PARENT");
        parent.setContent("{\"query\": $content(SQL-CHILD)}");

        ContentNode child = new ContentNode();
        child.setCode("SQL-CHILD");
        child.setContent(sql);

        mockCommonFinalization();
        mockChildResolution("SQL-CHILD", child);

        StepVerifier.create(contentHelper.fillContents(parent, StatusEnum.PUBLISHED, "fr"))
                .assertNext(result -> assertDoesNotThrow(() -> {
                    JsonNode root = new ObjectMapper().readTree(result.getContent());
                    assertEquals(sql, root.get("query").asText());
                }))
                .verifyComplete();
    }

    @Test
    void testFillContents_JsonObjectChildInjectedAsObject() {
        ContentNode parent = new ContentNode();
        parent.setCode("PARENT");
        parent.setContent("{\"steps\": [$content(STEP-1)]}");

        ContentNode child = new ContentNode();
        child.setCode("STEP-1");
        child.setContent("{\"name\":\"fetch\",\"script\":\"print(1)\"}");

        mockCommonFinalization();
        mockChildResolution("STEP-1", child);

        StepVerifier.create(contentHelper.fillContents(parent, StatusEnum.PUBLISHED, "fr"))
                .assertNext(result -> assertDoesNotThrow(() -> {
                    JsonNode root = new ObjectMapper().readTree(result.getContent());
                    assertTrue(root.get("steps").isArray());
                    assertEquals(1, root.get("steps").size());
                    assertTrue(root.get("steps").get(0).isObject());
                    assertEquals("fetch", root.get("steps").get(0).get("name").asText());
                }))
                .verifyComplete();
    }

    @Test
    void testFillContents_JsonArrayChildInjectedAsArray() {
        ContentNode parent = new ContentNode();
        parent.setCode("PARENT");
        parent.setContent("{\"items\": $content(LIST)}");

        ContentNode child = new ContentNode();
        child.setCode("LIST");
        child.setContent("[1,2,3]");

        mockCommonFinalization();
        mockChildResolution("LIST", child);

        StepVerifier.create(contentHelper.fillContents(parent, StatusEnum.PUBLISHED, "fr"))
                .assertNext(result -> assertDoesNotThrow(() -> {
                    JsonNode root = new ObjectMapper().readTree(result.getContent());
                    assertTrue(root.get("items").isArray());
                    assertEquals(3, root.get("items").size());
                    assertEquals(1, root.get("items").get(0).asInt());
                }))
                .verifyComplete();
    }

    @Test
    void testFillContents_JsonInJsonTwoLevels() {
        ContentNode parent = new ContentNode();
        parent.setCode("PARENT");
        parent.setContent("{\"nested\": $content(LEVEL-1)}");

        ContentNode level1 = new ContentNode();
        level1.setCode("LEVEL-1");
        level1.setContent("{\"inner\": $content(LEVEL-2)}");

        ContentNode level2 = new ContentNode();
        level2.setCode("LEVEL-2");
        level2.setContent("{\"value\":42}");

        mockCommonFinalization();
        mockChildResolution("LEVEL-1", level1);
        mockChildResolution("LEVEL-2", level2);

        StepVerifier.create(contentHelper.fillContents(parent, StatusEnum.PUBLISHED, "fr"))
                .assertNext(result -> assertDoesNotThrow(() -> {
                    JsonNode root = new ObjectMapper().readTree(result.getContent());
                    assertTrue(root.get("nested").isObject());
                    assertTrue(root.get("nested").get("inner").isObject());
                    assertEquals(42, root.get("nested").get("inner").get("value").asInt());
                }))
                .verifyComplete();
    }

    @Test
    void testFillContents_JsonInJsonThreeLevels() {
        ContentNode parent = new ContentNode();
        parent.setCode("PARENT");
        parent.setContent("{\"l1\": $content(L1)}");

        ContentNode l1 = new ContentNode();
        l1.setCode("L1");
        l1.setContent("{\"l2\": $content(L2)}");

        ContentNode l2 = new ContentNode();
        l2.setCode("L2");
        l2.setContent("{\"l3\": $content(L3)}");

        ContentNode l3 = new ContentNode();
        l3.setCode("L3");
        l3.setContent("{\"value\":\"deep\"}");

        mockCommonFinalization();
        mockChildResolution("L1", l1);
        mockChildResolution("L2", l2);
        mockChildResolution("L3", l3);

        StepVerifier.create(contentHelper.fillContents(parent, StatusEnum.PUBLISHED, "fr"))
                .assertNext(result -> assertDoesNotThrow(() -> {
                    JsonNode root = new ObjectMapper().readTree(result.getContent());
                    assertEquals("deep", root.get("l1").get("l2").get("l3").get("value").asText());
                }))
                .verifyComplete();
    }

    @Test
    void testFillContents_ValueWithSpecialCharsEscaped() {
        String originalValue = "abc\"def\\ghi\njkl";

        ContentNode element = new ContentNode();
        element.setCode("VALUE-TEST");
        element.setContent("{\"password\":\"$value(PASSWORD)\"}");
        element.setParentCode("PARENT-NODE");

        Value value = new Value();
        value.setKey("PASSWORD");
        value.setValue(originalValue);
        element.setValues(List.of(value));

        Node parentNode = new Node();
        parentNode.setCode("PARENT-NODE");
        parentNode.setDefaultLanguage("fr");

        when(nodeRepository.findByCodeAndStatus("PARENT-NODE", StatusEnum.PUBLISHED.name()))
                .thenReturn(Mono.just(parentNode));
        mockCommonFinalization();

        StepVerifier.create(contentHelper.fillContents(element, StatusEnum.PUBLISHED, "fr"))
                .assertNext(result -> {
                    assertFalse(result.getContent().contains("$value(PASSWORD)"));
                    assertDoesNotThrow(() -> {
                        JsonNode root = new ObjectMapper().readTree(result.getContent());
                        assertEquals(originalValue, root.get("password").asText());
                    });
                })
                .verifyComplete();
    }

    @Test
    void testFillContents_TranslateWithSpecialCharsEscaped() {
        String translationValue = "Bonjour \"monde\"\\n";

        ContentNode element = new ContentNode();
        element.setCode("TRANSLATE-TEST");
        element.setContent("{\"msg\":\"$translate(GREETING)\"}");
        element.setParentCode("PARENT-NODE");

        Translation translation = new Translation();
        translation.setLanguage("fr");
        translation.setKey("GREETING");
        translation.setValue(translationValue);
        element.setTranslations(List.of(translation));

        Node parentNode = new Node();
        parentNode.setCode("PARENT-NODE");
        parentNode.setDefaultLanguage("fr");

        when(nodeRepository.findByCodeAndStatus("PARENT-NODE", StatusEnum.PUBLISHED.name()))
                .thenReturn(Mono.just(parentNode));
        mockCommonFinalization();

        StepVerifier.create(contentHelper.fillContents(element, StatusEnum.PUBLISHED, "fr"))
                .assertNext(result -> {
                    assertFalse(result.getContent().contains("$translate(GREETING)"));
                    assertDoesNotThrow(() -> {
                        JsonNode root = new ObjectMapper().readTree(result.getContent());
                        assertEquals(translationValue, root.get("msg").asText());
                    });
                })
                .verifyComplete();
    }

    private void mockCommonFinalization() {
        when(pluginHelper.fillPlugin(any())).thenReturn(Mono.just(mockEntity));
        when(contentNodeMapper.fromEntity(any())).thenReturn(mockModel);
        rulesUtilsMockedStatic.when(() -> RulesUtils.evaluateContentNode(any()))
                .thenReturn(Mono.just(true));
    }

    private void mockChildResolution(String childCode, ContentNode child) {
        when(contentNodeRepository.findByCodeAndStatus(childCode, StatusEnum.PUBLISHED.name()))
                .thenReturn(Mono.just(child));
        when(contentDisplayHandler.findByContentCode(anyString()))
                .thenReturn(Mono.empty());
        when(contentDisplayHandler.addDisplay(anyString()))
                .thenReturn(Mono.just(true));
    }

    @Test
    void testFillContents_ValueResolvedWithoutParentNode() {
        ContentNode element = new ContentNode();
        element.setCode("NO-PARENT-VALUE");
        element.setContent("{\"provider\":\"$value(provider)\"}");
        // Pas de parentCode → parentNode sera null

        Value value = new Value();
        value.setKey("provider");
        value.setValue("ollama");
        element.setValues(List.of(value));

        mockCommonFinalization();

        StepVerifier.create(contentHelper.fillContents(element, StatusEnum.PUBLISHED, "fr"))
                .assertNext(result -> {
                    assertFalse(result.getContent().contains("$value(provider)"),
                            "Le $value() doit être résolu même sans parent Node");
                    assertDoesNotThrow(() -> {
                        JsonNode root = new ObjectMapper().readTree(result.getContent());
                        assertEquals("ollama", root.get("provider").asText());
                    });
                })
                .verifyComplete();
    }

    @Test
    void testFillContents_TranslateResolvedWithoutParentNode() {
        ContentNode element = new ContentNode();
        element.setCode("NO-PARENT-TRANSLATE");
        element.setContent("{\"msg\":\"$translate(HELLO)\"}");

        Translation fr = new Translation();
        fr.setLanguage("fr");
        fr.setKey("HELLO");
        fr.setValue("Bonjour");
        element.setTranslations(List.of(fr));

        mockCommonFinalization();

        StepVerifier.create(contentHelper.fillContents(element, StatusEnum.PUBLISHED, "fr"))
                .assertNext(result -> {
                    assertFalse(result.getContent().contains("$translate(HELLO)"),
                            "Le $translate() doit être résolu même sans parent Node");
                    assertDoesNotThrow(() -> {
                        JsonNode root = new ObjectMapper().readTree(result.getContent());
                        assertEquals("Bonjour", root.get("msg").asText());
                    });
                })
                .verifyComplete();
    }

    @Test
    void testFillContents_TranslateFallsBackToEnglishWhenNoParentAndNoExplicitLang() {
        ContentNode element = new ContentNode();
        element.setCode("EN-FALLBACK");
        element.setContent("{\"msg\":\"$translate(HELLO)\"}");

        Translation en = new Translation();
        en.setLanguage("en");
        en.setKey("HELLO");
        en.setValue("Hello");

        Translation fr = new Translation();
        fr.setLanguage("fr");
        fr.setKey("HELLO");
        fr.setValue("Bonjour");

        element.setTranslations(List.of(en, fr));

        mockCommonFinalization();

        StepVerifier.create(contentHelper.fillContents(element, StatusEnum.PUBLISHED, null))
                .assertNext(result -> assertDoesNotThrow(() -> {
                    JsonNode root = new ObjectMapper().readTree(result.getContent());
                    assertEquals("Hello", root.get("msg").asText(),
                            "Doit tomber sur la traduction 'en' par défaut (pas 'fr')");
                }))
                .verifyComplete();
    }


    @Test
    void testFillContents_NullValueDoesNotThrowAndOtherValuesResolved() {
        ContentNode element = new ContentNode();
        element.setCode("NULL-VALUE");
        element.setContent("{\"a\":\"$value(A)\",\"b\":\"$value(B)\"}");

        Value a = new Value();
        a.setKey("A");
        a.setValue("alpha");

        Value b = new Value();
        b.setKey("B");
        b.setValue(null);   // <-- déclencheur de la NPE avant le correctif

        element.setValues(List.of(a, b));

        mockCommonFinalization();

        StepVerifier.create(contentHelper.fillContents(element, StatusEnum.PUBLISHED, "fr"))
                .assertNext(result -> {
                    // A doit être résolu
                    assertFalse(result.getContent().contains("$value(A)"),
                            "Le Value 'A' valide doit être résolu malgré le null de 'B'");
                    // B reste en placeholder (on ne remplace pas par null)
                    assertTrue(result.getContent().contains("$value(B)"),
                            "Le placeholder du Value null reste en place, on ne remplace pas par null");
                    // Et surtout : le contenu reste un JSON valide (pas de NPE)
                    assertDoesNotThrow(() -> new ObjectMapper().readTree(result.getContent()));
                })
                .verifyComplete();
    }

    @Test
    void testFillContents_NullTranslationValueDoesNotThrowAndOtherTranslationsResolved() {
        ContentNode element = new ContentNode();
        element.setCode("NULL-TRANSLATION");
        element.setContent("{\"a\":\"$translate(A)\",\"b\":\"$translate(B)\"}");

        Translation a = new Translation();
        a.setLanguage("fr");
        a.setKey("A");
        a.setValue("alpha");

        Translation b = new Translation();
        b.setLanguage("fr");
        b.setKey("B");
        b.setValue(null);

        element.setTranslations(List.of(a, b));

        mockCommonFinalization();

        StepVerifier.create(contentHelper.fillContents(element, StatusEnum.PUBLISHED, "fr"))
                .assertNext(result -> {
                    assertFalse(result.getContent().contains("$translate(A)"));
                    assertTrue(result.getContent().contains("$translate(B)"));
                    assertDoesNotThrow(() -> new ObjectMapper().readTree(result.getContent()));
                })
                .verifyComplete();
    }

    @Test
    void testFillContents_NullValueKeyDoesNotThrow() {
        ContentNode element = new ContentNode();
        element.setCode("NULL-KEY");
        element.setContent("{\"a\":\"$value(A)\"}");

        Value a = new Value();
        a.setKey("A");
        a.setValue("alpha");

        Value nullKey = new Value();
        nullKey.setKey(null);
        nullKey.setValue("whatever");

        element.setValues(List.of(a, nullKey));

        mockCommonFinalization();

        StepVerifier.create(contentHelper.fillContents(element, StatusEnum.PUBLISHED, "fr"))
                .assertNext(result -> {
                    assertFalse(result.getContent().contains("$value(A)"));
                    assertDoesNotThrow(() -> new ObjectMapper().readTree(result.getContent()));
                })
                .verifyComplete();
    }

    @Test
    void testFillContents_ValueAndTranslateBothResolvedWithoutParent() {
        ContentNode element = new ContentNode();
        element.setCode("BOTH-NO-PARENT");
        element.setContent("{\"provider\":\"$value(provider)\",\"msg\":\"$translate(HELLO)\"}");

        Value value = new Value();
        value.setKey("provider");
        value.setValue("gemini");
        element.setValues(List.of(value));

        Translation tr = new Translation();
        tr.setLanguage("fr");
        tr.setKey("HELLO");
        tr.setValue("Salut");
        element.setTranslations(List.of(tr));

        mockCommonFinalization();

        StepVerifier.create(contentHelper.fillContents(element, StatusEnum.PUBLISHED, "fr"))
                .assertNext(result -> {
                    assertFalse(result.getContent().contains("$value(provider)"));
                    assertFalse(result.getContent().contains("$translate(HELLO)"));
                    assertDoesNotThrow(() -> {
                        JsonNode root = new ObjectMapper().readTree(result.getContent());
                        assertEquals("gemini", root.get("provider").asText());
                        assertEquals("Salut", root.get("msg").asText());
                    });
                })
                .verifyComplete();
    }

    @Test
    void testFillContents_NullValueOnChildDoesNotAbortParentResolution() {
        ContentNode parent = new ContentNode();
        parent.setCode("PARENT-NULL-CHILD");
        parent.setContent("{\"v\":\"$value(V)\",\"child\":$content(CHILD)}");

        Value parentValue = new Value();
        parentValue.setKey("V");
        parentValue.setValue("parent-ok");
        parent.setValues(List.of(parentValue));

        ContentNode child = new ContentNode();
        child.setCode("CHILD");
        child.setContent("{\"x\":\"$value(X)\"}");

        Value childValue = new Value();
        childValue.setKey("X");
        childValue.setValue(null);
        child.setValues(List.of(childValue));

        mockCommonFinalization();
        mockChildResolution("CHILD", child);

        StepVerifier.create(contentHelper.fillContents(parent, StatusEnum.PUBLISHED, "fr"))
                .assertNext(result -> {
                    // Le parent ne doit PAS être renvoyé brut
                    assertFalse(result.getContent().contains("$value(V)"),
                            "La valeur du parent doit être résolue malgré le null chez l'enfant");
                    assertFalse(result.getContent().contains("$content(CHILD)"),
                            "Le $content() doit avoir été substitué");
                    assertDoesNotThrow(() -> {
                        JsonNode root = new ObjectMapper().readTree(result.getContent());
                        assertEquals("parent-ok", root.get("v").asText());
                        assertTrue(root.get("child").has("x"));
                        // Le placeholder du child reste en place mais le JSON est valide
                        assertEquals("$value(X)", root.get("child").get("x").asText());
                    });
                })
                .verifyComplete();
    }

    @Test
    void testFillContents_OnErrorReturnsRawElement() {
        ContentNode element = new ContentNode();
        element.setCode("ERROR-CASE");
        element.setContent("plain text");
        String originalContent = element.getContent();

        when(pluginHelper.fillPlugin(any()))
                .thenReturn(Mono.error(new RuntimeException("boom")));

        StepVerifier.create(contentHelper.fillContents(element, StatusEnum.PUBLISHED, "fr"))
                .assertNext(result -> assertEquals(originalContent, result.getContent(),
                        "En cas d'erreur, l'élément brut est renvoyé (onErrorReturn)"))
                .verifyComplete();
    }
}
