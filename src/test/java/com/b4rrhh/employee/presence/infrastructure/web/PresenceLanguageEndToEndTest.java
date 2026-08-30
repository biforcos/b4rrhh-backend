package com.b4rrhh.employee.presence.infrastructure.web;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Extremo a extremo (backend#31): la ficha de presencia con {@code Accept-Language}
 * sirviendo una traduccion sembrada, contra el esquema real. Es el segundo caso que esta
 * franja tenia que cubrir: el {@code languageCode} atraveso nueve puertos sin que nadie lo
 * pasara (#24) y ningun test lo vio, porque los tests con mock probaban el adaptador, no
 * la respuesta HTTP — el parametro llegaba porque el test se lo daba. Aqui el idioma tiene
 * que viajar de la cabecera al resolutor de literales de verdad, y se afirma sobre el
 * cuerpo: un test que solo mirara el 200 recibiria el literal base y no se enteraria.
 */
@TestWebSobreEsquemaReal
class PresenceLanguageEndToEndTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private HireEmployeeUseCase hireEmployeeUseCase;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @WithMockUser(roles = "ADMIN")
    void presencesServeTheSeededTranslationWhenAcceptLanguageAsksForIt() throws Exception {
        jdbcTemplate.update("""
                insert into rulesystem.rule_entity_translation (rule_entity_id, language_code, name)
                select id, 'es-ES', 'Contratación' from rulesystem.rule_entity
                 where rule_system_code = 'ESP'
                   and rule_entity_type_code = 'EMPLOYEE_PRESENCE_ENTRY_REASON'
                   and code = 'HIRING'
                """);

        String employeeNumber = hireBaselineEmployee();

        mockMvc.perform(get("/employees/ESP/INTERNAL/{employeeNumber}/presences", employeeNumber)
                        .header("Accept-Language", "es-ES"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].entryReasonCode").value("HIRING"))
                .andExpect(jsonPath("$[0].entryReasonName").value("Contratación"));

        // Sin cabecera, el literal base: la traduccion solo sale cuando se pide.
        mockMvc.perform(get("/employees/ESP/INTERNAL/{employeeNumber}/presences", employeeNumber))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].entryReasonName").value("Hiring"));
    }

    /** El escenario base de {@code HireEmployeeBaselineFlywayIntegrationTest}, en un solo alta. */
    private String hireBaselineEmployee() {
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
                null,
                new HireEmployeeCommand.HireEmployeeWorkingTimeCommand(new BigDecimal("100"))
        )).employee().employeeNumber();
    }
}
