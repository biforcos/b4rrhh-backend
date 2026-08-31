package com.b4rrhh.payroll.basesalary.infrastructure.persistence;

import com.b4rrhh.support.TestSobreEsquemaReal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestSobreEsquemaReal
// El esquema real trae sembrados bindings y filas de tabla para el convenio
// 99002405011982 (V66/V67/V69/V70); los datos de estos tests usan owners y
// tablas propios (A1..A3, SB_1) que no chocan con esa semilla.
class BaseSalaryPersistenceLookupsTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PayrollObjectBindingLookupAdapter bindingLookupAdapter;

    @Autowired
    private PayrollTableRowLookupAdapter tableRowLookupAdapter;

    @Test
    void bindingLookupFiltersByActiveAndBoundObjectTypeTable() {
        jdbcTemplate.update(
                """
                insert into payroll.payroll_object_binding (
                    rule_system_code, owner_type_code, owner_code, binding_role_code,
                    bound_object_type_code, bound_object_code, active
                ) values (?, ?, ?, ?, ?, ?, ?)
                """,
                "ESP", "AGREEMENT", "A1", "BASE_SALARY_TABLE", "TABLE", "SB_A1", true
        );
        jdbcTemplate.update(
                """
                insert into payroll.payroll_object_binding (
                    rule_system_code, owner_type_code, owner_code, binding_role_code,
                    bound_object_type_code, bound_object_code, active
                ) values (?, ?, ?, ?, ?, ?, ?)
                """,
                "ESP", "AGREEMENT", "A2", "BASE_SALARY_TABLE", "TABLE", "SB_A2", false
        );
        jdbcTemplate.update(
                """
                insert into payroll.payroll_object_binding (
                    rule_system_code, owner_type_code, owner_code, binding_role_code,
                    bound_object_type_code, bound_object_code, active
                ) values (?, ?, ?, ?, ?, ?, ?)
                """,
                "ESP", "AGREEMENT", "A3", "BASE_SALARY_TABLE", "PAYROLL_CONCEPT", "SB_A3", true
        );

        assertEquals(
                "SB_A1",
                bindingLookupAdapter.resolveBoundObjectCode("ESP", "AGREEMENT", "A1", "BASE_SALARY_TABLE").orElseThrow()
        );
        assertTrue(bindingLookupAdapter.resolveBoundObjectCode("ESP", "AGREEMENT", "A2", "BASE_SALARY_TABLE").isEmpty());
        assertTrue(bindingLookupAdapter.resolveBoundObjectCode("ESP", "AGREEMENT", "A3", "BASE_SALARY_TABLE").isEmpty());
    }

    @Test
    void tableRowLookupReturnsLatestValidRowForEffectiveDate() {
        jdbcTemplate.update(
                """
                insert into payroll.payroll_table_row (
                    rule_system_code, table_code, search_code, start_date, end_date, monthly_value, active
                ) values (?, ?, ?, ?, ?, ?, ?)
                """,
                "ESP", "SB_1", "C1", LocalDate.of(2024, 1, 1), null, new BigDecimal("1200.00"), true
        );
        jdbcTemplate.update(
                """
                insert into payroll.payroll_table_row (
                    rule_system_code, table_code, search_code, start_date, end_date, monthly_value, active
                ) values (?, ?, ?, ?, ?, ?, ?)
                """,
                "ESP", "SB_1", "C1", LocalDate.of(2025, 1, 1), null, new BigDecimal("1400.00"), true
        );
        jdbcTemplate.update(
                """
                insert into payroll.payroll_table_row (
                    rule_system_code, table_code, search_code, start_date, end_date, monthly_value, active
                ) values (?, ?, ?, ?, ?, ?, ?)
                """,
                "ESP", "SB_1", "C1", LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31), new BigDecimal("999.00"), true
        );

        BigDecimal resolved = tableRowLookupAdapter.resolveMonthlyValue(
                "ESP",
                "SB_1",
                "C1",
                LocalDate.of(2026, 1, 15)
        ).orElseThrow();

        assertEquals(0, new BigDecimal("1400.00").compareTo(resolved));
    }
}
