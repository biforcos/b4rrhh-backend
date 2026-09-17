package com.b4rrhh.payroll.infrastructure.web;

import com.b4rrhh.payroll.application.usecase.BulkFinalizePayrollCommand;
import com.b4rrhh.payroll.application.usecase.BulkFinalizePayrollResult;
import com.b4rrhh.payroll.application.usecase.BulkFinalizePayrollUseCase;
import com.b4rrhh.payroll.application.usecase.BulkInvalidatePayrollCommand;
import com.b4rrhh.payroll.application.usecase.BulkInvalidatePayrollResult;
import com.b4rrhh.payroll.application.usecase.BulkInvalidatePayrollUseCase;
import com.b4rrhh.payroll.application.usecase.FinalizePayrollCommand;
import com.b4rrhh.payroll.application.usecase.FinalizePayrollUseCase;
import com.b4rrhh.payroll.application.usecase.GetPayrollByBusinessKeyUseCase;
import com.b4rrhh.payroll.application.usecase.InvalidatePayrollCommand;
import com.b4rrhh.payroll.application.usecase.InvalidatePayrollUseCase;
import com.b4rrhh.payroll.application.usecase.ListPayrollCalculationStepsUseCase;
import com.b4rrhh.payroll.application.usecase.PayrollLaunchEmployeeTarget;
import com.b4rrhh.payroll.application.usecase.PayrollLaunchTargetSelection;
import com.b4rrhh.payroll.application.usecase.ValidatePayrollCommand;
import com.b4rrhh.payroll.application.usecase.ValidatePayrollUseCase;
import com.b4rrhh.payroll.application.service.PayrollRuleFreshness;
import com.b4rrhh.payroll.domain.model.Payroll;
import com.b4rrhh.payroll.infrastructure.web.assembler.PayrollCalculationStepResponseAssembler;
import com.b4rrhh.payroll.infrastructure.web.assembler.PayrollResponseAssembler;
import com.b4rrhh.payroll.infrastructure.web.dto.BulkFinalizePayrollRequest;
import com.b4rrhh.payroll.infrastructure.web.dto.BulkFinalizePayrollResponse;
import com.b4rrhh.payroll.infrastructure.web.dto.BulkInvalidatePayrollRequest;
import com.b4rrhh.payroll.infrastructure.web.dto.BulkInvalidatePayrollResponse;
import com.b4rrhh.payroll.infrastructure.web.dto.InvalidatePayrollRequest;
import com.b4rrhh.payroll.infrastructure.web.dto.PayrollCalculationStepResponse;
import com.b4rrhh.payroll.infrastructure.web.dto.PayrollLaunchEmployeeTargetRequest;
import com.b4rrhh.payroll.infrastructure.web.dto.PayrollLaunchTargetSelectionRequest;
import com.b4rrhh.payroll.infrastructure.web.dto.PayrollResponse;
import com.b4rrhh.payroll.application.usecase.RecalculatePayrollCommand;
import com.b4rrhh.payroll.application.usecase.RecalculatePayrollUseCase;
import com.b4rrhh.payroll.application.usecase.SearchPayrollsQuery;
import com.b4rrhh.payroll.application.usecase.SearchPayrollsUseCase;
import com.b4rrhh.payroll.domain.model.PayrollStatus;
import com.b4rrhh.payroll.infrastructure.web.dto.PayrollSummaryResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/payrolls")
public class PayrollController {

    private final GetPayrollByBusinessKeyUseCase getPayrollByBusinessKeyUseCase;
    private final InvalidatePayrollUseCase invalidatePayrollUseCase;
    private final ValidatePayrollUseCase validatePayrollUseCase;
    private final FinalizePayrollUseCase finalizePayrollUseCase;
    private final BulkInvalidatePayrollUseCase bulkInvalidatePayrollUseCase;
    private final BulkFinalizePayrollUseCase bulkFinalizePayrollUseCase;
    private final SearchPayrollsUseCase searchPayrollsUseCase;
    private final RecalculatePayrollUseCase recalculatePayrollUseCase;
    private final ListPayrollCalculationStepsUseCase listPayrollCalculationStepsUseCase;
    private final PayrollResponseAssembler payrollResponseAssembler;
    private final PayrollCalculationStepResponseAssembler payrollCalculationStepResponseAssembler;
    private final PayrollRuleFreshness payrollRuleFreshness;

    public PayrollController(
            GetPayrollByBusinessKeyUseCase getPayrollByBusinessKeyUseCase,
            InvalidatePayrollUseCase invalidatePayrollUseCase,
            ValidatePayrollUseCase validatePayrollUseCase,
            FinalizePayrollUseCase finalizePayrollUseCase,
            BulkInvalidatePayrollUseCase bulkInvalidatePayrollUseCase,
            BulkFinalizePayrollUseCase bulkFinalizePayrollUseCase,
            SearchPayrollsUseCase searchPayrollsUseCase,
            RecalculatePayrollUseCase recalculatePayrollUseCase,
            ListPayrollCalculationStepsUseCase listPayrollCalculationStepsUseCase,
            PayrollResponseAssembler payrollResponseAssembler,
            PayrollCalculationStepResponseAssembler payrollCalculationStepResponseAssembler,
            PayrollRuleFreshness payrollRuleFreshness
    ) {
        this.getPayrollByBusinessKeyUseCase = getPayrollByBusinessKeyUseCase;
        this.invalidatePayrollUseCase = invalidatePayrollUseCase;
        this.validatePayrollUseCase = validatePayrollUseCase;
        this.finalizePayrollUseCase = finalizePayrollUseCase;
        this.bulkInvalidatePayrollUseCase = bulkInvalidatePayrollUseCase;
        this.bulkFinalizePayrollUseCase = bulkFinalizePayrollUseCase;
        this.searchPayrollsUseCase = searchPayrollsUseCase;
        this.recalculatePayrollUseCase = recalculatePayrollUseCase;
        this.listPayrollCalculationStepsUseCase = listPayrollCalculationStepsUseCase;
        this.payrollResponseAssembler = payrollResponseAssembler;
        this.payrollCalculationStepResponseAssembler = payrollCalculationStepResponseAssembler;
        this.payrollRuleFreshness = payrollRuleFreshness;
    }

    /**
     * El recibo servido, con la marca de si las reglas han cambiado desde que se calculó
     * ({@code backend#107}).
     *
     * <p>Por aquí pasan las cinco salidas que devuelven un recibo, y pasan a propósito: la marca no
     * es de la pantalla del recibo, es del recibo. Anular, validar, cerrar y recalcular devuelven
     * uno igual que la consulta, y si sólo la consulta la trajera, el mismo recibo diría una cosa u
     * otra según por dónde se hubiera llegado a él.
     */
    private PayrollResponse toResponse(Payroll payroll) {
        return payrollResponseAssembler.toResponse(
                payroll, payrollRuleFreshness.rulesChangedSinceCalculation(payroll));
    }

    @GetMapping("/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}/{payrollPeriodCode}/{payrollTypeCode}/{presenceNumber}")
    public ResponseEntity<PayrollResponse> getByBusinessKey(
            @PathVariable String ruleSystemCode,
            @PathVariable String employeeTypeCode,
            @PathVariable String employeeNumber,
            @PathVariable String payrollPeriodCode,
            @PathVariable String payrollTypeCode,
            @PathVariable Integer presenceNumber
    ) {
        return getPayrollByBusinessKeyUseCase.getByBusinessKey(
                ruleSystemCode,
                employeeTypeCode,
                employeeNumber,
                payrollPeriodCode,
                payrollTypeCode,
                presenceNumber
        )
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Los pasos con los que el motor calculó este recibo, en orden de ejecución.
     *
     * <p>Cuelga de la dirección del recibo y va fuera del {@code PayrollResponse} por dos motivos:
     * son 35 o 39 filas por recibo que viajarían en cada apertura de ficha para que casi nadie las
     * mire, y sobre todo porque la Valorización es un cajón que se abre a demanda —el momento de
     * traerlos es cuando alguien pregunta, que es el gesto que este endpoint sirve
     * ({@code backend#97}).
     *
     * <p>Los tres casos se distinguen y ninguno se confunde con otro: no hay recibo, {@code 404};
     * hay recibo y hay pasos, {@code 200} con la lista; hay recibo y no hay ni un paso,
     * {@code 200} con lista vacía, que es lo que devuelve todo recibo calculado antes de la
     * {@code V129}. Esa lista vacía <b>no</b> se rellena derivándola de {@code payroll_concept}.
     */
    @GetMapping("/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}/{payrollPeriodCode}/{payrollTypeCode}/{presenceNumber}/steps")
    public ResponseEntity<List<PayrollCalculationStepResponse>> getCalculationSteps(
            @PathVariable String ruleSystemCode,
            @PathVariable String employeeTypeCode,
            @PathVariable String employeeNumber,
            @PathVariable String payrollPeriodCode,
            @PathVariable String payrollTypeCode,
            @PathVariable Integer presenceNumber
    ) {
        return listPayrollCalculationStepsUseCase.listByPayrollBusinessKey(
                ruleSystemCode,
                employeeTypeCode,
                employeeNumber,
                payrollPeriodCode,
                payrollTypeCode,
                presenceNumber
        )
                .map(payrollCalculationStepResponseAssembler::toResponse)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}/{payrollPeriodCode}/{payrollTypeCode}/{presenceNumber}/invalidate")
    public ResponseEntity<PayrollResponse> invalidate(
            @PathVariable String ruleSystemCode,
            @PathVariable String employeeTypeCode,
            @PathVariable String employeeNumber,
            @PathVariable String payrollPeriodCode,
            @PathVariable String payrollTypeCode,
            @PathVariable Integer presenceNumber,
            @RequestBody InvalidatePayrollRequest request
    ) {
        Payroll payroll = invalidatePayrollUseCase.invalidate(new InvalidatePayrollCommand(
                ruleSystemCode,
                employeeTypeCode,
                employeeNumber,
                payrollPeriodCode,
                payrollTypeCode,
                presenceNumber,
                request.statusReasonCode()
        ));

        return ResponseEntity.ok(toResponse(payroll));
    }

    @PostMapping("/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}/{payrollPeriodCode}/{payrollTypeCode}/{presenceNumber}/validate")
    public ResponseEntity<PayrollResponse> validate(
            @PathVariable String ruleSystemCode,
            @PathVariable String employeeTypeCode,
            @PathVariable String employeeNumber,
            @PathVariable String payrollPeriodCode,
            @PathVariable String payrollTypeCode,
            @PathVariable Integer presenceNumber
    ) {
        Payroll payroll = validatePayrollUseCase.validate(new ValidatePayrollCommand(
                ruleSystemCode,
                employeeTypeCode,
                employeeNumber,
                payrollPeriodCode,
                payrollTypeCode,
                presenceNumber
        ));

        return ResponseEntity.ok(toResponse(payroll));
    }

    @PostMapping("/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}/{payrollPeriodCode}/{payrollTypeCode}/{presenceNumber}/finalize")
    public ResponseEntity<PayrollResponse> finalizePayroll(
            @PathVariable String ruleSystemCode,
            @PathVariable String employeeTypeCode,
            @PathVariable String employeeNumber,
            @PathVariable String payrollPeriodCode,
            @PathVariable String payrollTypeCode,
            @PathVariable Integer presenceNumber
    ) {
        Payroll payroll = finalizePayrollUseCase.finalizePayroll(new FinalizePayrollCommand(
                ruleSystemCode,
                employeeTypeCode,
                employeeNumber,
                payrollPeriodCode,
                payrollTypeCode,
                presenceNumber
        ));

        return ResponseEntity.ok(toResponse(payroll));
    }

    @PostMapping("/invalidate-bulk")
    public ResponseEntity<BulkInvalidatePayrollResponse> invalidateBulk(
            @RequestBody BulkInvalidatePayrollRequest request
    ) {
        BulkInvalidatePayrollResult result = bulkInvalidatePayrollUseCase.invalidateBulk(
                new BulkInvalidatePayrollCommand(
                        request.ruleSystemCode(),
                        request.payrollPeriodCode(),
                        request.payrollTypeCode(),
                        request.statusReasonCode(),
                        toTargetSelection(request.targetSelection())
                )
        );

        return ResponseEntity.ok(new BulkInvalidatePayrollResponse(
                result.ruleSystemCode(),
                result.payrollPeriodCode(),
                result.payrollTypeCode(),
                result.totalCandidates(),
                result.totalFound(),
                result.totalInvalidated(),
                result.totalSkippedAlreadyNotValid(),
                result.totalSkippedProtected(),
                result.totalSkippedNotFound(),
                result.statusReasonCode()
        ));
    }


    /**
     * El tercer verbo del periodo (backend#102). Cerrar en masa no cierra un periodo: aplica a
     * muchos recibos el mismo verbo que /finalize aplica a uno, y por eso comparte el selector de
     * objetivo con los otros dos y no inventa ninguna entidad nueva.
     */
    @PostMapping("/finalize-bulk")
    public ResponseEntity<BulkFinalizePayrollResponse> finalizeBulk(
            @RequestBody BulkFinalizePayrollRequest request
    ) {
        BulkFinalizePayrollResult result = bulkFinalizePayrollUseCase.finalizeBulk(
                new BulkFinalizePayrollCommand(
                        request.ruleSystemCode(),
                        request.payrollPeriodCode(),
                        request.payrollTypeCode(),
                        toTargetSelection(request.targetSelection())
                )
        );

        return ResponseEntity.ok(new BulkFinalizePayrollResponse(
                result.ruleSystemCode(),
                result.payrollPeriodCode(),
                result.payrollTypeCode(),
                result.totalCandidates(),
                result.totalFound(),
                result.totalFinalized(),
                result.totalSkippedAlreadyDefinitive(),
                result.totalSkippedNotEligibleByStatus(),
                result.totalSkippedNotFound()
        ));
    }

    @GetMapping
    public ResponseEntity<List<PayrollSummaryResponse>> search(
            @RequestParam(required = false) String ruleSystemCode,
            @RequestParam(required = false) String payrollPeriodCode,
            @RequestParam(required = false) String employeeNumber,
            @RequestParam(required = false) String status
    ) {
        PayrollStatus parsedStatus = null;
        if (status != null) {
            try {
                parsedStatus = PayrollStatus.valueOf(status.toUpperCase(java.util.Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw new com.b4rrhh.payroll.domain.exception.InvalidPayrollArgumentException(
                    "Invalid status value: '" + status + "'. Valid values: " + java.util.Arrays.toString(PayrollStatus.values()));
            }
        }
        List<PayrollSummaryResponse> body = searchPayrollsUseCase
                .search(new SearchPayrollsQuery(ruleSystemCode, payrollPeriodCode, employeeNumber, parsedStatus))
                .stream()
                .map(payrollResponseAssembler::toSummaryResponse)
                .toList();
        return ResponseEntity.ok(body);
    }

    @PostMapping("/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}/{payrollPeriodCode}/{payrollTypeCode}/{presenceNumber}/recalculate")
    public ResponseEntity<PayrollResponse> recalculate(
            @PathVariable String ruleSystemCode,
            @PathVariable String employeeTypeCode,
            @PathVariable String employeeNumber,
            @PathVariable String payrollPeriodCode,
            @PathVariable String payrollTypeCode,
            @PathVariable Integer presenceNumber,
            Authentication authentication
    ) {
        Payroll payroll = recalculatePayrollUseCase.recalculate(new RecalculatePayrollCommand(
                ruleSystemCode, employeeTypeCode, employeeNumber,
                payrollPeriodCode, payrollTypeCode, presenceNumber,
                authentication == null ? null : authentication.getName()
        ));
        return ResponseEntity.ok(toResponse(payroll));
    }

    private PayrollLaunchTargetSelection toTargetSelection(PayrollLaunchTargetSelectionRequest request) {
        if (request == null) {
            return null;
        }
        return new PayrollLaunchTargetSelection(
                request.selectionType(),
                request.employee() == null ? null : new PayrollLaunchEmployeeTarget(
                        request.employee().employeeTypeCode(),
                        request.employee().employeeNumber()
                ),
                request.employees() == null ? null : request.employees().stream()
                        .map(e -> new PayrollLaunchEmployeeTarget(e.employeeTypeCode(), e.employeeNumber()))
                        .toList()
        );
    }
}