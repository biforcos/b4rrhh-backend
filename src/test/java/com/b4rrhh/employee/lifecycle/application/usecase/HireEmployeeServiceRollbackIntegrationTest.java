package com.b4rrhh.employee.lifecycle.application.usecase;

import com.b4rrhh.support.EsquemaRealInitializer;
import com.b4rrhh.employee.employee.application.service.EmployeeTypeCatalogValidator;
import com.b4rrhh.employee.employee.application.usecase.CreateEmployeeService;
import com.b4rrhh.employee.employee.domain.port.EmployeeRepository;
import com.b4rrhh.employee.employee.infrastructure.persistence.EmployeePersistenceAdapter;
import com.b4rrhh.employee.lifecycle.application.command.HireEmployeeCommand;
import com.b4rrhh.employee.lifecycle.application.participant.ContractParticipant;
import com.b4rrhh.employee.lifecycle.application.participant.CostCenterParticipant;
import com.b4rrhh.employee.lifecycle.application.participant.EmployeeCoreParticipant;
import com.b4rrhh.employee.lifecycle.application.participant.LaborClassificationParticipant;
import com.b4rrhh.employee.lifecycle.application.participant.PresenceParticipant;
import com.b4rrhh.employee.lifecycle.application.participant.WorkCenterParticipant;
import com.b4rrhh.employee.lifecycle.application.participant.WorkingTimeParticipant;
import com.b4rrhh.employee.lifecycle.application.port.NextEmployeeNumberPort;
import com.b4rrhh.employee.lifecycle.application.service.HireEmployeePreConditionValidator;
import com.b4rrhh.employee.lifecycle.domain.exception.HireEmployeeBusinessValidationException;
import com.b4rrhh.employee.lifecycle.domain.exception.HireEmployeeCatalogValueInvalidException;
import com.b4rrhh.employee.presence.domain.model.Presence;
import com.b4rrhh.employee.working_time.domain.exception.InvalidWorkingTimePercentageException;
import com.b4rrhh.employee.workcenter.domain.exception.WorkCenterCatalogValueInvalidException;
import com.b4rrhh.employee.workcenter.domain.port.WorkCenterCompanyLookupPort;
import com.b4rrhh.employee.workcenter.domain.service.WorkCenterCompanyValidator;
import com.b4rrhh.rulesystem.domain.model.RuleEntity;
import com.b4rrhh.rulesystem.domain.port.RuleEntityRepository;
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

import java.math.BigDecimal;
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
        HireEmployeeService.class,
        HireEmployeeServiceRollbackIntegrationTest.HireEmployeeRollbackTestConfig.class
})
// El esquema no lo declara el test: es el de produccion, aplicado por Flyway
// una vez y clonado para este contexto (ver EsquemaReal). Los tests van sin
// transaccion (NOT_SUPPORTED) para mirar el rollback del servicio desde
// fuera; las migraciones no siembran ningun empleado, asi que el count
// parte de cero y el rollback debe devolverlo a cero.
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = EsquemaRealInitializer.class)
class HireEmployeeServiceRollbackIntegrationTest {

    @Autowired
    private HireEmployeeService service;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void rollsBackCreatedEmployeeWhenAnySubOperationFails() {
        HireEmployeeCommand command = new HireEmployeeCommand(
                "ESP",
                "INTERNAL",
                "Ana",
                "Lopez",
                null,
                "Ani",
                LocalDate.of(2026, 3, 23),
                "HIRE",
                "COMP",
                "BAD_WC",
                new HireEmployeeCommand.HireEmployeeContractCommand("CON", "SUB"),
                new HireEmployeeCommand.HireEmployeeLaborClassificationCommand("AGR", "CAT"),
                null,
                new HireEmployeeCommand.HireEmployeeWorkingTimeCommand(new BigDecimal("75"))
        );

        assertThrows(HireEmployeeCatalogValueInvalidException.class, () -> service.hire(command));

        Long employeeCount = jdbcTemplate.queryForObject(
                "select count(*) from employee.employee where rule_system_code = ? and employee_type_code = ?",
                Long.class,
                "ESP",
                "INTERNAL"
        );

        assertEquals(0L, employeeCount);
    }

    @Test
    void rollsBackCreatedEmployeeWhenWorkingTimeCreationFails() {
        HireEmployeeCommand command = new HireEmployeeCommand(
                "ESP",
                "INTERNAL",
                "Ana",
                "Lopez",
                null,
                "Ani",
                LocalDate.of(2026, 3, 23),
                "HIRE",
                "COMP",
                "WC1",
                new HireEmployeeCommand.HireEmployeeContractCommand("CON", "SUB"),
                new HireEmployeeCommand.HireEmployeeLaborClassificationCommand("AGR", "CAT"),
                null,
                new HireEmployeeCommand.HireEmployeeWorkingTimeCommand(new BigDecimal("150"))
        );

        assertThrows(HireEmployeeBusinessValidationException.class, () -> service.hire(command));

        Long employeeCount = jdbcTemplate.queryForObject(
                "select count(*) from employee.employee where rule_system_code = ? and employee_type_code = ?",
                Long.class,
                "ESP",
                "INTERNAL"
        );

        assertEquals(0L, employeeCount);
    }

    @TestConfiguration
    static class HireEmployeeRollbackTestConfig {

        @Bean
        NextEmployeeNumberPort nextEmployeeNumberPort() {
            return ruleSystemCode -> "EMP000001";
        }

        @Bean
        RuleEntityRepository ruleEntityRepository() {
            RuleEntity activeEntity = new RuleEntity(
                    1L, "ESP", "EMPLOYEE_TYPE", "INTERNAL", "Internal", null,
                    true, LocalDate.of(1900, 1, 1), null,
                    LocalDateTime.now(), LocalDateTime.now()
            );
            return new RuleEntityRepository() {
                @Override public List<RuleEntity> findAll() { return List.of(); }
                @Override public List<RuleEntity> findByFilters(String rs, String type, String code, Boolean active, LocalDate ref) { return List.of(); }
                @Override public java.util.Optional<RuleEntity> findApplicableByBusinessKey(String rs, String type, String code, LocalDate ref) { return java.util.Optional.empty(); }
                @Override public java.util.Optional<RuleEntity> findByBusinessKey(String rs, String type, String code) { return java.util.Optional.of(activeEntity); }
                @Override public java.util.Optional<RuleEntity> findByBusinessKeyAndStartDate(String rs, String type, String code, LocalDate start) { return java.util.Optional.empty(); }
                @Override public boolean existsOverlapExcludingStartDate(String rs, String type, String code, LocalDate pStart, LocalDate pEnd, LocalDate excluded) { return false; }
                @Override public void deleteByBusinessKeyAndStartDate(String rs, String type, String code, LocalDate start) {}
                @Override public RuleEntity save(RuleEntity e) { return e; }
            };
        }

        @Bean
        EmployeeTypeCatalogValidator employeeTypeCatalogValidator(RuleEntityRepository ruleEntityRepository) {
            return new EmployeeTypeCatalogValidator(ruleEntityRepository);
        }

        @Bean
        WorkCenterCompanyLookupPort workCenterCompanyLookupPort() {
            return (ruleSystemCode, workCenterCode, referenceDate) ->
                ("BAD_WC".equals(workCenterCode) || "WC1".equals(workCenterCode))
                    ? java.util.Optional.of("COMP")
                    : java.util.Optional.empty();
        }

        @Bean
        WorkCenterCompanyValidator workCenterCompanyValidator(WorkCenterCompanyLookupPort workCenterCompanyLookupPort) {
            return new WorkCenterCompanyValidator(workCenterCompanyLookupPort);
        }

        @Bean
        HireEmployeePreConditionValidator hireEmployeePreConditionValidator(
                WorkCenterCompanyValidator workCenterCompanyValidator,
                EmployeeTypeCatalogValidator employeeTypeCatalogValidator) {
            return new HireEmployeePreConditionValidator(workCenterCompanyValidator, employeeTypeCatalogValidator);
        }

        @Bean
        EmployeeCoreParticipant employeeCoreParticipant(EmployeeRepository employeeRepository) {
            return new EmployeeCoreParticipant(new CreateEmployeeService(employeeRepository));
        }

        @Bean
        PresenceParticipant presenceParticipant() {
            return new PresenceParticipant(command -> new Presence(
                    10L, 1L, 1,
                    command.companyCode(), command.entryReasonCode(), command.exitReasonCode(),
                    command.startDate(), command.endDate(),
                    LocalDateTime.now(), LocalDateTime.now()
            ));
        }

        @Bean
        WorkCenterParticipant workCenterParticipant() {
            return new WorkCenterParticipant(command -> {
                if ("BAD_WC".equals(command.workCenterCode())) {
                    throw new WorkCenterCatalogValueInvalidException("workCenterCode", command.workCenterCode());
                }
                return null;
            });
        }

        @Bean
        CostCenterParticipant costCenterParticipant() {
            return new CostCenterParticipant(command -> null);
        }

        @Bean
        ContractParticipant contractParticipant() {
            return new ContractParticipant(command -> null);
        }

        @Bean
        LaborClassificationParticipant laborClassificationParticipant() {
            return new LaborClassificationParticipant(command -> null);
        }

        @Bean
        WorkingTimeParticipant workingTimeParticipant() {
            return new WorkingTimeParticipant(command -> {
                if (command.workingTimePercentage().compareTo(new BigDecimal("100")) > 0) {
                    throw new InvalidWorkingTimePercentageException(
                            "workingTimePercentage must be greater than 0 and less than or equal to 100");
                }
                return null;
            });
        }
    }
}
