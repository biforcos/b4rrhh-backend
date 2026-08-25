package com.b4rrhh.payroll.basesalary.application.service;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.flyway.enabled=false"
})
// El esquema no lo declara el test: es el de produccion, aplicado por Flyway
// una vez y clonado para este contexto (ver EsquemaReal). Las semillas del
// convenio real (V61, V65-V67) vienen incluidas; antes este test montaba su
// propia base con un subconjunto congelado de migraciones.
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = EsquemaRealInitializer.class)
@Import({
        CalculateBaseSalaryService.class,
        PayrollObjectBindingLookupAdapter.class,
        PayrollTableRowLookupAdapter.class,
        PayrollObjectActivationLookupAdapter.class,
        EmployeeAgreementCategoryLookupAdapter.class,
        EmployeeByBusinessKeyLookupAdapter.class,
        EmployeeAgreementContextLookupAdapter.class,
        EmployeeBusinessKeyLookupSupport.class
})
class CalculateBaseSalaryServiceRealAgreementFlywayIntegrationTest {

    private static final String RULE_SYSTEM_CODE = "ESP";
    private static final String EMPLOYEE_TYPE_CODE = "INTERNAL";
    private static final String REAL_AGREEMENT_CODE = "99002405011982";
    private static int empCounter = 0;

    @Autowired
    private CalculateBaseSalaryService service;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void calculateBaseSalaryResolvesMonthlyValueFromRealAgreementTableRow() throws PayrollConceptNotApplicableException {
        LocalDate effectiveDate = LocalDate.of(2026, 1, 15);
        String employeeNumber = "EMP" + (empCounter++);

        long employeeId = insertEmployee(employeeNumber);
        insertPresence(employeeId, effectiveDate);
        insertLaborClassification(employeeId, "99002405-G2", effectiveDate);

        BigDecimal result = service.calculateBaseSalary(
                RULE_SYSTEM_CODE,
                EMPLOYEE_TYPE_CODE,
                employeeNumber,
                effectiveDate
        );

        assertNotNull(result);
        assertEquals(0, new BigDecimal("1425.00").compareTo(result), 
                "Grupo II base salary should be 1425.00");
    }

    @Test
    void calculateBaseSalaryForDifferentCategories() throws PayrollConceptNotApplicableException {
        LocalDate effectiveDate = LocalDate.of(2026, 1, 15);

        // Category G1
        {
            String employeeNumber = "EMP" + (empCounter++);
            long employeeId = insertEmployee(employeeNumber);
            insertPresence(employeeId, effectiveDate);
            insertLaborClassification(employeeId, "99002405-G1", effectiveDate);

            BigDecimal result = service.calculateBaseSalary(
                    RULE_SYSTEM_CODE,
                    EMPLOYEE_TYPE_CODE,
                    employeeNumber,
                    effectiveDate
            );
            assertEquals(0, new BigDecimal("1850.00").compareTo(result));
        }

        // Category G3
        {
            String employeeNumber = "EMP" + (empCounter++);
            long employeeId = insertEmployee(employeeNumber);
            insertPresence(employeeId, effectiveDate);
            insertLaborClassification(employeeId, "99002405-G3", effectiveDate);

            BigDecimal result = service.calculateBaseSalary(
                    RULE_SYSTEM_CODE,
                    EMPLOYEE_TYPE_CODE,
                    employeeNumber,
                    effectiveDate
            );
            assertEquals(0, new BigDecimal("1200.00").compareTo(result));
        }
    }

    @Test
    void calculateBaseSalaryFailsWhenNoLaborClassificationExists() {
        LocalDate effectiveDate = LocalDate.of(2026, 1, 15);
        String employeeNumber = "EMP" + (empCounter++);

        insertEmployee(employeeNumber);
        // No labor classification inserted

        assertThrows(
                IllegalStateException.class,
                () -> service.calculateBaseSalary(
                        RULE_SYSTEM_CODE,
                        EMPLOYEE_TYPE_CODE,
                        employeeNumber,
                        effectiveDate
                ),
                "Should fail when no classification exists"
        );
    }

            @Test
            void calculateBaseSalaryFailsWhenBaseSalaryActivationIsInactive() {
            LocalDate effectiveDate = LocalDate.of(2026, 1, 15);
            String employeeNumber = "EMP" + (empCounter++);

            long employeeId = insertEmployee(employeeNumber);
            insertPresence(employeeId, effectiveDate);
            insertLaborClassification(employeeId, "99002405-G2", effectiveDate);
            deactivateBaseSalaryActivation();

            assertThrows(
                PayrollConceptNotApplicableException.class,
                () -> service.calculateBaseSalary(
                    RULE_SYSTEM_CODE,
                    EMPLOYEE_TYPE_CODE,
                    employeeNumber,
                    effectiveDate
                ),
                "Should fail when BASE_SALARY activation is inactive"
            );
            }

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
                "BaseSalary",
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

    private void deactivateBaseSalaryActivation() {
        jdbcTemplate.update(
                """
                update payroll.payroll_object_activation
                   set active = false,
                       updated_at = current_timestamp
                 where rule_system_code = ?
                   and owner_type_code = 'AGREEMENT'
                   and owner_code = ?
                   and target_object_type_code = 'PAYROLL_CONCEPT'
                   and target_object_code = 'BASE_SALARY'
                """,
                RULE_SYSTEM_CODE,
                REAL_AGREEMENT_CODE
        );
    }
}
