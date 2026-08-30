package com.b4rrhh.rulesystem.translation.infrastructure.web;

import com.b4rrhh.rulesystem.translation.application.usecase.GetRuleEntityTranslationCoverageUseCase;
import com.b4rrhh.rulesystem.translation.application.usecase.RuleEntityTranslationCoverage;
import com.b4rrhh.rulesystem.translation.application.usecase.RuleEntityTranslationCoverage.MissingCode;
import com.b4rrhh.rulesystem.translation.application.usecase.RuleEntityTranslationCoverage.TypeCoverage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RuleEntityTranslationCoverageControllerHttpTest {

    @Mock
    private GetRuleEntityTranslationCoverageUseCase getCoverageUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new RuleEntityTranslationCoverageController(getCoverageUseCase))
                .build();
    }

    @Test
    void reportsCoverageForTheCanonicalFormOfTheLanguage() throws Exception {
        when(getCoverageUseCase.getCoverage("es-ES")).thenReturn(new RuleEntityTranslationCoverage("es-ES", List.of(
                new TypeCoverage("EMPLOYEE_PRESENCE_ENTRY_REASON", 3, 1, 2, List.of(
                        new MissingCode("FRA", "HIRING", "Hiring"),
                        new MissingCode("PRT", "HIRING", "Hiring")
                ))
        )));

        mockMvc.perform(get("/rule-entity-translations/coverage").param("languageCode", "es_es"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.languageCode").value("es-ES"))
                .andExpect(jsonPath("$.types[0].ruleEntityTypeCode").value("EMPLOYEE_PRESENCE_ENTRY_REASON"))
                .andExpect(jsonPath("$.types[0].total").value(3))
                .andExpect(jsonPath("$.types[0].translated").value(1))
                .andExpect(jsonPath("$.types[0].missing").value(2))
                .andExpect(jsonPath("$.types[0].missingCodes[0].ruleSystemCode").value("FRA"))
                .andExpect(jsonPath("$.types[0].missingCodes[0].code").value("HIRING"))
                .andExpect(jsonPath("$.types[0].missingCodes[0].name").value("Hiring"));
    }

    @Test
    void rejectsALanguageWithoutCanonicalForm() throws Exception {
        mockMvc.perform(get("/rule-entity-translations/coverage").param("languageCode", "spanish"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_LANGUAGE_CODE"));

        verify(getCoverageUseCase, never()).getCoverage(anyString());
    }

    @Test
    void requiresTheLanguage() throws Exception {
        mockMvc.perform(get("/rule-entity-translations/coverage"))
                .andExpect(status().isBadRequest());
    }
}
