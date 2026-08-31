package com.b4rrhh.employee.employee.infrastructure.persistence;

import com.b4rrhh.support.TestSobreEsquemaReal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestSobreEsquemaReal
// El esquema es el de produccion: rule_system_code tiene clave ajena a
// rulesystem.rule_system, asi que los sistemas inventados (RS001/RS002) se
// sustituyen por los sembrados FRA y PRT. Las migraciones no siembran ningun
// empleado, y estos tests van en transaccion: cuentan solo lo suyo.
class SpringDataEmployeeRepositoryIntegrationTest {

    @Autowired
    private SpringDataEmployeeRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void executesLikeFiltersAcrossAllExpectedFields() {
        Long employeeId = insertEmployee(
                "FRA",
                "TYPE_A",
                "NUM123",
                "NOMBREX",
                "APELLIDOY",
                "APELLIDOZ",
                "PREFW",
                "ACTIVE"
        );

        insertWorkCenter(employeeId, 1, "MADRID_HQ", LocalDate.now().minusDays(30), null);

        insertEmployee(
                "PRT",
                "TYPE_B",
                "NUM999",
                "OTRO",
                "EMPLEADO",
                null,
                null,
                "INACTIVE"
        );

        assertSingleMatchForQ("NUM123", "NUM123");
        assertSingleMatchForQ("FRA", "NUM123");
        assertSingleMatchForQ("TYPE_A", "NUM123");
        assertSingleMatchForQ("NOMBREX", "NUM123");
        assertSingleMatchForQ("APELLIDOY", "NUM123");
        assertSingleMatchForQ("APELLIDOZ", "NUM123");
        assertSingleMatchForQ("PREFW", "NUM123");
        assertSingleMatchForQ("NOMBREX APELLIDOY APELLIDOZ", "NUM123");
    }

    @Test
    void supportsNullQClauseAndReturnsActiveWorkCenter() {
        Long employeeId = insertEmployee(
                "ESP",
                "INTERNAL",
                "EMP001",
                "LIDIA",
                "MORALES",
                null,
                null,
                "ACTIVE"
        );

        insertWorkCenter(employeeId, 1, "OLD_CENTER", LocalDate.now().minusDays(40), LocalDate.now().minusDays(10));
        insertWorkCenter(employeeId, 2, "MADRID_HQ", LocalDate.now().minusDays(5), null);

        Page<EmployeeDirectoryProjection> result = repository.findDirectoryByFilters(
                null,
                "ESP",
                "INTERNAL",
                "ACTIVE",
                LocalDate.now(),
                PageRequest.of(0, 20)
        );

        assertEquals(1, result.getNumberOfElements());
        assertEquals("EMP001", result.getContent().get(0).employeeNumber());
        assertEquals("MADRID_HQ", result.getContent().get(0).workCenterCode());
    }

    // backend#18: el total cuenta los que cumplen el filtro, no los de la página. Los tres casos
    // del issue, con 45 empleados propios en vez de los 250 de la semilla de la demo, que no
    // viene en las migraciones.
    @Test
    void totalCountsEveryMatchAndNotJustThePage() {
        for (int i = 1; i <= 45; i++) {
            insertEmployee("ESP", "INTERNAL", String.format("EMP%03d", i), "NOMBRE", "APELLIDO", null, null, "ACTIVE");
        }

        Page<EmployeeDirectoryProjection> nobody = repository.findDirectoryByFilters(
                "NADIE-SE-LLAMA-ASI", null, null, null, LocalDate.now(), PageRequest.of(0, 20)
        );
        assertEquals(0, nobody.getTotalElements());
        assertTrue(nobody.getContent().isEmpty());

        Page<EmployeeDirectoryProjection> everyone = repository.findDirectoryByFilters(
                null, null, null, null, LocalDate.now(), PageRequest.of(0, 20)
        );
        assertEquals(45, everyone.getTotalElements());
        assertEquals(20, everyone.getNumberOfElements());

        Page<EmployeeDirectoryProjection> fourthPage = repository.findDirectoryByFilters(
                null, null, null, null, LocalDate.now(), PageRequest.of(3, 10)
        );
        assertEquals(45, fourthPage.getTotalElements());
        assertEquals(10, fourthPage.getNumberOfElements());
        assertEquals("EMP031", fourthPage.getContent().get(0).employeeNumber());
    }

    @Test
    void totalCarriesTheSameFiltersAsThePage() {
        insertEmployee("ESP", "INTERNAL", "EMP001", "LIDIA", "MORALES", null, null, "ACTIVE");
        insertEmployee("ESP", "INTERNAL", "EMP002", "LIDIA", "GARCIA", null, null, "INACTIVE");
        insertEmployee("ESP", "INTERNAL", "EMP003", "MARTA", "MORALES", null, null, "ACTIVE");

        Page<EmployeeDirectoryProjection> result = repository.findDirectoryByFilters(
                "LIDIA", null, null, "ACTIVE", LocalDate.now(), PageRequest.of(0, 1)
        );

        // Una fila por página, pero el total es el de quienes son LIDIA y ACTIVE: una, no tres.
        assertEquals(1, result.getTotalElements());
        assertEquals("EMP001", result.getContent().get(0).employeeNumber());
    }

    private void assertSingleMatchForQ(String q, String expectedEmployeeNumber) {
        Page<EmployeeDirectoryProjection> result = repository.findDirectoryByFilters(
                q,
                null,
                null,
                null,
                LocalDate.now(),
                PageRequest.of(0, 20)
        );

        assertFalse(result.isEmpty());
        assertEquals(1, result.getTotalElements());
        assertEquals(expectedEmployeeNumber, result.getContent().get(0).employeeNumber());
    }

    private Long insertEmployee(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber,
            String firstName,
            String lastName1,
            String lastName2,
            String preferredName,
            String status
    ) {
        jdbcTemplate.update(
                """
                        insert into employee.employee (
                            rule_system_code,
                            employee_type_code,
                            employee_number,
                            first_name,
                            last_name_1,
                            last_name_2,
                            preferred_name,
                            status,
                            created_at,
                            updated_at
                        ) values (?, ?, ?, ?, ?, ?, ?, ?, current_timestamp, current_timestamp)
                        """,
                ruleSystemCode,
                employeeTypeCode,
                employeeNumber,
                firstName,
                lastName1,
                lastName2,
                preferredName,
                status
        );

        return jdbcTemplate.queryForObject(
                "select id from employee.employee where rule_system_code = ? and employee_type_code = ? and employee_number = ?",
                Long.class,
                ruleSystemCode,
                employeeTypeCode,
                employeeNumber
        );
    }

    private void insertWorkCenter(
            Long employeeId,
            int assignmentNumber,
            String workCenterCode,
            LocalDate startDate,
            LocalDate endDate
    ) {
        jdbcTemplate.update(
                """
                        insert into employee.work_center (
                            employee_id,
                            work_center_assignment_number,
                            work_center_code,
                            start_date,
                            end_date,
                            created_at,
                            updated_at
                        ) values (?, ?, ?, ?, ?, current_timestamp, current_timestamp)
                        """,
                employeeId,
                assignmentNumber,
                workCenterCode,
                startDate,
                endDate
        );
    }
}