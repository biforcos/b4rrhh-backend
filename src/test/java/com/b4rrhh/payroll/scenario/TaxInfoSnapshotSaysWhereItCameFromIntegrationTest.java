package com.b4rrhh.payroll.scenario;

import com.b4rrhh.payroll.application.usecase.LaunchPayrollCalculationCommand;
import com.b4rrhh.payroll.application.usecase.LaunchPayrollCalculationUseCase;
import com.b4rrhh.payroll.application.usecase.PayrollLaunchEmployeeTarget;
import com.b4rrhh.payroll.application.usecase.PayrollLaunchTargetSelection;
import com.b4rrhh.payroll.application.usecase.PayrollLaunchTargetSelectionType;
import com.b4rrhh.support.TestWebSobreEsquemaReal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * La foto fiscal del recibo dice si la situacion se declaro o se supuso (`backend#92`).
 *
 * <p>Hasta este issue, un empleado sin declaracion se calculaba con la situacion por omision
 * —soltero, sin descendientes, territorio comun— y esa situacion se escribia en
 * `payroll.payroll_context_snapshot` con exactamente la misma forma que una leida. Los 873
 * recibos de la demo estan en ese caso, porque `employee.employee_tax_information` esta vacia.
 *
 * <p>Dos empleados y no uno, y la declaracion del segundo dice **lo mismo** que el valor por
 * omision: con una sola situacion no se distingue nada, y con dos que difieren en los datos
 * tampoco se prueba que lo que los separa sea la procedencia (la leccion del `backend#91`).
 */
@TestWebSobreEsquemaReal
class TaxInfoSnapshotSaysWhereItCameFromIntegrationTest {

    private static final String RULE_SYSTEM = "TST";
    private static final LocalDate ALTA = LocalDate.of(2025, 1, 1);

    private String sinDeclaracion;
    private String conDeclaracionIgualAlDefecto;
    private PayrollScenarioFixtures fixtures;

    @Autowired
    private LaunchPayrollCalculationUseCase launchPayrollCalculationUseCase;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void setUpData() {
        fixtures = new PayrollScenarioFixtures(jdbc);
        fixtures.seedConceptGraph(RULE_SYSTEM);

        sinDeclaracion = altaDe("SIN");
        conDeclaracionIgualAlDefecto = altaDe("CON");

        long conId = jdbc.queryForObject(
                "select id from employee.employee where rule_system_code = ? and employee_number = ?",
                Long.class, RULE_SYSTEM, conDeclaracionIgualAlDefecto);
        fixtures.insertTaxInformation(conId, ALTA, "SINGLE_OR_OTHER", 0, "COMUN");
    }

    @Test
    void theSnapshotTellsADeclaredSituationApartFromAnAssumedOne() {
        calcular(sinDeclaracion);
        calcular(conDeclaracionIgualAlDefecto);

        assertEquals("DEFAULT_NO_DECLARATION", fuenteFiscalDe(sinDeclaracion));
        assertEquals("DECLARED", fuenteFiscalDe(conDeclaracionIgualAlDefecto));

        // Y el resto del payload es identico en los dos: si el `source` no estuviera, los dos
        // recibos afirmarian lo mismo y uno de los dos estaria afirmando algo que nadie declaro.
        assertEquals(
                situacionFiscalDe(sinDeclaracion),
                situacionFiscalDe(conDeclaracionIgualAlDefecto));
    }

    private String altaDe(String sufijo) {
        String numero = "TX" + sufijo + (System.nanoTime() % 100_000_000L);
        long employeeId = fixtures.insertEmployee(RULE_SYSTEM, "INTERNAL", numero);
        fixtures.insertPresence(employeeId, 1, ALTA, null);
        fixtures.insertLaborClassification(employeeId, ALTA);
        fixtures.insertWorkingTime(employeeId, new BigDecimal("100.00"), ALTA, null);
        return numero;
    }

    private void calcular(String employeeNumber) {
        var run = launchPayrollCalculationUseCase.launch(new LaunchPayrollCalculationCommand(
                RULE_SYSTEM,
                "202501",
                "NORMAL",
                "ENGINE",
                "1.0",
                new PayrollLaunchTargetSelection(
                        PayrollLaunchTargetSelectionType.SINGLE_EMPLOYEE,
                        new PayrollLaunchEmployeeTarget("INTERNAL", employeeNumber),
                        null
                ),
                null
        ));
        assertEquals(1, run.totalCalculated(), employeeNumber + " tenia que calcularse");
    }

    private String fuenteFiscalDe(String employeeNumber) {
        String fuente = jdbc.queryForObject(
                "select snapshot_payload_json ->> 'source'"
                        + "  from payroll.payroll_context_snapshot s"
                        + "  join payroll.payroll p on p.id = s.payroll_id"
                        + " where p.rule_system_code = ? and p.employee_number = ?"
                        + "   and s.snapshot_type_code = 'EMPLOYEE_TAX_INFORMATION'",
                String.class, RULE_SYSTEM, employeeNumber);
        assertNotNull(fuente, employeeNumber + ": la foto fiscal tiene que decir de donde sale");
        return fuente;
    }

    /** Los ocho campos de la situacion, sin el `source`: lo que los dos recibos comparten. */
    private String situacionFiscalDe(String employeeNumber) {
        return jdbc.queryForObject(
                "select (snapshot_payload_json::jsonb - 'source')::text"
                        + "  from payroll.payroll_context_snapshot s"
                        + "  join payroll.payroll p on p.id = s.payroll_id"
                        + " where p.rule_system_code = ? and p.employee_number = ?"
                        + "   and s.snapshot_type_code = 'EMPLOYEE_TAX_INFORMATION'",
                String.class, RULE_SYSTEM, employeeNumber);
    }
}
