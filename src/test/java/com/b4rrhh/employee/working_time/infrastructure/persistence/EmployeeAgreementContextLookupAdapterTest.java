package com.b4rrhh.employee.working_time.infrastructure.persistence;

import com.b4rrhh.employee.employee.infrastructure.persistence.EmployeeEntity;
import com.b4rrhh.employee.labor_classification.infrastructure.persistence.LaborClassificationEntity;
import com.b4rrhh.employee.working_time.application.port.EmployeeAgreementContext;
import com.b4rrhh.support.TestSobreEsquemaReal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@TestSobreEsquemaReal
// Antes esto corria contra H2 con un DDL a mano; el esquema es ahora el real.
// Las migraciones no siembran empleados y el test va en transaccion, asi que
// los numeros EMPxxx no chocan con nada.
class EmployeeAgreementContextLookupAdapterTest {

    private static final String RULE_SYSTEM_CODE = "ESP";

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private EmployeeAgreementContextLookupAdapter adapter;

    @Test
    void resolveContextReturnsCorrectAgreementContextAtEffectiveDate() {
        EmployeeEntity employee = employeeEntity(RULE_SYSTEM_CODE, "INTERNAL", "EMP001");
        entityManager.persist(employee);
        entityManager.flush();

        LaborClassificationEntity classification = laborClassification(
                employee.getId(), "99002405011982", "99002405-G2",
                LocalDate.of(2024, 1, 1), null);
        entityManager.persist(classification);
        entityManager.flush();

        EmployeeAgreementContext result = adapter.resolveContext(employee.getId(), LocalDate.of(2026, 1, 10));

        assertEquals(RULE_SYSTEM_CODE, result.ruleSystemCode());
        assertEquals("99002405011982", result.agreementCode());
    }

    @Test
        void resolveContextPicksLatestValidClassificationWhenMoreThanOneRecordMatchesDate() {
        EmployeeEntity employee = employeeEntity(RULE_SYSTEM_CODE, "INTERNAL", "EMP002");
        entityManager.persist(employee);
        entityManager.flush();

        LaborClassificationEntity initial = laborClassification(
            employee.getId(), "99002405011981", "99002405-G1",
            LocalDate.of(2024, 1, 1), null);
        LaborClassificationEntity replacement = laborClassification(
                employee.getId(), "99002405011982", "99002405-G1",
            LocalDate.of(2024, 6, 1), null);
        entityManager.persist(initial);
        entityManager.persist(replacement);
        entityManager.flush();

        EmployeeAgreementContext result = adapter.resolveContext(employee.getId(), LocalDate.of(2024, 7, 1));

        assertEquals("99002405011982", result.agreementCode());
    }

    @Test
    void resolveContextThrowsWhenNoClassificationExistsAtDate() {
        EmployeeEntity employee = employeeEntity(RULE_SYSTEM_CODE, "INTERNAL", "EMP003");
        entityManager.persist(employee);
        entityManager.flush();

        LaborClassificationEntity classification = laborClassification(
                employee.getId(), "99002405011982", "99002405-G2",
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));
        entityManager.persist(classification);
        entityManager.flush();

        // Effective date is after the classification end_date
        assertThrows(IllegalStateException.class,
                () -> adapter.resolveContext(employee.getId(), LocalDate.of(2026, 1, 10)),
                "Should throw when no valid labor classification found at effective date");
    }

    @Test
    void resolveContextThrowsWhenEmployeeHasNoClassificationsAtAll() {
        EmployeeEntity employee = employeeEntity(RULE_SYSTEM_CODE, "INTERNAL", "EMP004");
        entityManager.persist(employee);
        entityManager.flush();

        assertThrows(IllegalStateException.class,
                () -> adapter.resolveContext(employee.getId(), LocalDate.of(2026, 1, 10)),
                "Should throw when employee has no labor classifications");
    }

    private EmployeeEntity employeeEntity(String ruleSystemCode, String employeeTypeCode, String employeeNumber) {
        EmployeeEntity entity = new EmployeeEntity();
        entity.setRuleSystemCode(ruleSystemCode);
        entity.setEmployeeTypeCode(employeeTypeCode);
        entity.setEmployeeNumber(employeeNumber);
        entity.setFirstName("Test");
        entity.setLastName1("Employee");
        entity.setStatus("ACTIVE");
        return entity;
    }

    private LaborClassificationEntity laborClassification(Long employeeId, String agreementCode,
                                                           String agreementCategoryCode,
                                                           LocalDate startDate, LocalDate endDate) {
        LaborClassificationEntity entity = new LaborClassificationEntity();
        entity.setEmployeeId(employeeId);
        entity.setAgreementCode(agreementCode);
        entity.setAgreementCategoryCode(agreementCategoryCode);
        entity.setStartDate(startDate);
        entity.setEndDate(endDate);
        return entity;
    }
}
