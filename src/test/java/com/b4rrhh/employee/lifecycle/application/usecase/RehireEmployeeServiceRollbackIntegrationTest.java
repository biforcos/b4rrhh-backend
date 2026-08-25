package com.b4rrhh.employee.lifecycle.application.usecase;

import com.b4rrhh.support.TestPostgresInitializer;
import com.b4rrhh.employee.contract.application.command.CreateContractCommand;
import com.b4rrhh.employee.contract.application.command.ListEmployeeContractsCommand;
import com.b4rrhh.employee.contract.application.usecase.CreateContractUseCase;
import com.b4rrhh.employee.contract.application.usecase.ListEmployeeContractsUseCase;
import com.b4rrhh.employee.contract.domain.model.Contract;
import com.b4rrhh.employee.cost_center.application.usecase.CreateCostCenterDistributionUseCase;
import com.b4rrhh.employee.employee.application.service.EmployeeTypeCatalogValidator;
import com.b4rrhh.employee.employee.application.usecase.GetEmployeeByBusinessKeyUseCase;
import com.b4rrhh.employee.employee.domain.port.EmployeeRepository;
import com.b4rrhh.employee.employee.infrastructure.persistence.EmployeePersistenceAdapter;
import com.b4rrhh.employee.labor_classification.application.command.CreateLaborClassificationCommand;
import com.b4rrhh.employee.labor_classification.application.command.ListEmployeeLaborClassificationsCommand;
import com.b4rrhh.employee.labor_classification.application.usecase.CreateLaborClassificationUseCase;
import com.b4rrhh.employee.labor_classification.application.usecase.ListEmployeeLaborClassificationsUseCase;
import com.b4rrhh.employee.labor_classification.domain.model.LaborClassification;
import com.b4rrhh.employee.lifecycle.application.command.RehireEmployeeCommand;
import com.b4rrhh.employee.lifecycle.domain.exception.RehireEmployeeBusinessValidationException;
import com.b4rrhh.employee.presence.application.usecase.CreatePresenceCommand;
import com.b4rrhh.employee.presence.application.usecase.CreatePresenceUseCase;
import com.b4rrhh.employee.presence.application.usecase.ListEmployeePresencesUseCase;
import com.b4rrhh.employee.presence.domain.model.Presence;
import com.b4rrhh.employee.working_time.application.usecase.CreateWorkingTimeUseCase;
import com.b4rrhh.employee.working_time.application.usecase.ListEmployeeWorkingTimesCommand;
import com.b4rrhh.employee.working_time.application.usecase.ListEmployeeWorkingTimesUseCase;
import com.b4rrhh.employee.working_time.domain.exception.InvalidWorkingTimePercentageException;
import com.b4rrhh.employee.working_time.domain.model.WorkingTime;
import com.b4rrhh.employee.working_time.domain.model.WorkingTimeDerivedHours;
import com.b4rrhh.employee.workcenter.application.usecase.CreateWorkCenterCommand;
import com.b4rrhh.employee.workcenter.application.usecase.CreateWorkCenterUseCase;
import com.b4rrhh.employee.workcenter.application.usecase.ListEmployeeWorkCentersUseCase;
import com.b4rrhh.employee.workcenter.domain.model.WorkCenter;
import com.b4rrhh.employee.workcenter.domain.port.WorkCenterCompanyLookupPort;
import com.b4rrhh.employee.workcenter.domain.service.WorkCenterCompanyValidator;
import com.b4rrhh.rulesystem.domain.model.RuleEntity;
import com.b4rrhh.rulesystem.domain.port.RuleEntityRepository;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.flyway.enabled=false"
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({
        EmployeePersistenceAdapter.class,
        RehireEmployeeService.class,
        RehireEmployeeServiceRollbackIntegrationTest.RehireEmployeeRollbackTestConfig.class
})
// Estos tests levantan su propio esquema a mano en @BeforeEach, y hasta ahora
// lo hacian contra H2. El DDL es el mismo; el motor no. Comprobar una
// restriccion de integridad en una base que no es la de produccion solo
// demuestra que H2 la respeta.
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = TestPostgresInitializer.class)
class RehireEmployeeServiceRollbackIntegrationTest {

    @Autowired
    private RehireEmployeeService service;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpSchema() {
        jdbcTemplate.execute("create schema if not exists employee");
        jdbcTemplate.execute("drop table if exists employee.employee");

        jdbcTemplate.execute("""
                create table employee.employee (
                    id bigint generated by default as identity primary key,
                    rule_system_code varchar(5) not null,
                    employee_type_code varchar(30) not null,
                    employee_number varchar(15) not null,
                    first_name varchar(100) not null,
                    last_name_1 varchar(100) not null,
                    last_name_2 varchar(100),
                    preferred_name varchar(300),
                    status varchar(30) not null,
                    created_at timestamp not null,
                    updated_at timestamp not null,
                    photo_url varchar(512),
                    constraint uk_employee_business_key unique (rule_system_code, employee_type_code, employee_number)
                )
                """);

        jdbcTemplate.update(
                "insert into employee.employee (rule_system_code, employee_type_code, employee_number, first_name, last_name_1, status, created_at, updated_at) values (?, ?, ?, ?, ?, ?, current_timestamp, current_timestamp)",
                "ESP",
                "INTERNAL",
                "EMP001",
                "Ana",
                "Lopez",
                "TERMINATED"
        );
    }

    @Test
    void rollsBackEmployeeStatusWhenWorkingTimeCreationFailsAfterRehire() {
        RehireEmployeeCommand command = new RehireEmployeeCommand(
                "ESP",
                "INTERNAL",
                "EMP001",
                LocalDate.of(2026, 4, 15),
                "REHIRE",
                "ES01",
                "METAL",
                "OFICIAL_1",
                "CON",
                "SUB",
                "MADRID_01",
                null,
                new RehireEmployeeCommand.RehireEmployeeWorkingTimeCommand(new java.math.BigDecimal("0"))
        );

            assertThrows(RehireEmployeeBusinessValidationException.class, () -> service.rehire(command));

        String persistedStatus = jdbcTemplate.queryForObject(
                "select status from employee.employee where rule_system_code = ? and employee_type_code = ? and employee_number = ?",
                String.class,
                "ESP",
                "INTERNAL",
                "EMP001"
        );

        assertEquals("TERMINATED", persistedStatus);
    }

    @TestConfiguration
    static class RehireEmployeeRollbackTestConfig {

        @Bean
        RuleEntityRepository ruleEntityRepository() {
            return new RuleEntityRepository() {
                @Override
                public List<RuleEntity> findAll() {
                    return List.of();
                }

                @Override
                public List<RuleEntity> findByFilters(String ruleSystemCode, String ruleEntityTypeCode, String code, Boolean active, LocalDate referenceDate) {
                    return List.of();
                }

                @Override
                public Optional<RuleEntity> findApplicableByBusinessKey(String ruleSystemCode, String ruleEntityTypeCode, String code, LocalDate referenceDate) {
                    return Optional.empty();
                }

                @Override
                public Optional<RuleEntity> findByBusinessKey(String ruleSystemCode, String ruleEntityTypeCode, String code) {
                    return Optional.of(new RuleEntity(
                            1L, ruleSystemCode, ruleEntityTypeCode, code,
                            code, null, true,
                            LocalDate.of(1900, 1, 1), null,
                            LocalDateTime.now(), LocalDateTime.now()
                    ));
                }

                @Override
                public Optional<RuleEntity> findByBusinessKeyAndStartDate(String ruleSystemCode, String ruleEntityTypeCode, String code, LocalDate startDate) {
                    return Optional.empty();
                }

                @Override
                public boolean existsOverlapExcludingStartDate(String ruleSystemCode, String ruleEntityTypeCode, String code, LocalDate projectedStartDate, LocalDate projectedEndDate, LocalDate excludedStartDate) {
                    return false;
                }

                @Override
                public void deleteByBusinessKeyAndStartDate(String ruleSystemCode, String ruleEntityTypeCode, String code, LocalDate startDate) {
                }

                @Override
                public RuleEntity save(RuleEntity ruleEntity) {
                    return ruleEntity;
                }
            };
        }

        @Bean
        EmployeeTypeCatalogValidator employeeTypeCatalogValidator(RuleEntityRepository ruleEntityRepository) {
            return new EmployeeTypeCatalogValidator(ruleEntityRepository);
        }

        @Bean
        GetEmployeeByBusinessKeyUseCase getEmployeeByBusinessKeyUseCase(EmployeeRepository employeeRepository) {
            return (ruleSystemCode, employeeTypeCode, employeeNumber) -> employeeRepository
                    .findByRuleSystemCodeAndEmployeeTypeCodeAndEmployeeNumber(
                            ruleSystemCode,
                            employeeTypeCode,
                            employeeNumber
                    );
        }

        @Bean
        ListEmployeePresencesUseCase listEmployeePresencesUseCase() {
            return (ruleSystemCode, employeeTypeCode, employeeNumber) -> List.of(new Presence(
                    10L,
                    100L,
                    1,
                    "ES01",
                    "HIRE",
                    "VOL",
                    LocalDate.of(2026, 1, 1),
                    LocalDate.of(2026, 3, 31),
                    LocalDateTime.now(),
                    LocalDateTime.now()
            ));
        }

        @Bean
        ListEmployeeContractsUseCase listEmployeeContractsUseCase() {
            return (ListEmployeeContractsCommand command) -> List.of(new Contract(
                    100L,
                    "CON",
                    "SUB",
                    LocalDate.of(2026, 1, 1),
                    LocalDate.of(2026, 3, 31)
            ));
        }

        @Bean
        ListEmployeeLaborClassificationsUseCase listEmployeeLaborClassificationsUseCase() {
            return (ListEmployeeLaborClassificationsCommand command) -> List.of(new LaborClassification(
                    100L,
                    "METAL",
                    "OFICIAL_1",
                    LocalDate.of(2026, 1, 1),
                    LocalDate.of(2026, 3, 31)
            ));
        }

        @Bean
        ListEmployeeWorkCentersUseCase listEmployeeWorkCentersUseCase() {
            return (ruleSystemCode, employeeTypeCode, employeeNumber) -> List.of(new WorkCenter(
                    20L,
                    100L,
                    1,
                    "MADRID_01",
                    LocalDate.of(2026, 1, 1),
                    LocalDate.of(2026, 3, 31),
                    LocalDateTime.now(),
                    LocalDateTime.now()
            ));
        }

                @Bean
                ListEmployeeWorkingTimesUseCase listEmployeeWorkingTimesUseCase() {
                    return (ListEmployeeWorkingTimesCommand command) -> List.of(WorkingTime.rehydrate(
                        30L,
                        100L,
                        1,
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 3, 31),
                        new java.math.BigDecimal("100"),
                        new WorkingTimeDerivedHours(
                            new java.math.BigDecimal("40.00"),
                            new java.math.BigDecimal("8.00"),
                            new java.math.BigDecimal("166.67")
                        ),
                        LocalDateTime.now(),
                        LocalDateTime.now()
                    ));
                }

        @Bean
        CreatePresenceUseCase createPresenceUseCase() {
            return command -> new Presence(
                    11L,
                    100L,
                    2,
                    command.companyCode(),
                    command.entryReasonCode(),
                    command.exitReasonCode(),
                    command.startDate(),
                    command.endDate(),
                    LocalDateTime.now(),
                    LocalDateTime.now()
            );
        }

        @Bean
        CreateLaborClassificationUseCase createLaborClassificationUseCase() {
            return command -> new LaborClassification(
                    100L,
                    command.agreementCode(),
                    command.agreementCategoryCode(),
                    command.startDate(),
                    command.endDate()
            );
        }

        @Bean
        CreateContractUseCase createContractUseCase() {
            return command -> new Contract(
                    100L,
                    command.contractCode(),
                    command.contractSubtypeCode(),
                    command.startDate(),
                    command.endDate()
            );
        }

        @Bean
        WorkCenterCompanyLookupPort workCenterCompanyLookupPort() {
            return (ruleSystemCode, workCenterCode, referenceDate) -> java.util.Optional.of("ES01");
        }

        @Bean
        WorkCenterCompanyValidator workCenterCompanyValidator(WorkCenterCompanyLookupPort workCenterCompanyLookupPort) {
            return new WorkCenterCompanyValidator(workCenterCompanyLookupPort);
        }

        @Bean
        CreateWorkCenterUseCase createWorkCenterUseCase() {
            return command -> new WorkCenter(
                    21L,
                    100L,
                    2,
                    command.workCenterCode(),
                    command.startDate(),
                    command.endDate(),
                    LocalDateTime.now(),
                    LocalDateTime.now()
            );
        }

        @Bean
        CreateCostCenterDistributionUseCase createCostCenterDistributionUseCase() {
            return command -> {
                throw new UnsupportedOperationException("Not expected in this rollback test");
            };
        }

        @Bean
        CreateWorkingTimeUseCase createWorkingTimeUseCase() {
            return command -> {
                throw new InvalidWorkingTimePercentageException(
                        "workingTimePercentage must be greater than 0 and less than or equal to 100"
                );
            };
        }
    }
}