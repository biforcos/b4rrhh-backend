package com.b4rrhh.employee.employee.infrastructure.web;

import com.b4rrhh.employee.lifecycle.application.command.TerminateEmployeeCommand;
import com.b4rrhh.employee.lifecycle.application.usecase.TerminateEmployeeUseCase;
import com.b4rrhh.support.DatosDePrueba;
import com.b4rrhh.support.TestWebSobreEsquemaReal;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * El estado del empleado se lee de sus presencias en la fecha que se pregunta (b4rrhh/backend#148).
 *
 * <p>Se grababa en el momento de registrar el cese: con un cese para el 30/09 grabado el 28, la
 * cabecera decía «Baja» el 29. Un estado guardado que depende de la fecha miente entre el día que
 * se graba y el día que se cumple. El día del cese es el último de la presencia, así que ese día
 * todavía es alta; la baja empieza al siguiente.
 */
@TestWebSobreEsquemaReal
@WithMockUser(roles = "ADMIN")
class TheStatusIsReadFromThePresencesOnTheDateAskedEndToEndTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TerminateEmployeeUseCase terminateEmployeeUseCase;

    @Test
    void aCeaseRecordedForTheThirtiethLeavesTheEmployeeActiveUntilThenAndTerminatedFromTheFirst() throws Exception {
        String number = hire("2026-04-01", DatosDePrueba.dni());
        terminate(number, "2026-09-30");

        statusOn(number, "2026-09-29")
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.statusDate").value("2026-09-29"))
                .andExpect(jsonPath("$.statusSince").value("2026-04-01"))
                .andExpect(jsonPath("$.plannedTerminationDate").value("2026-09-30"))
                .andExpect(jsonPath("$.plannedHireDate").value(nullValue()));

        statusOn(number, "2026-09-30")
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.plannedTerminationDate").value("2026-09-30"));

        statusOn(number, "2026-10-01")
                .andExpect(jsonPath("$.status").value("TERMINATED"))
                .andExpect(jsonPath("$.statusSince").value("2026-10-01"))
                .andExpect(jsonPath("$.plannedTerminationDate").value(nullValue()))
                .andExpect(jsonPath("$.plannedHireDate").value(nullValue()));
    }

    @Test
    void aRehireInTheFutureLeavesTheEmployeeTerminatedUntilItsDate() throws Exception {
        String dni = DatosDePrueba.dni();
        String number = hire("2026-04-01", dni);
        terminate(number, "2026-05-13");
        rehire(number, "2026-07-01", dni);

        statusOn(number, "2026-06-15")
                .andExpect(jsonPath("$.status").value("TERMINATED"))
                .andExpect(jsonPath("$.statusSince").value("2026-05-14"))
                .andExpect(jsonPath("$.plannedHireDate").value("2026-07-01"));

        statusOn(number, "2026-07-01")
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.statusSince").value("2026-07-01"))
                .andExpect(jsonPath("$.plannedTerminationDate").value(nullValue()))
                .andExpect(jsonPath("$.plannedHireDate").value(nullValue()));
    }

    @Test
    void aHireInTheFutureIsNotYetAnActiveEmployee() throws Exception {
        String number = hire("2026-11-01", DatosDePrueba.dni());

        statusOn(number, "2026-10-15")
                .andExpect(jsonPath("$.status").value("NOT_HIRED"))
                .andExpect(jsonPath("$.statusSince").value(nullValue()))
                .andExpect(jsonPath("$.plannedHireDate").value("2026-11-01"));

        statusOn(number, "2026-11-01")
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.statusSince").value("2026-11-01"));
    }

    @Test
    void withoutADateTheStatusIsTheOneOfToday() throws Exception {
        LocalDate today = LocalDate.now();
        String number = hire(today.minusMonths(1).toString(), DatosDePrueba.dni());
        terminate(number, today.plusDays(1).toString());

        mockMvc.perform(get("/employees/ESP/INTERNAL/{n}", number))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.statusDate").value(today.toString()))
                .andExpect(jsonPath("$.plannedTerminationDate").value(today.plusDays(1).toString()));
    }

    @Test
    void theDirectoryAndItsFilterSayTheSameAsTheCard() throws Exception {
        LocalDate today = LocalDate.now();
        String number = hire(today.minusMonths(1).toString(), DatosDePrueba.dni());
        terminate(number, today.plusDays(1).toString());

        mockMvc.perform(get("/employees").param("q", number).param("ruleSystemCode", "ESP"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].employeeNumber").value(number))
                .andExpect(jsonPath("$.items[0].status").value("ACTIVE"));

        mockMvc.perform(get("/employees").param("q", number).param("ruleSystemCode", "ESP")
                        .param("status", "ACTIVE"))
                .andExpect(jsonPath("$.items[*].employeeNumber").value(hasItem(number)));

        mockMvc.perform(get("/employees").param("q", number).param("ruleSystemCode", "ESP")
                        .param("status", "TERMINATED"))
                .andExpect(jsonPath("$.items[*].employeeNumber").value(not(hasItem(number))));
    }

    @Test
    void theTerminationAnswersWithTheStatusOfToday() throws Exception {
        LocalDate today = LocalDate.now();
        String number = hire(today.minusMonths(1).toString(), DatosDePrueba.dni());

        mockMvc.perform(post("/employees/ESP/INTERNAL/{n}/terminate", number)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "terminationDate": "%s", "exitReasonCode": "TERMINATION" }
                                """.formatted(today.plusDays(10))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    private ResultActions statusOn(String number, String date) throws Exception {
        return mockMvc.perform(get("/employees/ESP/INTERNAL/{n}", number).param("referenceDate", date))
                .andExpect(status().isOk());
    }

    private String hire(String hireDate, String dni) throws Exception {
        return JsonPath.read(mockMvc.perform(post("/employees/hire")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ruleSystemCode": "ESP",
                                  "employeeTypeCode": "INTERNAL",
                                  "firstName": "Ana",
                                  "lastName1": "Lopez",
                                  "identifier": { "identifierTypeCode": "NATIONAL_ID", "identifierValue": "%s", "issuingCountryCode": "ESP" },
                                  "hireDate": "%s",
                                  "companyCode": "ES01",
                                  "workCenterCode": "MAIN_OFFICE",
                                  "costCenterDistribution": { "items": [ { "costCenterCode": "CC_ADMIN", "allocationPercentage": 100.0 } ] },
                                  "contract": { "contractTypeCode": "100", "contractSubtypeCode": "01" },
                                  "laborClassification": { "agreementCode": "99002405011982", "agreementCategoryCode": "99002405-G3" },
                                  "workingTime": { "workingTimePercentage": 100 }
                                }
                                """.formatted(dni, hireDate)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.employeeNumber");
    }

    private void terminate(String number, String terminationDate) {
        terminateEmployeeUseCase.terminate(new TerminateEmployeeCommand(
                "ESP", "INTERNAL", number, LocalDate.parse(terminationDate), "TERMINATION"));
    }

    private void rehire(String number, String rehireDate, String dni) throws Exception {
        mockMvc.perform(post("/employees/ESP/INTERNAL/{n}/rehire", number)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rehireDate": "%s",
                                  "entryReasonCode": "REHIRE",
                                  "companyCode": "ES01",
                                  "identifier": { "identifierTypeCode": "NATIONAL_ID", "identifierValue": "%s", "issuingCountryCode": "ESP" },
                                  "workCenter": { "workCenterCode": "MAIN_OFFICE" },
                                  "costCenterDistribution": { "items": [ { "costCenterCode": "CC_ADMIN", "allocationPercentage": 100.0 } ] },
                                  "contract": { "contractTypeCode": "100", "contractSubtypeCode": "01" },
                                  "laborClassification": { "agreementCode": "99002405011982", "agreementCategoryCode": "99002405-G3" },
                                  "workingTime": { "workingTimePercentage": 100 }
                                }
                                """.formatted(rehireDate, dni)))
                .andExpect(status().isCreated());
    }
}
