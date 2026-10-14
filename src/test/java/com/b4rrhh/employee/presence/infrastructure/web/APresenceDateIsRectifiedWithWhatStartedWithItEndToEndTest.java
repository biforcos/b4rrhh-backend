package com.b4rrhh.employee.presence.infrastructure.web;

import com.b4rrhh.employee.lifecycle.application.command.TerminateEmployeeCommand;
import com.b4rrhh.employee.lifecycle.application.usecase.TerminateEmployeeUseCase;
import com.b4rrhh.support.DatosDePrueba;
import com.b4rrhh.support.TestWebSobreEsquemaReal;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Rectificar la fecha de alta o de cese de una presencia (backend#172).
 *
 * <p>La sesión de workspace#18 dio de alta a alguien el 01/10 por error y se quedó sin salida: «no
 * puedo corregir de ninguna manera la fecha de alta… Jaque mate». Rectificar mueve la presencia y
 * todo lo que empezó (o acabó) con ella ese mismo día —es un alta, no cinco vigencias que casualmente
 * coinciden—, y se niega cuando la fecha ya se pagó, cuando pisa otra presencia o cuando deja una
 * ausencia fuera.</p>
 */
@TestWebSobreEsquemaReal
class APresenceDateIsRectifiedWithWhatStartedWithItEndToEndTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TerminateEmployeeUseCase terminateEmployeeUseCase;

    // El test va dentro de una transacción que no se confirma: lo que JPA tiene pendiente se escribe
    // antes de leerlo con SQL.
    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void theHireDateMovesThePresenceAndEverythingThatStartedThatDay() throws Exception {
        String employee = hire("2026-10-01");

        mockMvc.perform(rectify(employee, 1, "start", "2026-09-29", false))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.oldDate").value("2026-10-01"))
                .andExpect(jsonPath("$.newDate").value("2026-09-29"))
                .andExpect(jsonPath("$.moves[*].vertical", hasItem("contract")))
                .andExpect(jsonPath("$.moves[*].vertical", hasItem("working_time")));

        assertThat(starts(employee)).containsOnly(LocalDate.of(2026, 9, 29));
        Map<String, Object> trail = jdbcTemplate.queryForMap("""
                select r.field, r.old_date, r.new_date, r.rectified_by
                  from employee.presence_rectification r
                  join employee.employee e on e.id = r.employee_id
                 where e.employee_number = ?
                """, employee);
        assertThat(trail).containsEntry("field", "START_DATE")
                .containsEntry("old_date", java.sql.Date.valueOf("2026-10-01"))
                .containsEntry("new_date", java.sql.Date.valueOf("2026-09-29"))
                .containsEntry("rectified_by", "admin");
    }

    @Test
    void aTrialRunSaysWhatWouldMoveAndMovesNothing() throws Exception {
        String employee = hire("2026-10-01");

        mockMvc.perform(rectify(employee, 1, "start", "2026-09-29", true))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dryRun").value(true))
                .andExpect(jsonPath("$.moves[*].vertical", hasItem("contract")));

        assertThat(starts(employee)).containsOnly(LocalDate.of(2026, 10, 1));
    }

    @Test
    void aPresenceWithADefinitivePayrollIsNotRectifiedAndSaysWhich() throws Exception {
        String employee = hire("2026-04-01");
        jdbcTemplate.update("""
                insert into payroll.payroll (rule_system_code, employee_type_code, employee_number, payroll_period_code,
                                             payroll_type_code, presence_number, status, calculated_at,
                                             calculation_engine_code, calculation_engine_version)
                values ('ESP', 'INTERNAL', ?, '202604', 'NORMAL', 1, 'DEFINITIVE', now(), 'TEST', '1')
                """, employee);

        mockMvc.perform(rectify(employee, 1, "start", "2026-03-30", false))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PRESENCE_RECTIFICATION_PAID"))
                .andExpect(jsonPath("$.message", containsString("202604")));

        assertThat(starts(employee)).containsOnly(LocalDate.of(2026, 4, 1));
    }

    @Test
    void aDateThatRunsIntoAnotherPresenceIsRefused() throws Exception {
        String employee = hire("2026-04-01");
        terminateEmployeeUseCase.terminate(new TerminateEmployeeCommand(
                "ESP", "INTERNAL", employee, LocalDate.of(2026, 5, 13), "TERMINATION"));
        rehire(employee, "2026-07-01");

        mockMvc.perform(rectify(employee, 2, "start", "2026-05-10", false))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PRESENCE_RECTIFICATION_OVERLAP"))
                .andExpect(jsonPath("$.message", containsString("13/05/2026")));
    }

    @Test
    void aMistypedTerminationDateMovesWhatClosedWithIt() throws Exception {
        String employee = hire("2026-04-01");
        terminateEmployeeUseCase.terminate(new TerminateEmployeeCommand(
                "ESP", "INTERNAL", employee, LocalDate.of(2026, 5, 13), "TERMINATION"));

        mockMvc.perform(rectify(employee, 1, "end", "2026-05-20", false))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.moves[*].vertical", hasItem("contract")));

        assertThat(ends(employee)).containsOnly(LocalDate.of(2026, 5, 20));
    }

    @Test
    void anAbsenceLeftOutsideThePresenceIsRefused() throws Exception {
        String employee = hire("2026-04-01");
        jdbcTemplate.update("""
                insert into employee.employee_absence (employee_id, absence_type_code, start_date, start_time, end_date,
                                                       benefit_entitled, created_at, updated_at)
                select id, 'VACATION', date '2026-04-02', 0, date '2026-04-03', false, now(), now()
                  from employee.employee where employee_number = ?
                """, employee);

        mockMvc.perform(rectify(employee, 1, "start", "2026-04-05", false))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PRESENCE_RECTIFICATION_CONFLICT"))
                .andExpect(jsonPath("$.message", containsString("ausencia")));
    }

    /** Las fechas de inicio de la presencia, el contrato, la jornada y la clasificación. */
    private List<LocalDate> starts(String employee) {
        return dates(employee, "start_date");
    }

    private List<LocalDate> ends(String employee) {
        return dates(employee, "end_date");
    }

    private List<LocalDate> dates(String employee, String column) {
        entityManager.flush();
        return jdbcTemplate.queryForList("""
                select t.%1$s from employee.presence t join employee.employee e on e.id = t.employee_id where e.employee_number = ?
                union all
                select t.%1$s from employee.contract t join employee.employee e on e.id = t.employee_id where e.employee_number = ?
                union all
                select t.%1$s from employee.working_time t join employee.employee e on e.id = t.employee_id where e.employee_number = ?
                union all
                select t.%1$s from employee.labor_classification t join employee.employee e on e.id = t.employee_id where e.employee_number = ?
                """.formatted(column), LocalDate.class, employee, employee, employee, employee);
    }

    private static org.springframework.test.web.servlet.RequestBuilder rectify(
            String employee, int presence, String which, String date, boolean dryRun) {
        return post("/employees/ESP/INTERNAL/{n}/presences/{p}/rectify-" + which + "-date", employee, presence)
                .with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"newDate\":\"%s\",\"dryRun\":%s}".formatted(date, dryRun));
    }

    private String hire(String hireDate) throws Exception {
        String response = mockMvc.perform(post("/employees/hire")
                        .with(user("admin").roles("ADMIN"))
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
                                """.formatted(DatosDePrueba.dni(), hireDate)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.employeeNumber");
    }

    private void rehire(String employee, String date) throws Exception {
        String dni = jdbcTemplate.queryForObject("""
                select i.identifier_value from employee.identifier i join employee.employee e on e.id = i.employee_id
                 where e.employee_number = ?
                """, String.class, employee);
        mockMvc.perform(post("/employees/ESP/INTERNAL/{n}/rehire", employee)
                        .with(user("admin").roles("ADMIN"))
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
                                """.formatted(date, dni)))
                .andExpect(status().isCreated());
    }
}
