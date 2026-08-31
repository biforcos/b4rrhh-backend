package com.b4rrhh.employee.labor_classification.application.usecase;

import com.b4rrhh.support.DatosDePrueba;
import com.b4rrhh.support.EsquemaRealInitializer;
import com.b4rrhh.employee.labor_classification.application.command.ReplaceLaborClassificationFromDateCommand;
import com.b4rrhh.employee.labor_classification.application.port.EmployeeLaborClassificationContext;
import com.b4rrhh.employee.labor_classification.application.port.EmployeeLaborClassificationLookupPort;
import com.b4rrhh.employee.labor_classification.application.service.AgreementCategoryRelationValidator;
import com.b4rrhh.employee.labor_classification.application.service.LaborClassificationCatalogValidator;
import com.b4rrhh.employee.labor_classification.application.service.LaborClassificationPresenceCoverageValidator;
import com.b4rrhh.employee.labor_classification.domain.model.LaborClassification;
import com.b4rrhh.employee.labor_classification.infrastructure.persistence.LaborClassificationEntity;
import com.b4rrhh.employee.labor_classification.infrastructure.persistence.LaborClassificationPersistenceAdapter;
import com.b4rrhh.employee.labor_classification.infrastructure.persistence.SpringDataLaborClassificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.ContextConfiguration;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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
        LaborClassificationPersistenceAdapter.class,
        ReplaceLaborClassificationFromDateService.class,
        ReplaceLaborClassificationFromDateServiceIntegrationTest.ReplaceLaborClassificationTestConfig.class
})
// El esquema no lo declara el test: es el de produccion, aplicado por Flyway
// una vez y clonado para este contexto (ver EsquemaReal). La @TestConfiguration
// anidada impide compartir el contexto de TestSobreEsquemaReal, asi que este
// test lleva su propio initializer.
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = EsquemaRealInitializer.class)
class ReplaceLaborClassificationFromDateServiceIntegrationTest {

    private static final String RULE_SYSTEM_CODE = "ESP";
    private static final String EMPLOYEE_TYPE_CODE = "INTERNAL";
    private static final String EMPLOYEE_NUMBER = "EMP001";

    // El id lo asigna la base ('generated always') y cambia en cada test; el
    // stub del lookup port lo lee de aqui. Static porque la @TestConfiguration
    // crea el bean una sola vez por contexto.
    private static Long employeeId;

    @Autowired
    private ReplaceLaborClassificationFromDateService service;

    @Autowired
    private SpringDataLaborClassificationRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpDatos() {
        employeeId = DatosDePrueba.empleado(jdbcTemplate);

        insertLaborClassification(
                employeeId,
                "AGR_OFFICE",
                "CAT_ADMIN",
                LocalDate.of(2026, 1, 1),
                null
        );
    }

    @Test
    void splitReplacementUpdatesExistingAndInsertsNewPeriodInOrder() {
        LaborClassification replaced = service.replaceFromDate(new ReplaceLaborClassificationFromDateCommand(
                RULE_SYSTEM_CODE,
                EMPLOYEE_TYPE_CODE,
                EMPLOYEE_NUMBER,
                LocalDate.of(2026, 3, 1),
                "AGR_TECH",
                "CAT_TECH_1"
        ));

        assertEquals(LocalDate.of(2026, 3, 1), replaced.getStartDate());
        assertNull(replaced.getEndDate());

        List<LaborClassificationEntity> rows = repository.findByEmployeeIdOrderByStartDateAsc(employeeId);
        assertEquals(2, rows.size());

        assertEquals(LocalDate.of(2026, 1, 1), rows.get(0).getStartDate());
        assertEquals(LocalDate.of(2026, 2, 28), rows.get(0).getEndDate());
        assertEquals("AGR_OFFICE", rows.get(0).getAgreementCode());
        assertEquals("CAT_ADMIN", rows.get(0).getAgreementCategoryCode());

        assertEquals(LocalDate.of(2026, 3, 1), rows.get(1).getStartDate());
        assertNull(rows.get(1).getEndDate());
        assertEquals("AGR_TECH", rows.get(1).getAgreementCode());
        assertEquals("CAT_TECH_1", rows.get(1).getAgreementCategoryCode());
    }

    @Test
    void replaceAtExactStartDateUpdatesSingleExistingRowWithoutDuplicate() {
        jdbcTemplate.update("delete from employee.labor_classification where employee_id = ?", employeeId);
        insertLaborClassification(
                employeeId,
                "AGR_OFFICE",
                "CAT_ADMIN",
                LocalDate.of(2026, 3, 1),
                null
        );

        LaborClassification replaced = service.replaceFromDate(new ReplaceLaborClassificationFromDateCommand(
                RULE_SYSTEM_CODE,
                EMPLOYEE_TYPE_CODE,
                EMPLOYEE_NUMBER,
                LocalDate.of(2026, 3, 1),
                "AGR_TECH",
                "CAT_TECH_1"
        ));

        assertEquals(LocalDate.of(2026, 3, 1), replaced.getStartDate());

        List<LaborClassificationEntity> rows = repository.findByEmployeeIdOrderByStartDateAsc(employeeId);
        assertEquals(1, rows.size());
        assertEquals(LocalDate.of(2026, 3, 1), rows.get(0).getStartDate());
        assertEquals("AGR_TECH", rows.get(0).getAgreementCode());
        assertEquals("CAT_TECH_1", rows.get(0).getAgreementCategoryCode());
    }

    // Este test comprueba que el servicio deshace SU transaccion cuando el
    // insert falla. Para verlo hay que mirar desde fuera, y @DataJpaTest lo
    // envuelve todo en una transaccion propia: dentro de ella el servicio no
    // tiene transaccion que deshacer, y en Postgres la consulta posterior ni
    // siquiera se ejecuta porque la transaccion quedo abortada por el fallo.
    // Con NOT_SUPPORTED el test no abre transaccion: la preparacion se
    // confirma, el servicio gestiona la suya, y las consultas ven lo que de
    // verdad quedo grabado. En H2 pasaba por casualidad, no por diseno.
    //
    // No hace falta limpiar despues: cada @BeforeEach crea un empleado nuevo
    // con numero unico (DatosDePrueba), y las filas que este test deja quedan
    // colgadas de un empleado que nadie mas consulta.
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void replaceIsAtomicWhenInsertFailsAfterUpdate() {
        String tooLongAgreementCode = "AGR_CODE_LONGER_THAN_THIRTY_CHARS";

        assertThrows(
                DataIntegrityViolationException.class,
                () -> service.replaceFromDate(new ReplaceLaborClassificationFromDateCommand(
                        RULE_SYSTEM_CODE,
                        EMPLOYEE_TYPE_CODE,
                        EMPLOYEE_NUMBER,
                        LocalDate.of(2026, 3, 1),
                        tooLongAgreementCode,
                        "CAT_TECH_1"
                ))
        );

            Long rowCount = jdbcTemplate.queryForObject(
                "select count(*) from employee.labor_classification where employee_id = ?",
                Long.class,
                employeeId
            );
            String agreementCode = jdbcTemplate.queryForObject(
                "select agreement_code from employee.labor_classification where employee_id = ? and start_date = ?",
                String.class,
                employeeId,
                LocalDate.of(2026, 1, 1)
            );
            String agreementCategoryCode = jdbcTemplate.queryForObject(
                "select agreement_category_code from employee.labor_classification where employee_id = ? and start_date = ?",
                String.class,
                employeeId,
                LocalDate.of(2026, 1, 1)
            );
            Date endDate = jdbcTemplate.queryForObject(
                "select end_date from employee.labor_classification where employee_id = ? and start_date = ?",
                Date.class,
                employeeId,
                LocalDate.of(2026, 1, 1)
            );

            assertEquals(1L, rowCount);
            assertEquals("AGR_OFFICE", agreementCode);
            assertEquals("CAT_ADMIN", agreementCategoryCode);
            assertNull(endDate);
    }

    private void insertLaborClassification(
            Long employeeId,
            String agreementCode,
            String agreementCategoryCode,
            LocalDate startDate,
            LocalDate endDate
    ) {
        jdbcTemplate.update(
                """
                insert into employee.labor_classification (
                    employee_id,
                    agreement_code,
                    agreement_category_code,
                    start_date,
                    end_date,
                    created_at,
                    updated_at
                ) values (?,?,?,?,?,?,?)
                """,
                employeeId,
                agreementCode,
                agreementCategoryCode,
                startDate,
                endDate,
                java.sql.Timestamp.valueOf(LocalDateTime.now()),
                java.sql.Timestamp.valueOf(LocalDateTime.now())
        );
    }

    @TestConfiguration
    static class ReplaceLaborClassificationTestConfig {

        @Bean
        EmployeeLaborClassificationLookupPort employeeLaborClassificationLookupPort() {
            return new EmployeeLaborClassificationLookupPort() {
                @Override
                public Optional<EmployeeLaborClassificationContext> findByBusinessKey(
                        String ruleSystemCode,
                        String employeeTypeCode,
                        String employeeNumber
                ) {
                    return findByBusinessKeyForUpdate(ruleSystemCode, employeeTypeCode, employeeNumber);
                }

                @Override
                public Optional<EmployeeLaborClassificationContext> findByBusinessKeyForUpdate(
                        String ruleSystemCode,
                        String employeeTypeCode,
                        String employeeNumber
                ) {
                    return Optional.of(new EmployeeLaborClassificationContext(
                            employeeId,
                            RULE_SYSTEM_CODE,
                            EMPLOYEE_TYPE_CODE,
                            EMPLOYEE_NUMBER
                    ));
                }
            };
        }

        @Bean
        LaborClassificationCatalogValidator laborClassificationCatalogValidator() {
            return new LaborClassificationCatalogValidator(null) {
                @Override
                public String normalizeRequiredCode(String fieldName, String value) {
                    if (value == null || value.trim().isEmpty()) {
                        throw new IllegalArgumentException(fieldName + " is required");
                    }

                    return value.trim().toUpperCase();
                }

                @Override
                public void validateAgreementCode(String ruleSystemCode, String agreementCode, LocalDate referenceDate) {
                    // Always valid in this integration test.
                }

                @Override
                public void validateAgreementCategoryCode(
                        String ruleSystemCode,
                        String agreementCategoryCode,
                        LocalDate referenceDate
                ) {
                    // Always valid in this integration test.
                }
            };
        }

        @Bean
        AgreementCategoryRelationValidator agreementCategoryRelationValidator() {
            return new AgreementCategoryRelationValidator(null) {
                @Override
                public void validateAgreementCategoryRelation(
                        String ruleSystemCode,
                        String agreementCode,
                        String agreementCategoryCode,
                        LocalDate referenceDate
                ) {
                    // Always valid in this integration test.
                }
            };
        }

        @Bean
        LaborClassificationPresenceCoverageValidator laborClassificationPresenceCoverageValidator() {
            return new LaborClassificationPresenceCoverageValidator(null) {
                @Override
                public void validateFullCoverage(
                        Long employeeId,
                        List<LaborClassification> projectedLaborClassificationHistory,
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
