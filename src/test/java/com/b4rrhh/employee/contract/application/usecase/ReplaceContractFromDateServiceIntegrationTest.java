package com.b4rrhh.employee.contract.application.usecase;

import com.b4rrhh.support.DatosDePrueba;
import com.b4rrhh.support.EsquemaRealInitializer;
import com.b4rrhh.employee.contract.application.command.ReplaceContractFromDateCommand;
import com.b4rrhh.employee.contract.application.port.EmployeeContractContext;
import com.b4rrhh.employee.contract.application.port.EmployeeContractLookupPort;
import com.b4rrhh.employee.contract.application.service.ContractSubtypeRelationValidator;
import com.b4rrhh.employee.contract.application.service.ContractCatalogValidator;
import com.b4rrhh.employee.contract.application.service.ContractPresenceCoverageValidator;
import com.b4rrhh.employee.contract.domain.exception.ContractInvalidException;
import com.b4rrhh.employee.contract.domain.model.Contract;
import com.b4rrhh.employee.contract.infrastructure.persistence.ContractEntity;
import com.b4rrhh.employee.contract.infrastructure.persistence.ContractPersistenceAdapter;
import com.b4rrhh.employee.contract.infrastructure.persistence.SpringDataContractRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.ContextConfiguration;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.flyway.enabled=false"
})
@Import({
        ContractPersistenceAdapter.class,
        ReplaceContractFromDateService.class,
        ReplaceContractFromDateServiceIntegrationTest.ReplaceContractTestConfig.class
})
// El esquema no lo declara el test: es el de produccion, aplicado por Flyway
// una vez y clonado para este contexto (ver EsquemaReal). La @TestConfiguration
// anidada impide compartir el contexto de TestSobreEsquemaReal, asi que este
// test lleva su propio initializer.
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = EsquemaRealInitializer.class)
class ReplaceContractFromDateServiceIntegrationTest {

    private static final String RULE_SYSTEM_CODE = "ESP";
    private static final String EMPLOYEE_TYPE_CODE = "INTERNAL";
    private static final String EMPLOYEE_NUMBER = "EMP001";

    // El id lo asigna la base ('generated always') y cambia en cada test; el
    // stub del lookup port lo lee de aqui. Static porque la @TestConfiguration
    // crea el bean una sola vez por contexto.
    private static Long employeeId;

    @Autowired
    private ReplaceContractFromDateService service;

    @Autowired
    private SpringDataContractRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpDatos() {
        employeeId = DatosDePrueba.empleado(jdbcTemplate);

        insertContract(
                employeeId,
                "IND",
                "FT1",
                LocalDate.of(2026, 1, 1),
                null
        );
    }

    @Test
    void splitReplacementUpdatesExistingAndInsertsNewPeriodInOrder() {
        Contract replaced = service.replaceFromDate(new ReplaceContractFromDateCommand(
                RULE_SYSTEM_CODE,
                EMPLOYEE_TYPE_CODE,
                EMPLOYEE_NUMBER,
                LocalDate.of(2026, 3, 1),
                "TMP",
                "PT1"
        ));

        assertEquals(LocalDate.of(2026, 3, 1), replaced.getStartDate());
        assertNull(replaced.getEndDate());

        List<ContractEntity> rows = repository.findByEmployeeIdOrderByStartDateAsc(employeeId);
        assertEquals(2, rows.size());

        assertEquals(LocalDate.of(2026, 1, 1), rows.get(0).getStartDate());
        assertEquals(LocalDate.of(2026, 2, 28), rows.get(0).getEndDate());
        assertEquals("IND", rows.get(0).getContractCode());
        assertEquals("FT1", rows.get(0).getContractSubtypeCode());

        assertEquals(LocalDate.of(2026, 3, 1), rows.get(1).getStartDate());
        assertNull(rows.get(1).getEndDate());
        assertEquals("TMP", rows.get(1).getContractCode());
        assertEquals("PT1", rows.get(1).getContractSubtypeCode());
    }

    @Test
    void replaceAtExactStartDateUpdatesSingleExistingRowWithoutDuplicate() {
        jdbcTemplate.update("delete from employee.contract where employee_id = ?", employeeId);
        insertContract(
                employeeId,
                "IND",
                "FT1",
                LocalDate.of(2026, 3, 1),
                null
        );

        Contract replaced = service.replaceFromDate(new ReplaceContractFromDateCommand(
                RULE_SYSTEM_CODE,
                EMPLOYEE_TYPE_CODE,
                EMPLOYEE_NUMBER,
                LocalDate.of(2026, 3, 1),
                "TMP",
                "PT1"
        ));

        assertEquals(LocalDate.of(2026, 3, 1), replaced.getStartDate());

        List<ContractEntity> rows = repository.findByEmployeeIdOrderByStartDateAsc(employeeId);
        assertEquals(1, rows.size());
        assertEquals(LocalDate.of(2026, 3, 1), rows.get(0).getStartDate());
        assertEquals("TMP", rows.get(0).getContractCode());
        assertEquals("PT1", rows.get(0).getContractSubtypeCode());
    }

    @Test
        void replaceKeepsOriginalStateWhenIncomingCodeLengthIsInvalid() {
        String invalidLengthContractCode = "LONG";

        assertThrows(
            ContractInvalidException.class,
                () -> service.replaceFromDate(new ReplaceContractFromDateCommand(
                        RULE_SYSTEM_CODE,
                        EMPLOYEE_TYPE_CODE,
                        EMPLOYEE_NUMBER,
                        LocalDate.of(2026, 3, 1),
                invalidLengthContractCode,
                        "PT1"
                ))
        );

            Long rowCount = jdbcTemplate.queryForObject(
                "select count(*) from employee.contract where employee_id = ?",
                Long.class,
                employeeId
            );
            String contractCode = jdbcTemplate.queryForObject(
                "select contract_code from employee.contract where employee_id = ? and start_date = ?",
                String.class,
                employeeId,
                LocalDate.of(2026, 1, 1)
            );
            String contractSubtypeCode = jdbcTemplate.queryForObject(
                "select contract_subtype_code from employee.contract where employee_id = ? and start_date = ?",
                String.class,
                employeeId,
                LocalDate.of(2026, 1, 1)
            );
            Date endDate = jdbcTemplate.queryForObject(
                "select end_date from employee.contract where employee_id = ? and start_date = ?",
                Date.class,
                employeeId,
                LocalDate.of(2026, 1, 1)
            );

            assertEquals(1L, rowCount);
            assertEquals("IND", contractCode);
            assertEquals("FT1", contractSubtypeCode);
            assertNull(endDate);
    }

    private void insertContract(
            Long employeeId,
            String contractCode,
            String contractSubtypeCode,
            LocalDate startDate,
            LocalDate endDate
    ) {
        jdbcTemplate.update(
                """
                insert into employee.contract (
                    employee_id,
                    contract_code,
                    contract_subtype_code,
                    start_date,
                    end_date,
                    created_at,
                    updated_at
                ) values (?,?,?,?,?,?,?)
                """,
                employeeId,
                contractCode,
                contractSubtypeCode,
                startDate,
                endDate,
                java.sql.Timestamp.valueOf(LocalDateTime.now()),
                java.sql.Timestamp.valueOf(LocalDateTime.now())
        );
    }

    @TestConfiguration
    static class ReplaceContractTestConfig {

        @Bean
        EmployeeContractLookupPort employeeContractLookupPort() {
            return new EmployeeContractLookupPort() {
                @Override
                public Optional<EmployeeContractContext> findByBusinessKey(
                        String ruleSystemCode,
                        String employeeTypeCode,
                        String employeeNumber
                ) {
                    return findByBusinessKeyForUpdate(ruleSystemCode, employeeTypeCode, employeeNumber);
                }

                @Override
                public Optional<EmployeeContractContext> findByBusinessKeyForUpdate(
                        String ruleSystemCode,
                        String employeeTypeCode,
                        String employeeNumber
                ) {
                    return Optional.of(new EmployeeContractContext(
                            employeeId,
                            RULE_SYSTEM_CODE,
                            EMPLOYEE_TYPE_CODE,
                            EMPLOYEE_NUMBER
                    ));
                }
            };
        }

        @Bean
        ContractCatalogValidator contractCatalogValidator() {
            return new ContractCatalogValidator(null) {
                @Override
                public String normalizeRequiredCode(String fieldName, String value) {
                    if (value == null || value.trim().isEmpty()) {
                        throw new IllegalArgumentException(fieldName + " is required");
                    }

                    return value.trim().toUpperCase();
                }

                @Override
                public void validateContractCode(String ruleSystemCode, String contractCode, LocalDate referenceDate) {
                    // Always valid in this integration test.
                }

                @Override
                public void validateContractSubtypeCode(
                        String ruleSystemCode,
                        String contractSubtypeCode,
                        LocalDate referenceDate
                ) {
                    // Always valid in this integration test.
                }
            };
        }

        @Bean
        ContractSubtypeRelationValidator contractSubtypeRelationValidator() {
            return new ContractSubtypeRelationValidator(null) {
                @Override
                public void validateContractSubtypeRelation(
                        String ruleSystemCode,
                        String contractCode,
                        String contractSubtypeCode,
                        LocalDate referenceDate
                ) {
                    // Always valid in this integration test.
                }
            };
        }

        @Bean
        ContractPresenceCoverageValidator contractPresenceCoverageValidator() {
            return new ContractPresenceCoverageValidator(null) {
                @Override
                public void validateFullCoverage(
                        Long employeeId,
                        List<Contract> projectedContractHistory,
                        String ruleSystemCode,
                        String employeeTypeCode,
                        String employeeNumber
                ) {
                    // No-op for persistence-focused integration scenarios.
                }
            };
        }
    }
}
