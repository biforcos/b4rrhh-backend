package com.b4rrhh.employee.absence.infrastructure.web;

import com.b4rrhh.employee.lifecycle.application.command.TerminateEmployeeCommand;
import com.b4rrhh.employee.lifecycle.application.usecase.TerminateEmployeeUseCase;
import com.b4rrhh.support.DatosDePrueba;
import com.b4rrhh.support.TestWebSobreEsquemaReal;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Una ausencia es de una presencia (b4rrhh/backend#147).
 *
 * <p>Se comprobaban el inicio y el fin por separado, cada uno contra cualquier presencia, así que
 * una ausencia del 10/05 al 05/07 de alguien cesado el 13/05 y readmitido el 01/07 pasaba: su
 * inicio caía en la primera y su fin en la segunda. Que cruce el hueco son dos ausencias o un error
 * de fechas, y en los dos casos hay que decirlo.
 */
@TestWebSobreEsquemaReal
class AnAbsenceFallsWithinOnePresenceEndToEndTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TerminateEmployeeUseCase terminateEmployeeUseCase;

    private String employeeNumber;

    /** Alta el 01/04, cese el 13/05, readmisión el 01/07: dos presencias y un hueco entre ellas. */
    @BeforeEach
    void hireCeaseAndRehire() throws Exception {
        String dni = DatosDePrueba.dni();
        employeeNumber = JsonPath.read(mockMvc.perform(post("/employees/hire")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ruleSystemCode": "ESP",
                                  "employeeTypeCode": "INTERNAL",
                                  "firstName": "Ana",
                                  "lastName1": "Lopez",
                                  "identifier": { "identifierTypeCode": "NATIONAL_ID", "identifierValue": "%s", "issuingCountryCode": "ESP" },
                                  "hireDate": "2026-04-01",
                                  "companyCode": "ES01",
                                  "workCenterCode": "MAIN_OFFICE",
                                  "costCenterDistribution": { "items": [ { "costCenterCode": "CC_ADMIN", "allocationPercentage": 100.0 } ] },
                                  "contract": { "contractTypeCode": "100", "contractSubtypeCode": "01" },
                                  "laborClassification": { "agreementCode": "99002405011982", "agreementCategoryCode": "99002405-G3" },
                                  "workingTime": { "workingTimePercentage": 100 }
                                }
                                """.formatted(dni)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.employeeNumber");

        terminateEmployeeUseCase.terminate(new TerminateEmployeeCommand(
                "ESP", "INTERNAL", employeeNumber, LocalDate.of(2026, 5, 13), "TERMINATION"));

        mockMvc.perform(post("/employees/ESP/INTERNAL/{n}/rehire", employeeNumber)
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rehireDate": "2026-07-01",
                                  "entryReasonCode": "REHIRE",
                                  "companyCode": "ES01",
                                  "identifier": { "identifierTypeCode": "NATIONAL_ID", "identifierValue": "%s", "issuingCountryCode": "ESP" },
                                  "workCenter": { "workCenterCode": "MAIN_OFFICE" },
                                  "costCenterDistribution": { "items": [ { "costCenterCode": "CC_ADMIN", "allocationPercentage": 100.0 } ] },
                                  "contract": { "contractTypeCode": "100", "contractSubtypeCode": "01" },
                                  "laborClassification": { "agreementCode": "99002405011982", "agreementCategoryCode": "99002405-G3" },
                                  "workingTime": { "workingTimePercentage": 100 }
                                }
                                """.formatted(dni)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void anAbsenceAcrossTheGapIsRefusedNamingThePresenceItStartsIn() throws Exception {
        absence("2026-05-10", "2026-07-05")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("ABSENCE_OUTSIDE_PRESENCE_PERIOD"))
                .andExpect(jsonPath("$.message").value(containsString("01/04/2026")))
                .andExpect(jsonPath("$.message").value(containsString("13/05/2026")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void anAbsenceInsideOnePresenceIsAccepted() throws Exception {
        absence("2026-07-02", "2026-07-05")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.endDate").value("2026-07-05"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void anAbsenceStartingOnTheLastDayOfAPresenceAndEndingAfterIsRefused() throws Exception {
        absence("2026-05-13", "2026-05-14")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("ABSENCE_OUTSIDE_PRESENCE_PERIOD"))
                .andExpect(jsonPath("$.message").value(containsString("13/05/2026")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void anOpenAbsenceInAClosedPresenceIsRefused() throws Exception {
        absence("2026-05-10", null)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("ABSENCE_OUTSIDE_PRESENCE_PERIOD"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void anOpenAbsenceInTheOpenPresenceIsAccepted() throws Exception {
        absence("2026-07-02", null)
                .andExpect(status().isOk());
    }

    private ResultActions absence(String startDate, String endDate) throws Exception {
        String end = endDate == null ? "null" : "\"" + endDate + "\"";
        return mockMvc.perform(put("/employees/ESP/INTERNAL/{n}/absences/VACATION/{start}",
                        employeeNumber, startDate)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"endDate\": " + end + " }"));
    }
}
