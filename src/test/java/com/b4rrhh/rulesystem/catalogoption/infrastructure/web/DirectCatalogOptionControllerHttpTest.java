package com.b4rrhh.rulesystem.catalogoption.infrastructure.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.b4rrhh.rulesystem.catalogoption.application.query.GetDirectCatalogOptionsQuery;
import com.b4rrhh.rulesystem.catalogoption.application.usecase.DirectCatalogOptionsResult;
import com.b4rrhh.rulesystem.catalogoption.application.usecase.GetDirectCatalogOptionsUseCase;
import com.b4rrhh.rulesystem.catalogoption.domain.model.DirectCatalogOption;
import com.b4rrhh.shared.infrastructure.web.language.ResponseLanguageArgumentResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DirectCatalogOptionControllerHttpTest {

    @Mock
    private GetDirectCatalogOptionsUseCase getDirectCatalogOptionsUseCase;

        private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        DirectCatalogOptionController controller = new DirectCatalogOptionController(getDirectCatalogOptionsUseCase);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new DirectCatalogOptionExceptionHandler())
                .setCustomArgumentResolvers(new ResponseLanguageArgumentResolver())
                .build();
    }

    @Test
    void returnsDirectCatalogOptions() throws Exception {
        when(getDirectCatalogOptionsUseCase.get(any(GetDirectCatalogOptionsQuery.class)))
                .thenReturn(new DirectCatalogOptionsResult(
                        "ES_DEFAULT",
                        "WORK_CENTER",
                        LocalDate.of(2026, 3, 22),
                        List.of(new DirectCatalogOption(
                                "MAIN_OFFICE",
                                "Oficina central",
                                true,
                                LocalDate.of(2020, 1, 1),
                                null
                        ))
                ));

        MvcResult result = mockMvc.perform(get("/catalog-options/direct")
                        .queryParam("ruleSystemCode", "ES_DEFAULT")
                        .queryParam("ruleEntityTypeCode", "WORK_CENTER")
                        .queryParam("referenceDate", "2026-03-22")
                        .queryParam("q", "office"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ruleSystemCode").value("ES_DEFAULT"))
                .andExpect(jsonPath("$.ruleEntityTypeCode").value("WORK_CENTER"))
                .andExpect(jsonPath("$.items[0].code").value("MAIN_OFFICE"))
                .andExpect(jsonPath("$.items[0].name").value("Oficina central"))
                .andExpect(jsonPath("$.items[0].active").value(true))
                .andExpect(jsonPath("$.items[0].id").doesNotExist())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertDateNode(body.path("referenceDate"), LocalDate.of(2026, 3, 22));
        assertDateNode(body.path("items").path(0).path("startDate"), LocalDate.of(2020, 1, 1));
        assertTrue(body.path("items").path(0).path("endDate").isNull());

        ArgumentCaptor<GetDirectCatalogOptionsQuery> captor =
                ArgumentCaptor.forClass(GetDirectCatalogOptionsQuery.class);
        verify(getDirectCatalogOptionsUseCase).get(captor.capture());
        assertEquals("ES_DEFAULT", captor.getValue().ruleSystemCode());
        assertEquals("WORK_CENTER", captor.getValue().ruleEntityTypeCode());
        assertEquals(LocalDate.of(2026, 3, 22), captor.getValue().referenceDate());
        assertEquals("office", captor.getValue().q());
    }

    @Test
    void returnsEmptyItemsWhenNoResults() throws Exception {
        when(getDirectCatalogOptionsUseCase.get(any(GetDirectCatalogOptionsQuery.class)))
                .thenReturn(new DirectCatalogOptionsResult(
                        "ES_DEFAULT",
                        "WORK_CENTER",
                        LocalDate.of(2026, 3, 22),
                        List.of()
                ));

        mockMvc.perform(get("/catalog-options/direct")
                        .queryParam("ruleSystemCode", "ES_DEFAULT")
                        .queryParam("ruleEntityTypeCode", "WORK_CENTER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    void returns400WhenRuleSystemCodeIsBlank() throws Exception {
        when(getDirectCatalogOptionsUseCase.get(any(GetDirectCatalogOptionsQuery.class)))
                .thenThrow(new IllegalArgumentException("ruleSystemCode is required"));

        mockMvc.perform(get("/catalog-options/direct")
                        .queryParam("ruleSystemCode", "   ")
                        .queryParam("ruleEntityTypeCode", "WORK_CENTER"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("ruleSystemCode is required"));
    }

    @Test
    void returns400WhenRuleEntityTypeCodeIsBlank() throws Exception {
        when(getDirectCatalogOptionsUseCase.get(any(GetDirectCatalogOptionsQuery.class)))
                .thenThrow(new IllegalArgumentException("ruleEntityTypeCode is required"));

        mockMvc.perform(get("/catalog-options/direct")
                        .queryParam("ruleSystemCode", "ES_DEFAULT")
                        .queryParam("ruleEntityTypeCode", "   "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("ruleEntityTypeCode is required"));
    }

    @Test
    void capturesReferenceDateAndQFilters() throws Exception {
        when(getDirectCatalogOptionsUseCase.get(any(GetDirectCatalogOptionsQuery.class)))
                .thenReturn(new DirectCatalogOptionsResult(
                        "ES_DEFAULT",
                        "WORK_CENTER",
                        LocalDate.of(2026, 3, 22),
                        List.of()
                ));

        mockMvc.perform(get("/catalog-options/direct")
                        .queryParam("ruleSystemCode", "ES_DEFAULT")
                        .queryParam("ruleEntityTypeCode", "WORK_CENTER")
                        .queryParam("referenceDate", "2026-03-22")
                        .queryParam("q", "main"))
                .andExpect(status().isOk());

        ArgumentCaptor<GetDirectCatalogOptionsQuery> captor =
                ArgumentCaptor.forClass(GetDirectCatalogOptionsQuery.class);
        verify(getDirectCatalogOptionsUseCase).get(captor.capture());
        assertEquals(LocalDate.of(2026, 3, 22), captor.getValue().referenceDate());
        assertEquals("main", captor.getValue().q());
    }

        private void assertDateNode(JsonNode node, LocalDate expectedDate) {
                if (node.isTextual()) {
                        assertEquals(expectedDate.toString(), node.asText());
                        return;
                }

                assertTrue(node.isArray());
                assertEquals(3, node.size());
                assertEquals(expectedDate.getYear(), node.get(0).asInt());
                assertEquals(expectedDate.getMonthValue(), node.get(1).asInt());
                assertEquals(expectedDate.getDayOfMonth(), node.get(2).asInt());
        }
}
