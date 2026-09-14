package com.b4rrhh.payroll.infrastructure.web;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PayrollOpenApiContractTest {

    @Test
    void calculateEndpointIsExplicitlyDocumentedAsTemporaryStub() throws IOException {
        String contract = contrato();

        assertTrue(contract.contains("/payrolls/calculate:"));
        assertTrue(contract.contains("Temporary pipeline-validation stub endpoint"));
        assertTrue(contract.contains("Temporary stub request used to materialize a payroll result during the pre-launch phase"));
        assertTrue(contract.contains("Temporary stub-provided payroll concepts"));
        assertTrue(contract.contains("Temporary stub-provided context snapshots"));
        assertTrue(contract.contains("/payroll/calculation-runs/launch:"));
        assertTrue(contract.contains("/payroll/calculation-runs/{runId}:"));
        assertTrue(contract.contains("/payroll/calculation-runs/{runId}/messages:"));
        assertTrue(contract.contains("Accept a payroll calculation run and return its identity without waiting"));
        assertTrue(contract.contains("\"202\":"));
        assertTrue(contract.contains("Calculation run accepted"));
        assertTrue(contract.contains("PayrollLaunchTargetSelectionRequest"));
        assertTrue(contract.contains("ALL_EMPLOYEES_WITH_PRESENCE_IN_PERIOD"));
        assertTrue(contract.contains("employee is required only for SINGLE_EMPLOYEE"));
        assertTrue(contract.contains("employees is required only for EMPLOYEE_LIST"));
        assertTrue(contract.contains("List persisted operational messages for a payroll calculation run"));
        assertTrue(contract.contains("PayrollCalculationRunMessagesResponse"));
        assertTrue(contract.contains("Payroll calculation run not found"));
        assertTrue(contract.contains("PayrollWarningResponse"));
        assertTrue(contract.contains("warnings:"));
        assertTrue(contract.contains("Functional payroll warning attached to a payroll result"));
    }

    @Test
    void payrollResponseDocumentsTheRunThatProducedThePayroll() throws IOException {
        String contract = contrato();

        assertTrue(contract.contains("Calculation run that produced this payroll"));
        assertTrue(contract.contains("which is the case for the temporary calculate"));
    }

    // El endpoint de mensajes llevaba tiempo servido por el backend, escrito en payroll-api.yaml y
    // ausente del contrato del que el frontend genera, o sea invisible para la pantalla que los
    // tiene que ensenar (frontend#61). Desde backend#80 hay un solo contrato y "falta aqui" y
    // "no existe" son lo mismo, pero estas afirmaciones se quedan: son lo que aquel issue costo.
    @Test
    void frontendFacingContractCarriesThePayrollCalculationRunSurface() throws IOException {
        String contract = contrato();

        assertTrue(contract.contains("/payroll/calculation-runs/launch:"));
        assertTrue(contract.contains("/payroll/calculation-runs/{runId}:"));
        assertTrue(contract.contains("/payroll/calculation-runs/{runId}/messages:"));
        assertTrue(contract.contains("listPayrollCalculationRunMessages"));
        // El cliente del frontend se genera de aqui: si este contrato sigue diciendo 201,
        // el cliente tratara un 202 como error y la pantalla no llegara nunca (#75).
        assertTrue(contract.contains("Calculation run accepted"));
        assertTrue(contract.contains("PayrollCalculationRunMessagesResponse:"));
        assertTrue(contract.contains("PayrollCalculationRunMessageResponse:"));
        assertTrue(contract.contains("Calculation run that produced this payroll"));
    }

    /**
     * Los pasos del calculo se sirven y se declaran, y el contrato dice lo que no se puede hacer
     * con ellos ({@code backend#97}).
     *
     * <p>No es un candado de forma: es que las tres cosas que rompen este endpoint —reordenar,
     * indexar por concepto y leer la lista vacia como «no hay conceptos»— no se ven mirando los
     * campos. Un cliente generado de un contrato que no las diga las hara.
     */
    @Test
    void theCalculationStepsEndpointIsDocumentedWithTheThreeThingsThatBreakIt() throws IOException {
        String contract = contrato();

        assertTrue(contract.contains("{presenceNumber}/steps:"));
        assertTrue(contract.contains("listPayrollCalculationSteps"));
        assertTrue(contract.contains("PayrollCalculationStepResponse"));

        // 1. El orden es el de ejecucion y no se toca.
        assertTrue(contract.contains("Items are ordered by executionOrder and that order is the whole point"));

        // 2. La clave de fila es executionOrder, no conceptCode.
        assertTrue(contract.contains("The row identity is executionOrder, never conceptCode"));
        assertTrue(contract.contains("carries concept 101 twice"));

        // 3. Y la lista vacia significa una cosa y solo una.
        assertTrue(contract.contains("An empty array means \"this payroll was calculated before the engine stored its steps\""));
        assertTrue(contract.contains("never \"this payroll has no concepts\""));
        assertTrue(contract.contains("never filled in by deriving steps from the payslip lines"));

        // Y el que llego al folio se distingue por el campo, no por la naturaleza.
        assertTrue(contract.contains("payslipOrderCode is null when that step did not reach the payslip"));
        assertTrue(contract.contains("the amount column must not be summed"));
    }

    @Test
    void bulkInvalidateEndpointIsDocumented() throws IOException {
        String contract = contrato();

        assertTrue(contract.contains("/payrolls/invalidate-bulk:"));
        assertTrue(contract.contains("bulkInvalidatePayroll"));
        assertTrue(contract.contains("Bulk invalidation workflow for payroll results"));
        assertTrue(contract.contains("invalidates only existing CALCULATED payrolls") ||
                contract.contains("invalidates all existing CALCULATED payrolls"));
        assertTrue(contract.contains("Protected payrolls") ||
                contract.contains("protected from bulk invalidation"));
        assertTrue(contract.contains("totalCandidates represents expanded presence-based units"));
        assertTrue(contract.contains("BulkInvalidatePayrollRequest"));
        assertTrue(contract.contains("BulkInvalidatePayrollResponse"));
        assertTrue(contract.contains("totalCandidates"));
        assertTrue(contract.contains("totalFound"));
        assertTrue(contract.contains("totalInvalidated"));
        assertTrue(contract.contains("totalSkippedAlreadyNotValid"));
        assertTrue(contract.contains("totalSkippedProtected"));
        assertTrue(contract.contains("totalSkippedNotFound"));
        assertTrue(contract.contains("EXPLICIT_VALIDATED or DEFINITIVE"));
    }

    /**
     * El contrato con los espacios normalizados. Estas afirmaciones son sobre lo que el contrato
     * dice, no sobre como esta doblado: comparar contra el texto crudo hacia que reajustar una
     * linea del YAML tumbara el candado sin que el contrato hubiera cambiado (backend#80).
     */
    private static String contrato() throws IOException {
        return Files.readString(Path.of("openapi", "personnel-administration-api.yaml"))
                .replaceAll("\\s+", " ");
    }
}
