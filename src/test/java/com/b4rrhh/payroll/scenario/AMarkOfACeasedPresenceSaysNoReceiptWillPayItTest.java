package com.b4rrhh.payroll.scenario;

import com.b4rrhh.employee.payroll_input.application.usecase.CreateEmployeePayrollInputCommand;
import com.b4rrhh.employee.payroll_input.application.usecase.CreateEmployeePayrollInputUseCase;
import com.b4rrhh.payroll.application.usecase.BulkFinalizePayrollCommand;
import com.b4rrhh.payroll.application.usecase.BulkFinalizePayrollUseCase;
import com.b4rrhh.payroll.application.usecase.LaunchPayrollCalculationCommand;
import com.b4rrhh.payroll.application.usecase.LaunchPayrollCalculationUseCase;
import com.b4rrhh.payroll.application.usecase.PayrollLaunchEmployeeTarget;
import com.b4rrhh.payroll.application.usecase.PayrollLaunchTargetSelection;
import com.b4rrhh.payroll.application.usecase.PayrollLaunchTargetSelectionType;
import com.b4rrhh.payroll.application.usecase.PayrollRetroRequest;
import com.b4rrhh.support.TestWebSobreEsquemaReal;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Una marca de una presencia cesada dice que <b>no hay recibo que la pague</b> ({@code backend#139}).
 *
 * <p>Lo cuenta la corrida desde el {@code #133} (RETRO_MARK_WITHOUT_A_RECEIPT_TO_PAY_IT). Lo que faltaba es
 * que la ficha lo dijera: una marca así llevaba meses en «pendiente» sin explicar por qué. No se paga en
 * la presencia nueva —eso sería un finiquito complementario, otro recibo y otro camino (ADR-076 §8)—, así
 * que lo que hay que ver es que nadie la va a pagar.
 *
 * <p>La regla exacta: la presencia de la marca cesó <b>y el recibo del mes de su cese ya está cerrado</b>.
 * Si el cese es del mes abierto, ese recibo todavía la paga.
 */
@TestWebSobreEsquemaReal
@WithMockUser(roles = "ADMIN")
class AMarkOfACeasedPresenceSaysNoReceiptWillPayItTest {

    private static final LocalDate ENERO_1 = LocalDate.of(2025, 1, 1);

    @Autowired private MockMvc mockMvc;
    @Autowired private LaunchPayrollCalculationUseCase launch;
    @Autowired private BulkFinalizePayrollUseCase cerrarEnMasa;
    @Autowired private CreateEmployeePayrollInputUseCase createInput;
    @Autowired private JdbcTemplate jdbc;

    @PersistenceContext private EntityManager entityManager;

    private PayrollScenarioFixtures fixtures;

    @BeforeEach
    void setUp() {
        fixtures = new PayrollScenarioFixtures(jdbc);
    }

    @Test
    void laMarcaDeUnaPresenciaCesadaConSuUltimoMesCerradoNoTieneReciboQueLaPague() throws Exception {
        String emp = numeroUnico();
        long empId = fixtures.insertEmployee("ESP", "INTERNAL", emp);
        fixtures.insertPresence(empId, 1, ENERO_1, LocalDate.of(2025, 8, 31));
        fixtures.insertPresence(empId, 2, LocalDate.of(2025, 10, 1), null);
        fixtures.insertLaborClassification(empId, ENERO_1);
        fixtures.insertWorkingTime(empId, new BigDecimal("100.00"), ENERO_1, null);

        calcularYCerrar(emp, "202508");
        createInput.create(new CreateEmployeePayrollInputCommand(
                "ESP", "INTERNAL", emp, "H01", 202508, new BigDecimal("10")));
        entityManager.flush();

        assertTrue(jdbc.queryForObject("select count(*) from payroll.retro_mark where employee_number = ?"
                + " and presence_number = 1 and status = 'ACTIVE'", Integer.class, emp) == 1,
                "la corrección a agosto ha dejado su marca en la presencia 1");

        mockMvc.perform(get("/employees/ESP/INTERNAL/{emp}/retro-marks", emp))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].presenceNumber").value(1))
                .andExpect(jsonPath("$[0].withoutAReceiptToPayIt").value(true));
    }

    @Test
    void laMarcaDeUnaPresenciaVivaSiTieneQuienLaPague() throws Exception {
        String emp = numeroUnico();
        long empId = fixtures.insertEmployee("ESP", "INTERNAL", emp);
        fixtures.insertPresence(empId, 1, ENERO_1, null);
        fixtures.insertLaborClassification(empId, ENERO_1);
        fixtures.insertWorkingTime(empId, new BigDecimal("100.00"), ENERO_1, null);

        calcularYCerrar(emp, "202508");
        createInput.create(new CreateEmployeePayrollInputCommand(
                "ESP", "INTERNAL", emp, "H01", 202508, new BigDecimal("10")));
        entityManager.flush();

        mockMvc.perform(get("/employees/ESP/INTERNAL/{emp}/retro-marks", emp))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].withoutAReceiptToPayIt").value(false));
    }

    private void calcularYCerrar(String emp, String periodo) {
        PayrollLaunchTargetSelection uno = new PayrollLaunchTargetSelection(
                PayrollLaunchTargetSelectionType.SINGLE_EMPLOYEE,
                new PayrollLaunchEmployeeTarget("INTERNAL", emp),
                null);
        var run = launch.launch(new LaunchPayrollCalculationCommand(
                "ESP", periodo, "NORMAL", "ENGINE", "1.0", uno, null, PayrollRetroRequest.none()));
        assertTrue("COMPLETED".equals(run.status()), "la corrida de " + periodo + ": " + run.status());
        cerrarEnMasa.finalizeBulk(new BulkFinalizePayrollCommand("ESP", periodo, "NORMAL", uno));
        entityManager.flush();
        entityManager.clear();
    }

    private String numeroUnico() {
        return "PC" + (System.nanoTime() % 1_000_000_000L);
    }
}
