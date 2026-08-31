package com.b4rrhh.employee.journey.infrastructure.persistence;

import com.b4rrhh.support.TestSobreEsquemaReal;
import com.b4rrhh.employee.journey.application.port.JourneyContractReadPort;
import com.b4rrhh.employee.journey.application.port.JourneyContractRecord;
import com.b4rrhh.employee.journey.application.port.JourneyCostCenterReadPort;
import com.b4rrhh.employee.journey.application.port.JourneyCostCenterRecord;
import com.b4rrhh.employee.journey.application.port.JourneyLaborClassificationReadPort;
import com.b4rrhh.employee.journey.application.port.JourneyLaborClassificationRecord;
import com.b4rrhh.employee.journey.application.port.JourneyPresenceReadPort;
import com.b4rrhh.employee.journey.application.port.JourneyPresenceRecord;
import com.b4rrhh.employee.journey.application.port.JourneyWorkCenterReadPort;
import com.b4rrhh.employee.journey.application.port.JourneyWorkCenterRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestSobreEsquemaReal
// El esquema es el de produccion: el empleado existe de verdad, su id lo
// asigna la base ('generated always') y la presencia exige presence_number y
// entry_reason_code, que el DDL a mano no declaraba.
class JourneyReadPersistenceAdapterIntegrationTest {

    @Autowired
    private JourneyPresenceReadPort journeyPresenceReadPort;

    @Autowired
    private JourneyContractReadPort journeyContractReadPort;

    @Autowired
    private JourneyLaborClassificationReadPort journeyLaborClassificationReadPort;

    @Autowired
    private JourneyWorkCenterReadPort journeyWorkCenterReadPort;

    @Autowired
    private JourneyCostCenterReadPort journeyCostCenterReadPort;

    @Autowired
    private EmployeeJourneyLookupAdapter employeeJourneyLookupAdapter;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long empleado;

    @BeforeEach
    void setUpDatos() {
        empleado = jdbcTemplate.queryForObject("""
                insert into employee.employee (
                    rule_system_code, employee_type_code, employee_number,
                    first_name, last_name_1, preferred_name
                ) values ('ESP', 'INTERNAL', 'EMP001', 'LIDIA', 'MORALES', 'Lidia Morales')
                returning id
                """, Long.class);
    }

    @Test
    void readQueriesReturnOrderedRecordsByStartDateForAllTracks() {
        insertPresence(empleado, 2, "C002", LocalDate.of(2026, 2, 1), null);
        insertPresence(empleado, 1, "C001", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

        insertContract(empleado, "TMP", "PT1", LocalDate.of(2026, 2, 1), null);
        insertContract(empleado, "IND", "FT1", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

        insertLaborClassification(empleado, "AGR_B", "CAT_B", LocalDate.of(2026, 2, 1), null);
        insertLaborClassification(empleado, "AGR_A", "CAT_A", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

        insertWorkCenter(empleado, 2, "BCN", LocalDate.of(2026, 2, 1), null);
        insertWorkCenter(empleado, 1, "MAD", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

        insertCostCenter(empleado, "CC20", new BigDecimal("40.00"), LocalDate.of(2026, 2, 1), null);
        insertCostCenter(empleado, "CC10", new BigDecimal("60.00"), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

        List<JourneyPresenceRecord> presences = journeyPresenceReadPort.findByEmployeeIdOrderByStartDate(empleado);
        List<JourneyContractRecord> contracts = journeyContractReadPort.findByEmployeeIdOrderByStartDate(empleado);
        List<JourneyLaborClassificationRecord> laborClassifications = journeyLaborClassificationReadPort.findByEmployeeIdOrderByStartDate(empleado);
        List<JourneyWorkCenterRecord> workCenters = journeyWorkCenterReadPort.findByEmployeeIdOrderByStartDate(empleado);
        List<JourneyCostCenterRecord> costCenters = journeyCostCenterReadPort.findByEmployeeIdOrderByStartDate(empleado);

        assertEquals(LocalDate.of(2026, 1, 1), presences.get(0).startDate());
        assertEquals(LocalDate.of(2026, 1, 1), contracts.get(0).startDate());
        assertEquals(LocalDate.of(2026, 1, 1), laborClassifications.get(0).startDate());
        assertEquals(LocalDate.of(2026, 1, 1), workCenters.get(0).startDate());
        assertEquals(LocalDate.of(2026, 1, 1), costCenters.get(0).startDate());
    }

    @Test
    void costCenterQueryReturnsMultipleItemsForSameEmployee() {
        insertCostCenter(empleado, "CC10", new BigDecimal("50.00"), LocalDate.of(2026, 1, 1), null);
        insertCostCenter(empleado, "CC20", new BigDecimal("50.00"), LocalDate.of(2026, 1, 1), null);

        List<JourneyCostCenterRecord> costCenters = journeyCostCenterReadPort.findByEmployeeIdOrderByStartDate(empleado);

        assertEquals(2, costCenters.size());
        assertEquals("CC10", costCenters.get(0).costCenterCode());
        assertEquals("CC20", costCenters.get(1).costCenterCode());
    }

    @Test
    void employeeLookupResolvesBusinessKeyAndDisplayName() {
        var employee = employeeJourneyLookupAdapter.findByBusinessKey("ESP", "INTERNAL", "EMP001").orElseThrow();

        assertEquals(empleado, employee.employeeId());
        assertEquals("ESP", employee.ruleSystemCode());
        assertEquals("INTERNAL", employee.employeeTypeCode());
        assertEquals("EMP001", employee.employeeNumber());
        assertEquals("Lidia Morales", employee.displayName());
    }

    private void insertPresence(
            Long employeeId,
            Integer presenceNumber,
            String companyCode,
            LocalDate startDate,
            LocalDate endDate
    ) {
        jdbcTemplate.update(
                "insert into employee.presence (employee_id, presence_number, company_code, entry_reason_code, exit_reason_code, start_date, end_date) values (?, ?, ?, ?, ?, ?, ?)",
                employeeId,
                presenceNumber,
                companyCode,
                "ENTRY",
                null,
                startDate,
                endDate
        );
    }

    private void insertContract(
            Long employeeId,
            String contractCode,
            String contractSubtypeCode,
            LocalDate startDate,
            LocalDate endDate
    ) {
        jdbcTemplate.update(
                "insert into employee.contract (employee_id, contract_code, contract_subtype_code, start_date, end_date) values (?, ?, ?, ?, ?)",
                employeeId,
                contractCode,
                contractSubtypeCode,
                startDate,
                endDate
        );
    }

    private void insertLaborClassification(
            Long employeeId,
            String agreementCode,
            String agreementCategoryCode,
            LocalDate startDate,
            LocalDate endDate
    ) {
        jdbcTemplate.update(
                "insert into employee.labor_classification (employee_id, agreement_code, agreement_category_code, start_date, end_date) values (?, ?, ?, ?, ?)",
                employeeId,
                agreementCode,
                agreementCategoryCode,
                startDate,
                endDate
        );
    }

    private void insertWorkCenter(
            Long employeeId,
            Integer assignmentNumber,
            String workCenterCode,
            LocalDate startDate,
            LocalDate endDate
    ) {
        jdbcTemplate.update(
                "insert into employee.work_center (employee_id, work_center_assignment_number, work_center_code, start_date, end_date) values (?, ?, ?, ?, ?)",
                employeeId,
                assignmentNumber,
                workCenterCode,
                startDate,
                endDate
        );
    }

    private void insertCostCenter(
            Long employeeId,
            String costCenterCode,
            BigDecimal allocationPercentage,
            LocalDate startDate,
            LocalDate endDate
    ) {
        jdbcTemplate.update(
                "insert into employee.cost_center (employee_id, cost_center_code, allocation_percentage, start_date, end_date) values (?, ?, ?, ?, ?)",
                employeeId,
                costCenterCode,
                allocationPercentage,
                startDate,
                endDate
        );
    }
}
