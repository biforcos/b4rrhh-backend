package com.b4rrhh.payroll.infrastructure.web;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PayrollOpenApiContractTest {

    @Test
    void calculateEndpointIsExplicitlyDocumentedAsTemporaryStub() throws IOException {
        String contract = Files.readString(Path.of("openapi", "payroll-api.yaml"));

        assertTrue(contract.contains("/payrolls/calculate:"));
        assertTrue(contract.contains("Temporary pipeline-validation stub endpoint"));
        assertTrue(contract.contains("Temporary stub request used to materialize a payroll result during the pre-launch phase"));
        assertTrue(contract.contains("Temporary stub-provided payroll concepts"));
        assertTrue(contract.contains("Temporary stub-provided context snapshots"));
        assertTrue(contract.contains("/payroll/calculation-runs/launch:"));
        assertTrue(contract.contains("/payroll/calculation-runs/{runId}:"));
        assertTrue(contract.contains("/payroll/calculation-runs/{runId}/messages:"));
        assertTrue(contract.contains("Launch payroll calculation workflow synchronously"));
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
        String contract = Files.readString(Path.of("openapi", "payroll-api.yaml"));

        assertTrue(contract.contains("Calculation run that produced this payroll"));
        assertTrue(contract.contains("which is the case for the temporary calculate"));
    }

    @Test
    void bulkInvalidateEndpointIsDocumented() throws IOException {
        String contract = Files.readString(Path.of("openapi", "payroll-api.yaml"));

        assertTrue(contract.contains("/payrolls/invalidate-bulk:"));
        assertTrue(contract.contains("invalidatePayrollBulk"));
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
}