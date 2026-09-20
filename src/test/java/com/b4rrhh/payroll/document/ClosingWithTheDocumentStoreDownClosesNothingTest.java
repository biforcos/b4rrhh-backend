package com.b4rrhh.payroll.document;

import com.b4rrhh.payroll.application.usecase.BulkFinalizePayrollCommand;
import com.b4rrhh.payroll.application.usecase.BulkFinalizePayrollResult;
import com.b4rrhh.payroll.application.usecase.BulkFinalizePayrollUseCase;
import com.b4rrhh.payroll.application.usecase.FinalizePayrollCommand;
import com.b4rrhh.payroll.application.usecase.FinalizePayrollUseCase;
import com.b4rrhh.payroll.application.usecase.LaunchPayrollCalculationCommand;
import com.b4rrhh.payroll.application.usecase.LaunchPayrollCalculationUseCase;
import com.b4rrhh.payroll.application.usecase.PayrollLaunchEmployeeTarget;
import com.b4rrhh.payroll.application.usecase.PayrollLaunchTargetSelection;
import com.b4rrhh.payroll.application.usecase.PayrollLaunchTargetSelectionType;
import com.b4rrhh.payroll.document.domain.exception.PayslipDocumentStorageUnavailableException;
import com.b4rrhh.payroll.scenario.PayrollScenarioFixtures;
import com.b4rrhh.support.AlmacenDeDocumentosEnMemoria;
import com.b4rrhh.support.TestWebSobreEsquemaReal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cerrar con el almacen de documentos caido no cierra nada ({@code backend#113}).
 *
 * <h2>Que anade esto a lo que ya habia</h2>
 *
 * <p>El {@code backend#112} decidio que cerrar un recibo con el almacen inalcanzable <b>tiene que
 * fallar</b>, y lo probo contra el backend arrancado con MinIO parado. Contra el backend arrancado
 * y una sola vez: la suite no lo veia, porque su almacen es un mapa y un mapa no sabe caerse. Un
 * test que existe solo a mano es un test que se deja de correr el dia que se toca el cierre, que
 * es el unico dia en que hace falta.
 *
 * <p>{@code EveryPathToDefinitiveArchivesItsDocumentTest} ya garantiza que los dos caminos a
 * {@code DEFINITIVE} pasan por el documento. Esto garantiza lo otro: que los dos se <b>detienen</b>
 * cuando el documento no puede existir.
 *
 * <h2>Lo que se afirma, y por que no basta con la excepcion</h2>
 *
 * <p>Cada test afirma dos cosas, y la segunda es la que importa: que el cierre lanza lo que el
 * {@code #112} decidio, y que el recibo <b>sigue {@code CALCULATED}</b>. Un cierre que falla
 * despues de haber cambiado el estado es peor que uno que no falla, y con solo la excepcion el
 * test pasaria igual.
 *
 * <p>Y cada test se comprueba por el otro lado: con el interruptor apagado, el <b>mismo</b> cierre
 * sobre los <b>mismos</b> recibos pasa. Sin eso, un almacen que fallara siempre —o un cierre que
 * estuviera roto por cualquier otra razon— haria pasar la primera mitad sin probar nada.
 *
 * <p>Fuera de transaccion ({@code NOT_SUPPORTED}) por lo mismo que
 * {@code BulkCloseAndBulkInvalidateKnowEachOtherIntegrationTest}: lo que hay que afirmar es lo que
 * <b>quedo en la base</b>, no lo que la sesion tenia encolado. Aqui ademas es el centro del
 * asunto, porque el servicio de cierre es transaccional: si el test corriera dentro de una
 * transaccion suya, el estado del recibo lo diria el rollback del test y no el del cierre.
 *
 * <p>Cada test usa su propia reglamentacion, y no la {@code TST} que comparten los demas: lo que
 * aqui se confirma se queda en el clon, y el cierre en masa apunta a <b>todo el periodo</b>.
 */
@TestWebSobreEsquemaReal
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ClosingWithTheDocumentStoreDownClosesNothingTest {

    private static final String EMPLOYEE_TYPE = "INTERNAL";
    private static final String PERIOD = "202501";
    private static final String PAYROLL_TYPE = "NORMAL";
    private static final LocalDate PERIOD_START = LocalDate.of(2025, 1, 1);

    @Autowired
    private LaunchPayrollCalculationUseCase launchPayrollCalculationUseCase;

    @Autowired
    private FinalizePayrollUseCase finalizePayrollUseCase;

    @Autowired
    private BulkFinalizePayrollUseCase bulkFinalizePayrollUseCase;

    @Autowired
    private AlmacenDeDocumentosEnMemoria.AlmacenEnMemoria almacen;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private PayrollScenarioFixtures fixtures;
    private String ruleSystem;

    @BeforeEach
    void setUpData() {
        fixtures = new PayrollScenarioFixtures(jdbcTemplate);
    }

    /**
     * El almacen se levanta pase lo que pase.
     *
     * <p>El contexto es uno solo para toda la suite: un almacen que se quedara caido no romperia
     * este test —que ya habria terminado— sino el siguiente, y el mensaje hablaria de otra cosa.
     */
    @AfterEach
    void levantarElAlmacen() {
        almacen.levantar();
    }

    /** Criterios 2 y 4: cerrar uno. */
    @Test
    void closingOnePayslipStopsAtTheStoreAndOnlyGoesThroughWhenItIsBack() {
        usar("DWN");
        String empleado = hireAndCalculate();
        assertEquals("CALCULATED", statusOf(empleado), "el escenario necesita un recibo de partida");

        almacen.caer();

        PayslipDocumentStorageUnavailableException fallo = assertThrows(
                PayslipDocumentStorageUnavailableException.class,
                () -> finalizeOne(empleado),
                "cerrar sin poder emitir el documento tiene que fallar");
        assertTrue(fallo.getMessage().contains(claveDe(empleado, 1)),
                "el error dice de que documento habla: " + fallo.getMessage());

        assertEquals("CALCULATED", statusOf(empleado),
                "y el recibo se queda como estaba: no se cierra lo que no se puede entregar");
        assertFalta(claveDe(empleado, 1));

        // Por el otro lado: con el almacen de vuelta, el mismo cierre pasa. Sin esto, un almacen
        // que fallara siempre haria verde lo de arriba sin probar nada.
        almacen.levantar();
        finalizeOne(empleado);

        assertEquals("DEFINITIVE", statusOf(empleado));
        assertEstan(List.of(claveDe(empleado, 1)));
    }

    /** Criterios 3 y 4: cerrar en masa. Tres recibos, ninguno cerrado. */
    @Test
    void closingInBulkStopsAtTheStoreAndOnlyGoesThroughWhenItIsBack() {
        usar("DWB");
        List<String> empleados = List.of(hireAndCalculate(), hireAndCalculate(), hireAndCalculate());
        empleados.forEach(empleado -> assertEquals("CALCULATED", statusOf(empleado),
                "el escenario necesita tres recibos de partida"));

        almacen.caer();

        assertThrows(PayslipDocumentStorageUnavailableException.class, this::finalizeBulk,
                "la tanda entera se detiene en el primer documento que no puede existir");

        empleados.forEach(empleado -> assertEquals("CALCULATED", statusOf(empleado),
                "ninguno de los tres se cierra: la transaccion entera se deshace, y no queda"
                        + " media tanda ni un DEFINITIVE sin papel"));
        empleados.forEach(empleado -> assertFalta(claveDe(empleado, 1)));

        almacen.levantar();
        BulkFinalizePayrollResult cierre = finalizeBulk();

        assertEquals(3, cierre.totalFinalized(), "con el almacen en pie se cierran los tres");
        empleados.forEach(empleado -> assertEquals("DEFINITIVE", statusOf(empleado)));
        assertEstan(empleados.stream().map(empleado -> claveDe(empleado, 1)).toList());
    }

    // ---------------------------------------------------------------------

    private void usar(String ruleSystemCode) {
        ruleSystem = ruleSystemCode;
        Integer alreadySeeded = jdbcTemplate.queryForObject(
                "select count(*) from rulesystem.rule_system where code = ?", Integer.class, ruleSystem);
        if (alreadySeeded == 0) {
            fixtures.seedConceptGraph(ruleSystem);
        }
    }

    private String hireAndCalculate() {
        String employeeNumber = "DC" + (System.nanoTime() % 1_000_000_000L);
        long employeeId = fixtures.insertEmployee(ruleSystem, EMPLOYEE_TYPE, employeeNumber);
        fixtures.insertPresence(employeeId, 1, PERIOD_START, null);
        fixtures.insertLaborClassification(employeeId, PERIOD_START);
        fixtures.insertWorkingTime(employeeId, new BigDecimal("100.00"), PERIOD_START, null);

        var run = launchPayrollCalculationUseCase.launch(new LaunchPayrollCalculationCommand(
                ruleSystem, PERIOD, PAYROLL_TYPE, "ENGINE", "1.0",
                new PayrollLaunchTargetSelection(
                        PayrollLaunchTargetSelectionType.SINGLE_EMPLOYEE,
                        new PayrollLaunchEmployeeTarget(EMPLOYEE_TYPE, employeeNumber),
                        null),
                null));
        assertEquals(1, run.totalCalculated(), "el escenario necesita un recibo de partida");
        return employeeNumber;
    }

    private void finalizeOne(String employee) {
        finalizePayrollUseCase.finalizePayroll(new FinalizePayrollCommand(
                ruleSystem, EMPLOYEE_TYPE, employee, PERIOD, PAYROLL_TYPE, 1));
    }

    private BulkFinalizePayrollResult finalizeBulk() {
        return bulkFinalizePayrollUseCase.finalizeBulk(new BulkFinalizePayrollCommand(
                ruleSystem, PERIOD, PAYROLL_TYPE,
                new PayrollLaunchTargetSelection(
                        PayrollLaunchTargetSelectionType.ALL_EMPLOYEES_WITH_PRESENCE_IN_PERIOD,
                        null, null)));
    }

    /** La misma direccion que {@code PayslipDocumentKey}, escrita a mano para poder afirmarla. */
    private String claveDe(String employee, int presenceNumber) {
        return String.join("/", ruleSystem, PERIOD, PAYROLL_TYPE, EMPLOYEE_TYPE,
                employee + "-" + presenceNumber + ".pdf");
    }

    private void assertFalta(String clave) {
        assertTrue(!almacen.claves().contains(clave),
                "no se archiva el documento de un recibo que no se cierra. Lo que hay: "
                        + almacen.claves());
    }

    private void assertEstan(List<String> claves) {
        List<String> faltan = new ArrayList<>(claves);
        faltan.removeAll(almacen.claves());
        assertTrue(faltan.isEmpty(),
                "faltan documentos de recibos cerrados: " + faltan + ". Lo que hay: "
                        + almacen.claves());
    }

    private String statusOf(String employee) {
        return jdbcTemplate.queryForObject(
                "select status from payroll.payroll"
                        + " where rule_system_code = ? and employee_type_code = ? and employee_number = ?"
                        + "   and payroll_period_code = ? and payroll_type_code = ? and presence_number = ?",
                String.class, ruleSystem, EMPLOYEE_TYPE, employee, PERIOD, PAYROLL_TYPE, 1);
    }
}
