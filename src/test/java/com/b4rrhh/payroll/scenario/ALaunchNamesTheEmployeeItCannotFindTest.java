package com.b4rrhh.payroll.scenario;

import com.b4rrhh.payroll.application.usecase.LaunchPayrollCalculationCommand;
import com.b4rrhh.payroll.application.usecase.LaunchPayrollCalculationUseCase;
import com.b4rrhh.payroll.application.usecase.PayrollLaunchEmployeeTarget;
import com.b4rrhh.payroll.application.usecase.PayrollLaunchTargetSelection;
import com.b4rrhh.payroll.application.usecase.PayrollLaunchTargetSelectionType;
import com.b4rrhh.payroll.application.usecase.PayrollRetroRequest;
import com.b4rrhh.payroll.domain.exception.InvalidPayrollArgumentException;
import com.b4rrhh.support.TestWebSobreEsquemaReal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Un lanzamiento a un empleado que no existe <b>dice qué no ha encontrado</b> ({@code frontend#88}).
 *
 * <p>Visto en la demo del 27/09: en «Empleado único» se escribió {@code EMP} en el tipo —lo razonable,
 * los números empiezan por EMP— y el lanzamiento contestó «No relevant employee presence was found for
 * payroll launch target». El tipo era {@code INTERNAL}, y nada lo decía.
 *
 * <p>Se contesta al pedir, con un 400 y sin dejar ejecución, como cualquier otro encargo mal formado: un
 * empleado que no existe no es algo que se descubra calculando.
 */
@TestWebSobreEsquemaReal
class ALaunchNamesTheEmployeeItCannotFindTest {

    @Autowired private LaunchPayrollCalculationUseCase launch;
    @Autowired private JdbcTemplate jdbc;

    private PayrollScenarioFixtures fixtures;

    @BeforeEach
    void setUp() {
        fixtures = new PayrollScenarioFixtures(jdbc);
    }

    @Test
    void unTipoQueNoExisteSeNombraConLosQueHay() {
        InvalidPayrollArgumentException fallo = assertThrows(InvalidPayrollArgumentException.class,
                () -> launch.requestLaunch(single("EMP", "EMP000001")));

        assertTrue(fallo.getMessage().contains("EMP"), "nombra el tipo que no existe: " + fallo.getMessage());
        assertTrue(fallo.getMessage().contains("INTERNAL"),
                "y dice cuáles tiene el sistema de reglas: " + fallo.getMessage());
    }

    @Test
    void unNumeroQueNoExisteSeNombra() {
        String inexistente = "NOEXISTE" + (System.nanoTime() % 100_000);
        InvalidPayrollArgumentException fallo = assertThrows(InvalidPayrollArgumentException.class,
                () -> launch.requestLaunch(single("INTERNAL", inexistente)));

        assertTrue(fallo.getMessage().contains(inexistente),
                "nombra el empleado que no existe: " + fallo.getMessage());
    }

    @Test
    void unEmpleadoQueExisteSeLanzaComoSiempre() {
        String emp = "LN" + (System.nanoTime() % 1_000_000_000L);
        long empId = fixtures.insertEmployee("ESP", "INTERNAL", emp);
        fixtures.insertPresence(empId, 1, LocalDate.of(2025, 1, 1), null);
        fixtures.insertLaborClassification(empId, LocalDate.of(2025, 1, 1));
        fixtures.insertWorkingTime(empId, new BigDecimal("100.00"), LocalDate.of(2025, 1, 1), null);

        assertEquals("COMPLETED", launch.launch(single("INTERNAL", emp)).status());
    }

    private static LaunchPayrollCalculationCommand single(String tipo, String numero) {
        return new LaunchPayrollCalculationCommand(
                "ESP", "202509", "NORMAL", "ENGINE", "1.0",
                new PayrollLaunchTargetSelection(
                        PayrollLaunchTargetSelectionType.SINGLE_EMPLOYEE,
                        new PayrollLaunchEmployeeTarget(tipo, numero),
                        List.of()),
                null,
                PayrollRetroRequest.none());
    }
}
