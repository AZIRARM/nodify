package com.itexpert.content.core.helpers;

import com.itexpert.content.lib.models.ContentNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;

class RenameContentNodeCodesHelperTest {

    private RenameContentNodeCodesHelper helper;

    @BeforeEach
    void setUp() {
        helper = new RenameContentNodeCodesHelper();
    }

    @Test
    void changeCodesAndReturnJson_shouldGenerateIdIfEnvironmentEmptyAndProcessJson() {
        ContentNode input = new ContentNode();
        input.setId(null);
        input.setContent("original content");

        try (MockedStatic<com.itexpert.content.core.utils.CodesUtils> codesUtilsMock = Mockito.mockStatic(com.itexpert.content.core.utils.CodesUtils.class)) {
            // Simuler CodesUtils.changeCodes pour retourner une JSON modifiée
            codesUtilsMock.when(() -> com.itexpert.content.core.utils.CodesUtils.changeCodes(anyString(), eq(""), eq(false)))
                    .thenAnswer(invocation -> {
                        String jsonInput = invocation.getArgument(0);
                        // Remplacer une partie du JSON par une chaîne fixe
                        return jsonInput.replace("original content", "changed content");
                    });

            StepVerifier.create(helper.changeCodesAndReturnJson(input, "", false))
                    .assertNext(result -> {
                        assertNotNull(result.getId(), "Id should be generated if environment is empty");
                        assertEquals("changed content", result.getContent());
                    })
                    .verifyComplete();

            codesUtilsMock.verify(() -> com.itexpert.content.core.utils.CodesUtils.changeCodes(anyString(), eq(""), eq(false)));
        }
    }

    @Test
    void changeCodesAndReturnJson_shouldNotGenerateIdIfEnvironmentNotEmpty() {
        ContentNode input = new ContentNode();
        UUID fixedId = UUID.randomUUID();
        input.setId(fixedId);
        input.setContent("original content");

        try (MockedStatic<com.itexpert.content.core.utils.CodesUtils> codesUtilsMock = Mockito.mockStatic(com.itexpert.content.core.utils.CodesUtils.class)) {
            codesUtilsMock.when(() -> com.itexpert.content.core.utils.CodesUtils.changeCodes(anyString(), eq("prod"), eq(true)))
                    .thenAnswer(invocation -> invocation.getArgument(0)); // renvoyer le JSON tel quel

            StepVerifier.create(helper.changeCodesAndReturnJson(input, "prod", true))
                    .assertNext(result -> {
                        assertEquals(fixedId, result.getId(), "Id should not change if environment is set");
                        assertEquals("original content", result.getContent());
                    })
                    .verifyComplete();

            codesUtilsMock.verify(() -> com.itexpert.content.core.utils.CodesUtils.changeCodes(anyString(), eq("prod"), eq(true)));
        }
    }
}

