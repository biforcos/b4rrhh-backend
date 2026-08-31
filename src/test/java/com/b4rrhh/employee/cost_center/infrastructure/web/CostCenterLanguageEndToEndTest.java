package com.b4rrhh.employee.cost_center.infrastructure.web;

import com.b4rrhh.employee.lifecycle.application.command.HireEmployeeCommand;
import com.b4rrhh.employee.lifecycle.application.usecase.HireEmployeeUseCase;
import com.b4rrhh.support.TestWebSobreEsquemaReal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Extremo a extremo (backend#27): el reparto de centros de coste con {@code Accept-Language}
 * sirviendo una traduccion sembrada, contra el esquema real. Este vertical resolvia el
 * nombre del centro dentro de los casos de uso; ahora la conversion vive en el assembler de
 * la capa web, y este test afirma que el idioma viaja de la cabecera al resolutor de
 * verdad: sin cabecera, el literal base; con ella, la traduccion.
 */
@TestWebSobreEsquemaReal
class CostCenterLanguageEndToEndTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private HireEmployeeUseCase hireEmployeeUseCase;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @WithMockUser(roles = "ADMIN")
    void costCentersServeTheSeededTranslationWhenAcceptLanguageAsksForIt() throws Exception {
        jdbcTemplate.update("""
                insert into rulesystem.rule_entity_translation (rule_entity_id, language_code, name)
                select id, 'es-ES', 'Administración' from rulesystem.rule_entity
                 where rule_system_code = 'ESP'
                   and rule_entity_type_code = 'COST_CENTER'
                   and code = 'CC_ADMIN'
                """);

        String employeeNumber = hireBaselineEmployeeWithCostCenter();

        mockMvc.perform(get("/employees/ESP/INTERNAL/{employeeNumber}/cost-centers/current", employeeNumber)
                        .header("Accept-Language", "es-ES"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentDistribution.items[0].costCenterCode").value("CC_ADMIN"))
                .andExpect(jsonPath("$.currentDistribution.items[0].costCenterName").value("Administración"));

        // Sin cabecera, el literal base: la traduccion solo sale cuando se pide.
        mockMvc.perform(get("/employees/ESP/INTERNAL/{employeeNumber}/cost-centers/current", employeeNumber))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentDistribution.items[0].costCenterName").value("Administration"));
    }

    /**
     * El escenario base de {@code HireEmployeeBaselineFlywayIntegrationTest}, en un solo
     * alta, con un reparto del 100% al centro de coste sembrado CC_ADMIN.
     */
    private String hireBaselineEmployeeWithCostCenter() {
        return hireEmployeeUseCase.hire(new HireEmployeeCommand(
                "ESP",
                "INTERNAL",
                "Ana",
                "Lopez",
                null,
                "Ani",
                LocalDate.of(2026, 4, 1),
                "HIRING",
                "ES01",
                "MAIN_OFFICE",
                new HireEmployeeCommand.HireEmployeeContractCommand("IND", "FT1"),
                new HireEmployeeCommand.HireEmployeeLaborClassificationCommand("AGR_OFFICE", "CAT_ADMIN"),
                new HireEmployeeCommand.HireEmployeeCostCenterDistributionCommand(List.of(
                        new HireEmployeeCommand.HireEmployeeCostCenterItemCommand("CC_ADMIN", 100.0)
                )),
                new HireEmployeeCommand.HireEmployeeWorkingTimeCommand(new BigDecimal("100"))
        )).employee().employeeNumber();
    }
}
