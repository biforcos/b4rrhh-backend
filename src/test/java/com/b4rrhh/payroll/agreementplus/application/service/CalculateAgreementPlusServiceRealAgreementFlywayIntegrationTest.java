package com.b4rrhh.payroll.agreementplus.application.service;

import com.b4rrhh.support.EsquemaRealInitializer;

import com.b4rrhh.employee.shared.infrastructure.persistence.EmployeeBusinessKeyLookupSupport;
import com.b4rrhh.employee.working_time.infrastructure.persistence.EmployeeAgreementContextLookupAdapter;
import com.b4rrhh.payroll.basesalary.infrastructure.persistence.EmployeeAgreementCategoryLookupAdapter;
import com.b4rrhh.payroll.basesalary.infrastructure.persistence.EmployeeByBusinessKeyLookupAdapter;
import com.b4rrhh.payroll.basesalary.infrastructure.persistence.PayrollObjectActivationLookupAdapter;
import com.b4rrhh.payroll.basesalary.infrastructure.persistence.PayrollObjectBindingLookupAdapter;
import com.b4rrhh.payroll.basesalary.infrastructure.persistence.PayrollTableRowLookupAdapter;
import com.b4rrhh.payroll.domain.model.PayrollConceptNotApplicableException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.flyway.enabled=false"
})
// El esquema no lo declara el test: es el de produccion, aplicado por Flyway
// una vez y clonado para este contexto (ver EsquemaReal). Las semillas del
// convenio real (V61, V68-V70) vienen incluidas; antes este test montaba su
// propia base con un subconjunto congelado de migraciones.
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = EsquemaRealInitializer.class)
@Import({
        CalculateAgreementPlusService.class,
        PayrollObjectBindingLookupAdapter.class,
        PayrollTableRowLookupAdapter.class,
        PayrollObjectActivationLookupAdapter.class,
        EmployeeAgreementCategoryLookupAdapter.class,
        EmployeeByBusinessKeyLookupAdapter.class,
        EmployeeAgreementContextLookupAdapter.class,
        EmployeeBusinessKeyLookupSupport.class
})
class CalculateAgreementPlusServiceRealAgreementFlywayIntegrationTest {

    private static final String RULE_SYSTEM_CODE = "ESP";
    private static final String EMPLOYEE_TYPE_CODE = "INTERNAL";
    private static final String REAL_AGREEMENT_CODE = "99002405011982";
    private static int empCounter = 0;

    @Autowired
    private CalculateAgreementPlusService service;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void calculateAgreementPlusResolvesMonthlyValueForGrupoII() throws PayrollConceptNotApplicableException {
        LocalDate effectiveDate = LocalDate.of(2026, 1, 15);
        String employeeNumber = "EMP" + (empCounter++);

        long employeeId = insertEmployee(employeeNumber);
        insertPresence(employeeId, effectiveDate);
        insertLaborClassification(employeeId, "99002405-G2", effectiveDate);

        BigDecimal result = service.calculateAgreementPlus(
                RULE_SYSTEM_CODE,
                EMPLOYEE_TYPE_CODE,
                employeeNumber,
                effectiveDate
        );

        assertNotNull(result);
        assertEquals(0, new BigDecimal("180.00").compareTo(result),
                "Grupo II agreement plus should be 180.00");
    }

    @Test
    void calculateAgreementPlusResolvesCorrectAmountForAllThreeGroups() throws PayrollConceptNotApplicableException {
        LocalDate effectiveDate = LocalDate.of(2026, 1, 15);

        // Grupo I
        {
            String employeeNumber = "EMP" + (empCounter++);
            long employeeId = insertEmployee(employeeNumber);
            insertPresence(employeeId, effectiveDate);
            insertLaborClassification(employeeId, "99002405-G1", effectiveDate);

            BigDecimal result = service.calculateAgreementPlus(
                    RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, employeeNumber, effectiveDate);
            assertEquals(0, new BigDecimal("250.00").compareTo(result),
                    "Grupo I agreement plus should be 250.00");
        }

        // Grupo III
        {
            String employeeNumber = "EMP" + (empCounter++);
            long employeeId = insertEmployee(employeeNumber);
            insertPresence(employeeId, effectiveDate);
            insertLaborClassification(employeeId, "99002405-G3", effectiveDate);

            BigDecimal result = service.calculateAgreementPlus(
                    RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, employeeNumber, effectiveDate);
            assertEquals(0, new BigDecimal("120.00").compareTo(result),
                    "Grupo III agreement plus should be 120.00");
        }
    }

    @Test
    void calculateAgreementPlusFailsWhenNoLaborClassificationExists() {
        LocalDate effectiveDate = LocalDate.of(2026, 1, 15);
        String employeeNumber = "EMP" + (empCounter++);

        insertEmployee(employeeNumber);
        // No labor classification inserted

        assertThrows(
                IllegalStateException.class,
                () -> service.calculateAgreementPlus(
                        RULE_SYSTEM_CODE,
                        EMPLOYEE_TYPE_CODE,
                        employeeNumber,
                        effectiveDate
                ),
                "Should fail when no labor classification exists"
        );
    }

    @Test
    void calculateAgreementPlusFailsWhenPlusConvenioActivationIsInactive() {
        LocalDate effectiveDate = LocalDate.of(2026, 1, 15);
        String employeeNumber = "EMP" + (empCounter++);

        long employeeId = insertEmployee(employeeNumber);
        insertPresence(employeeId, effectiveDate);
        insertLaborClassification(employeeId, "99002405-G2", effectiveDate);
        deactivatePlusConvenioActivation();

        assertThrows(
                PayrollConceptNotApplicableException.class,
                () -> service.calculateAgreementPlus(
                        RULE_SYSTEM_CODE,
                        EMPLOYEE_TYPE_CODE,
                        employeeNumber,
                        effectiveDate
                ),
                "Should fail when PLUS_CONVENIO activation is inactive"
        );
    }

    // -------------------------------------------------------
    // Helpers
    // -------------------------------------------------------

    private long insertEmployee(String employeeNumber) {
        jdbcTemplate.update(
                """
                insert into employee.employee (
                    rule_system_code,
                    employee_type_code,
                    employee_number,
                    first_name,
                    last_name_1,
                    status,
                    created_at,
                    updated_at
                ) values (?, ?, ?, ?, ?, ?, current_timestamp, current_timestamp)
                """,
                RULE_SYSTEM_CODE,
                EMPLOYEE_TYPE_CODE,
                employeeNumber,
                "Test",
                "AgreementPlus",
                "ACTIVE"
        );

        return jdbcTemplate.queryForObject(
                "select id from employee.employee where rule_system_code = ? and employee_type_code = ? and employee_number = ?",
                Long.class,
                RULE_SYSTEM_CODE,
                EMPLOYEE_TYPE_CODE,
                employeeNumber
        );
    }

    private void insertPresence(long employeeId, LocalDate effectiveDate) {
        jdbcTemplate.update(
                """
                insert into employee.presence (
                    employee_id,
                    presence_number,
                    company_code,
                    entry_reason_code,
                    start_date,
                    end_date,
                    created_at,
                    updated_at
                ) values (?, ?, ?, ?, ?, ?, current_timestamp, current_timestamp)
                """,
                employeeId,
                1,
                "ES01",
                "HIRE",
                effectiveDate,
                null
        );
    }

    private void insertLaborClassification(long employeeId, String categoryCode, LocalDate startDate) {
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
                ) values (?, ?, ?, ?, ?, current_timestamp, current_timestamp)
                """,
                employeeId,
                REAL_AGREEMENT_CODE,
                categoryCode,
                startDate,
                null
        );
    }

    private void deactivatePlusConvenioActivation() {
        jdbcTemplate.update(
                """
                update payroll.payroll_object_activation
                   set active = false,
                       updated_at = current_timestamp
                 where rule_system_code = ?
                   and owner_type_code = 'AGREEMENT'
                   and owner_code = ?
                   and target_object_type_code = 'PAYROLL_CONCEPT'
                   and target_object_code = 'PLUS_CONVENIO'
                """,
                RULE_SYSTEM_CODE,
                REAL_AGREEMENT_CODE
        );
    }
}
