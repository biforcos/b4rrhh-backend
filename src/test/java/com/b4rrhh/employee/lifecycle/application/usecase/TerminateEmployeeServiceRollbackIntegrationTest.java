package com.b4rrhh.employee.lifecycle.application.usecase;

import com.b4rrhh.support.EsquemaRealInitializer;
import com.b4rrhh.employee.absence.application.usecase.CloseOpenAbsenceAtTerminationUseCase;
import com.b4rrhh.employee.contract.application.usecase.ListEmployeeContractsUseCase;
import com.b4rrhh.employee.employee.application.usecase.GetEmployeeByBusinessKeyUseCase;
import com.b4rrhh.employee.employee.domain.port.EmployeeRepository;
import com.b4rrhh.employee.employee.infrastructure.persistence.EmployeePersistenceAdapter;
import com.b4rrhh.employee.labor_classification.application.usecase.ListEmployeeLaborClassificationsUseCase;
import com.b4rrhh.employee.lifecycle.application.command.TerminateEmployeeCommand;
import com.b4rrhh.employee.lifecycle.application.model.TerminationContext;
import com.b4rrhh.employee.lifecycle.application.participant.AbsenceTerminationParticipant;
import com.b4rrhh.employee.lifecycle.application.participant.CostCenterTerminationParticipant;
import com.b4rrhh.employee.lifecycle.application.participant.ContractTerminationParticipant;
import com.b4rrhh.employee.lifecycle.application.participant.LaborClassificationTerminationParticipant;
import com.b4rrhh.employee.lifecycle.application.participant.PresenceTerminationParticipant;
import com.b4rrhh.employee.lifecycle.application.participant.WorkCenterTerminationParticipant;
import com.b4rrhh.employee.lifecycle.application.port.TerminationParticipant;
import com.b4rrhh.employee.lifecycle.application.service.TerminationPreConditionValidator;
import com.b4rrhh.employee.lifecycle.domain.exception.TerminateEmployeeConflictException;
import com.b4rrhh.employee.presence.application.usecase.ListEmployeePresencesUseCase;
import com.b4rrhh.employee.presence.domain.model.Presence;
import com.b4rrhh.employee.workcenter.application.usecase.ListEmployeeWorkCentersUseCase;
import com.b4rrhh.employee.working_time.application.usecase.ListEmployeeWorkingTimesUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.ContextConfiguration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.flyway.enabled=false"
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({
        EmployeePersistenceAdapter.class,
        TerminateEmployeeService.class,
        TerminateEmployeeServiceRollbackIntegrationTest.TerminateEmployeeRollbackTestConfig.class
})
// El esquema no lo declara el test: es el de produccion, aplicado por Flyway
// una vez y clonado para este contexto (ver EsquemaReal). El test va sin
// transaccion (NOT_SUPPORTED): el insert del @BeforeEach queda escrito de
// verdad, y lo que se comprueba es que el servicio deshace lo suyo.
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = EsquemaRealInitializer.class)
class TerminateEmployeeServiceRollbackIntegrationTest {

    @Autowired
    private TerminateEmployeeService service;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpDatos() {
        jdbcTemplate.update(
                "insert into employee.employee (rule_system_code, employee_type_code, employee_number, first_name, last_name_1, status, created_at, updated_at) values (?, ?, ?, ?, ?, ?, current_timestamp, current_timestamp)",
                "ESP", "INTERNAL", "EMP001", "Ana", "Lopez", "ACTIVE");
    }

    @Test
    void rollsBackWhenParticipantThrowsBeforeEmployeeStatusSave() {
        TerminateEmployeeCommand command = new TerminateEmployeeCommand(
                "ESP", "INTERNAL", "EMP001", LocalDate.of(2026, 3, 31), "VOL");

        assertThrows(TerminateEmployeeConflictException.class, () -> service.terminate(command));

        String persistedStatus = jdbcTemplate.queryForObject(
                "select status from employee.employee where rule_system_code = ? and employee_type_code = ? and employee_number = ?",
                String.class, "ESP", "INTERNAL", "EMP001");

        assertEquals("ACTIVE", persistedStatus);
    }

    @TestConfiguration
    static class TerminateEmployeeRollbackTestConfig {

        @Bean
        GetEmployeeByBusinessKeyUseCase getEmployeeByBusinessKeyUseCase(EmployeeRepository employeeRepository) {
            return (ruleSystemCode, employeeTypeCode, employeeNumber) -> employeeRepository
                    .findByRuleSystemCodeAndEmployeeTypeCodeAndEmployeeNumber(
                            ruleSystemCode, employeeTypeCode, employeeNumber);
        }

        @Bean
        ListEmployeePresencesUseCase listEmployeePresencesUseCase() {
            return (rs, et, en) -> List.of(new Presence(
                    10L, 100L, 1, "COMP", "HIRE", null,
                    LocalDate.of(2026, 1, 1), null,
                    LocalDateTime.now(), LocalDateTime.now()));
        }

        @Bean
        ListEmployeeContractsUseCase listEmployeeContractsUseCase() {
            return command -> List.of();
        }

        @Bean
        ListEmployeeLaborClassificationsUseCase listEmployeeLaborClassificationsUseCase() {
            return command -> List.of();
        }

        @Bean
        ListEmployeeWorkCentersUseCase listEmployeeWorkCentersUseCase() {
            return (rs, et, en) -> List.of();
        }

        @Bean
        ListEmployeeWorkingTimesUseCase listEmployeeWorkingTimesUseCase() {
            return command -> List.of();
        }

        @Bean
        TerminationPreConditionValidator terminationPreConditionValidator(
                GetEmployeeByBusinessKeyUseCase getEmployeeByBusinessKey,
                ListEmployeePresencesUseCase listPresences,
                ListEmployeeContractsUseCase listContracts,
                ListEmployeeLaborClassificationsUseCase listLaborClassifications,
                ListEmployeeWorkCentersUseCase listWorkCenters,
                ListEmployeeWorkingTimesUseCase listWorkingTimes) {
            return new TerminationPreConditionValidator(
                    getEmployeeByBusinessKey, listPresences, listContracts,
                    listLaborClassifications, listWorkCenters, listWorkingTimes);
        }

        @Bean
        TerminationParticipant workingTimeTerminationParticipant() {
            return new TerminationParticipant() {
                @Override public int order() { return 10; }
                @Override public void participate(TerminationContext ctx) {
                    throw new TerminateEmployeeConflictException(
                            "Simulated working time conflict for rollback test");
                }
            };
        }

        @Bean
        WorkCenterTerminationParticipant workCenterTerminationParticipant() {
            return new WorkCenterTerminationParticipant(
                    (rs, et, en) -> List.of(),
                    command -> null);
        }

        @Bean
        CostCenterTerminationParticipant costCenterTerminationParticipant() {
            return new CostCenterTerminationParticipant(
                    (rs, et, en, date) -> {});
        }

        @Bean
        ContractTerminationParticipant contractTerminationParticipant(
                ListEmployeeContractsUseCase listContracts) {
            return new ContractTerminationParticipant(
                    listContracts,
                    command -> null);
        }

        @Bean
        LaborClassificationTerminationParticipant laborClassificationTerminationParticipant(
                ListEmployeeLaborClassificationsUseCase listLaborClassifications) {
            return new LaborClassificationTerminationParticipant(
                    listLaborClassifications,
                    command -> null);
        }

        @Bean
        PresenceTerminationParticipant presenceTerminationParticipant(
                ListEmployeePresencesUseCase listPresences) {
            return new PresenceTerminationParticipant(
                    listPresences,
                    command -> new Presence(
                            10L, 100L, command.presenceNumber(), "COMP", "HIRE",
                            command.exitReasonCode(),
                            LocalDate.of(2026, 1, 1), command.endDate(),
                            LocalDateTime.now(), LocalDateTime.now()));
        }

        @Bean
        CloseOpenAbsenceAtTerminationUseCase closeOpenAbsenceAtTerminationUseCase() {
            return (ruleSystemCode, employeeTypeCode, employeeNumber, terminationDate) -> {};
        }

        @Bean
        AbsenceTerminationParticipant absenceTerminationParticipant(
                CloseOpenAbsenceAtTerminationUseCase closeOpenAbsenceAtTerminationUseCase) {
            return new AbsenceTerminationParticipant(closeOpenAbsenceAtTerminationUseCase);
        }
    }
}
