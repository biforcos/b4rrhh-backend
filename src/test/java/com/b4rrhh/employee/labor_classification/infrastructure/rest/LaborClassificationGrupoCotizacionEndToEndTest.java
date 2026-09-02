package com.b4rrhh.employee.labor_classification.infrastructure.rest;

import com.b4rrhh.employee.lifecycle.application.command.HireEmployeeCommand;
import com.b4rrhh.employee.lifecycle.application.usecase.HireEmployeeUseCase;
import com.b4rrhh.support.TestWebSobreEsquemaReal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Extremo a extremo (backend#41): el grupo de cotizacion sale de la ficha con su literal,
 * contra el esquema real. Era el unico codigo de la ficha que viajaba solo: el ensamblador
 * resolvia el convenio y la categoria pero se paraba en el codigo del grupo.
 *
 * El caso existe sin inventar datos: la categoria {@code 99002405-G1} del convenio real
 * (V61) tiene su grupo {@code 01} en el perfil (V87), y el catalogo GRUPO_COTIZACION (V84)
 * le da el literal. Es cita reglamentaria (ADR-054): no hay traduccion que sembrar, y el
 * literal base ya esta en castellano, asi que con y sin {@code Accept-Language} sale lo
 * mismo. Eso tambien es lo que se afirma.
 */
@TestWebSobreEsquemaReal
class LaborClassificationGrupoCotizacionEndToEndTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private HireEmployeeUseCase hireEmployeeUseCase;

    @Test
    @WithMockUser(roles = "ADMIN")
    void laborClassificationsServeTheGrupoCotizacionWithItsSeededLiteral() throws Exception {
        String employeeNumber = hireEmployeeInRealAgreement();

        mockMvc.perform(get("/employees/ESP/INTERNAL/{employeeNumber}/labor-classifications", employeeNumber)
                        .header("Accept-Language", "es-ES"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].agreementCategoryCode").value("99002405-G1"))
                .andExpect(jsonPath("$[0].grupoCotizacionCode").value("01"))
                .andExpect(jsonPath("$[0].grupoCotizacionName").value("Ingenieros y Licenciados"));

        // Cita reglamentaria: sin cabecera sale el mismo literal, porque no hay otro.
        mockMvc.perform(get("/employees/ESP/INTERNAL/{employeeNumber}/labor-classifications", employeeNumber))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].grupoCotizacionCode").value("01"))
                .andExpect(jsonPath("$[0].grupoCotizacionName").value("Ingenieros y Licenciados"));
    }

    /** El escenario base de {@code HireEmployeeBaselineFlywayIntegrationTest}, sobre el convenio real de la V61. */
    private String hireEmployeeInRealAgreement() {
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
                new HireEmployeeCommand.HireEmployeeLaborClassificationCommand("99002405011982", "99002405-G1"),
                null,
                new HireEmployeeCommand.HireEmployeeWorkingTimeCommand(new BigDecimal("100"))
        )).employee().employeeNumber();
    }
}
