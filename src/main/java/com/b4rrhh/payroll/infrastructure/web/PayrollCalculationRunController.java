package com.b4rrhh.payroll.infrastructure.web;

import com.b4rrhh.payroll.application.usecase.GetPayrollCalculationRunUseCase;
import com.b4rrhh.payroll.application.usecase.LaunchPayrollCalculationCommand;
import com.b4rrhh.payroll.application.usecase.LaunchPayrollCalculationUseCase;
import com.b4rrhh.payroll.application.usecase.ListPayrollCalculationRunMessagesUseCase;
import com.b4rrhh.payroll.application.usecase.PayrollLaunchEmployeeTarget;
import com.b4rrhh.payroll.application.usecase.PayrollLaunchTargetSelection;
import com.b4rrhh.payroll.domain.model.CalculationRun;
import com.b4rrhh.payroll.infrastructure.web.assembler.PayrollCalculationRunMessageResponseAssembler;
import com.b4rrhh.payroll.infrastructure.web.assembler.PayrollCalculationRunResponseAssembler;
import com.b4rrhh.payroll.infrastructure.web.dto.LaunchPayrollCalculationRequest;
import com.b4rrhh.payroll.infrastructure.web.dto.PayrollCalculationRunMessagesResponse;
import com.b4rrhh.payroll.infrastructure.web.dto.PayrollCalculationRunResponse;
import com.b4rrhh.payroll.infrastructure.web.dto.PayrollLaunchEmployeeTargetRequest;
import com.b4rrhh.payroll.infrastructure.web.dto.PayrollLaunchTargetSelectionRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payroll/calculation-runs")
public class PayrollCalculationRunController {

    private final LaunchPayrollCalculationUseCase launchPayrollCalculationUseCase;
    private final GetPayrollCalculationRunUseCase getPayrollCalculationRunUseCase;
    private final ListPayrollCalculationRunMessagesUseCase listPayrollCalculationRunMessagesUseCase;
    private final PayrollCalculationRunResponseAssembler payrollCalculationRunResponseAssembler;
    private final PayrollCalculationRunMessageResponseAssembler payrollCalculationRunMessageResponseAssembler;

    public PayrollCalculationRunController(
            LaunchPayrollCalculationUseCase launchPayrollCalculationUseCase,
            GetPayrollCalculationRunUseCase getPayrollCalculationRunUseCase,
            ListPayrollCalculationRunMessagesUseCase listPayrollCalculationRunMessagesUseCase,
            PayrollCalculationRunResponseAssembler payrollCalculationRunResponseAssembler,
            PayrollCalculationRunMessageResponseAssembler payrollCalculationRunMessageResponseAssembler
    ) {
        this.launchPayrollCalculationUseCase = launchPayrollCalculationUseCase;
        this.getPayrollCalculationRunUseCase = getPayrollCalculationRunUseCase;
        this.listPayrollCalculationRunMessagesUseCase = listPayrollCalculationRunMessagesUseCase;
        this.payrollCalculationRunResponseAssembler = payrollCalculationRunResponseAssembler;
        this.payrollCalculationRunMessageResponseAssembler = payrollCalculationRunMessageResponseAssembler;
    }

    @PostMapping("/launch")
    public ResponseEntity<PayrollCalculationRunResponse> launch(
            @RequestBody LaunchPayrollCalculationRequest request,
            Authentication authentication
    ) {
        CalculationRun run = launchPayrollCalculationUseCase.launch(new LaunchPayrollCalculationCommand(
                request.ruleSystemCode(),
                request.payrollPeriodCode(),
                request.payrollTypeCode(),
                request.calculationEngineCode(),
                request.calculationEngineVersion(),
                toTargetSelection(request.targetSelection()),
                authentication == null ? null : authentication.getName()
        ));

        return ResponseEntity.status(HttpStatus.CREATED).body(payrollCalculationRunResponseAssembler.toResponse(run));
    }

    @GetMapping("/{runId}")
    public ResponseEntity<PayrollCalculationRunResponse> getById(@PathVariable Long runId) {
        return getPayrollCalculationRunUseCase.getById(runId)
                .map(payrollCalculationRunResponseAssembler::toResponse)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{runId}/messages")
    public ResponseEntity<PayrollCalculationRunMessagesResponse> listRunMessages(@PathVariable Long runId) {
        if (getPayrollCalculationRunUseCase.getById(runId).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(
                payrollCalculationRunMessageResponseAssembler.toResponse(
                        runId,
                        listPayrollCalculationRunMessagesUseCase.listByRunId(runId)
                )
        );
    }

    private PayrollLaunchTargetSelection toTargetSelection(PayrollLaunchTargetSelectionRequest request) {
        if (request == null) {
            return null;
        }
        return new PayrollLaunchTargetSelection(
                request.selectionType(),
                toTarget(request.employee()),
                request.employees() == null ? null : request.employees().stream().map(this::toTarget).toList()
        );
    }

    private PayrollLaunchEmployeeTarget toTarget(PayrollLaunchEmployeeTargetRequest request) {
        if (request == null) {
            return null;
        }
        return new PayrollLaunchEmployeeTarget(request.employeeTypeCode(), request.employeeNumber());
    }
}