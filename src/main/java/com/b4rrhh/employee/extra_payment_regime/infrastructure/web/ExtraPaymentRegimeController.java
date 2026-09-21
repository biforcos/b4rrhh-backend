package com.b4rrhh.employee.extra_payment_regime.infrastructure.web;

import com.b4rrhh.employee.extra_payment_regime.application.usecase.CreateExtraPaymentRegimeCommand;
import com.b4rrhh.employee.extra_payment_regime.application.usecase.CreateExtraPaymentRegimeUseCase;
import com.b4rrhh.employee.extra_payment_regime.application.usecase.DeleteExtraPaymentRegimeCommand;
import com.b4rrhh.employee.extra_payment_regime.application.usecase.DeleteExtraPaymentRegimeUseCase;
import com.b4rrhh.employee.extra_payment_regime.application.usecase.GetExtraPaymentRegimeByBusinessKeyCommand;
import com.b4rrhh.employee.extra_payment_regime.application.usecase.GetExtraPaymentRegimeByBusinessKeyUseCase;
import com.b4rrhh.employee.extra_payment_regime.application.usecase.ListEmployeeExtraPaymentRegimesCommand;
import com.b4rrhh.employee.extra_payment_regime.application.usecase.ListEmployeeExtraPaymentRegimesUseCase;
import com.b4rrhh.employee.extra_payment_regime.application.usecase.PlanExtraPaymentRegimeChangeCommand;
import com.b4rrhh.employee.extra_payment_regime.application.usecase.PlanExtraPaymentRegimeChangeUseCase;
import com.b4rrhh.employee.extra_payment_regime.application.usecase.UpdateExtraPaymentRegimeCommand;
import com.b4rrhh.employee.extra_payment_regime.application.usecase.UpdateExtraPaymentRegimeUseCase;
import com.b4rrhh.employee.extra_payment_regime.domain.model.ExtraPaymentRegime;
import com.b4rrhh.employee.extra_payment_regime.infrastructure.web.assembler.ExtraPaymentRegimeResponseAssembler;
import com.b4rrhh.employee.extra_payment_regime.infrastructure.web.dto.CreateExtraPaymentRegimeRequest;
import com.b4rrhh.employee.extra_payment_regime.infrastructure.web.dto.ExtraPaymentRegimePlanResponse;
import com.b4rrhh.employee.extra_payment_regime.infrastructure.web.dto.ExtraPaymentRegimeResponse;
import com.b4rrhh.employee.extra_payment_regime.infrastructure.web.dto.PlanExtraPaymentRegimeChangeRequest;
import com.b4rrhh.employee.extra_payment_regime.infrastructure.web.dto.UpdateExtraPaymentRegimeRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/employees/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}/extra-payment-regimes")
public class ExtraPaymentRegimeController {

    private final CreateExtraPaymentRegimeUseCase createExtraPaymentRegimeUseCase;
    private final ListEmployeeExtraPaymentRegimesUseCase listEmployeeExtraPaymentRegimesUseCase;
    private final GetExtraPaymentRegimeByBusinessKeyUseCase getExtraPaymentRegimeByBusinessKeyUseCase;
    private final UpdateExtraPaymentRegimeUseCase updateExtraPaymentRegimeUseCase;
    private final DeleteExtraPaymentRegimeUseCase deleteExtraPaymentRegimeUseCase;
    private final PlanExtraPaymentRegimeChangeUseCase planExtraPaymentRegimeChangeUseCase;
    private final ExtraPaymentRegimeResponseAssembler extraPaymentRegimeResponseAssembler;

    public ExtraPaymentRegimeController(
            CreateExtraPaymentRegimeUseCase createExtraPaymentRegimeUseCase,
            ListEmployeeExtraPaymentRegimesUseCase listEmployeeExtraPaymentRegimesUseCase,
            GetExtraPaymentRegimeByBusinessKeyUseCase getExtraPaymentRegimeByBusinessKeyUseCase,
            UpdateExtraPaymentRegimeUseCase updateExtraPaymentRegimeUseCase,
            DeleteExtraPaymentRegimeUseCase deleteExtraPaymentRegimeUseCase,
            PlanExtraPaymentRegimeChangeUseCase planExtraPaymentRegimeChangeUseCase,
            ExtraPaymentRegimeResponseAssembler extraPaymentRegimeResponseAssembler
    ) {
        this.createExtraPaymentRegimeUseCase = createExtraPaymentRegimeUseCase;
        this.listEmployeeExtraPaymentRegimesUseCase = listEmployeeExtraPaymentRegimesUseCase;
        this.getExtraPaymentRegimeByBusinessKeyUseCase = getExtraPaymentRegimeByBusinessKeyUseCase;
        this.updateExtraPaymentRegimeUseCase = updateExtraPaymentRegimeUseCase;
        this.deleteExtraPaymentRegimeUseCase = deleteExtraPaymentRegimeUseCase;
        this.planExtraPaymentRegimeChangeUseCase = planExtraPaymentRegimeChangeUseCase;
        this.extraPaymentRegimeResponseAssembler = extraPaymentRegimeResponseAssembler;
    }

    /**
     * El regimen es obligatorio aqui aunque el mandato admita no decirlo: quien lo pide por el API
     * lo esta eligiendo. Omitirlo es cosa del alta, que copia el del convenio.
     */
    @PostMapping
    public ResponseEntity<ExtraPaymentRegimeResponse> create(
            @PathVariable String ruleSystemCode,
            @PathVariable String employeeTypeCode,
            @PathVariable String employeeNumber,
            @RequestBody CreateExtraPaymentRegimeRequest request
    ) {
        if (request.getProrated() == null) {
            throw new IllegalArgumentException("prorated is required");
        }

        ExtraPaymentRegime created = createExtraPaymentRegimeUseCase.create(
                new CreateExtraPaymentRegimeCommand(
                        ruleSystemCode,
                        employeeTypeCode,
                        employeeNumber,
                        request.getStartDate(),
                        request.getEndDate(),
                        request.getProrated()
                )
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(extraPaymentRegimeResponseAssembler.toResponse(created));
    }

    @GetMapping
    public ResponseEntity<List<ExtraPaymentRegimeResponse>> list(
            @PathVariable String ruleSystemCode,
            @PathVariable String employeeTypeCode,
            @PathVariable String employeeNumber
    ) {
        List<ExtraPaymentRegimeResponse> response = extraPaymentRegimeResponseAssembler.toResponseList(
                listEmployeeExtraPaymentRegimesUseCase.listByEmployeeBusinessKey(
                        new ListEmployeeExtraPaymentRegimesCommand(
                                ruleSystemCode,
                                employeeTypeCode,
                                employeeNumber
                        )
                )
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{extraPaymentRegimeNumber}")
    public ResponseEntity<ExtraPaymentRegimeResponse> getByBusinessKey(
            @PathVariable String ruleSystemCode,
            @PathVariable String employeeTypeCode,
            @PathVariable String employeeNumber,
            @PathVariable Integer extraPaymentRegimeNumber
    ) {
        ExtraPaymentRegime regime = getExtraPaymentRegimeByBusinessKeyUseCase.getByBusinessKey(
                new GetExtraPaymentRegimeByBusinessKeyCommand(
                        ruleSystemCode,
                        employeeTypeCode,
                        employeeNumber,
                        extraPaymentRegimeNumber
                )
        );

        return ResponseEntity.ok(extraPaymentRegimeResponseAssembler.toResponse(regime));
    }

    /**
     * Lo que el cambio le haria a la serie, sin aplicarlo (ADR-057). Es lo que la pantalla ensena
     * antes de que el usuario confirme.
     */
    @PostMapping("/plan")
    public ResponseEntity<ExtraPaymentRegimePlanResponse> plan(
            @PathVariable String ruleSystemCode,
            @PathVariable String employeeTypeCode,
            @PathVariable String employeeNumber,
            @RequestBody PlanExtraPaymentRegimeChangeRequest request
    ) {
        return ResponseEntity.ok(extraPaymentRegimeResponseAssembler.toPlanResponse(
                planExtraPaymentRegimeChangeUseCase.plan(new PlanExtraPaymentRegimeChangeCommand(
                        ruleSystemCode,
                        employeeTypeCode,
                        employeeNumber,
                        request.operation(),
                        request.extraPaymentRegimeNumber(),
                        request.startDate(),
                        request.endDate()
                ))
        ));
    }

    @PutMapping("/{extraPaymentRegimeNumber}")
    public ResponseEntity<ExtraPaymentRegimeResponse> update(
            @PathVariable String ruleSystemCode,
            @PathVariable String employeeTypeCode,
            @PathVariable String employeeNumber,
            @PathVariable Integer extraPaymentRegimeNumber,
            @RequestBody UpdateExtraPaymentRegimeRequest request
    ) {
        ExtraPaymentRegime updated = updateExtraPaymentRegimeUseCase.update(
                new UpdateExtraPaymentRegimeCommand(
                        ruleSystemCode,
                        employeeTypeCode,
                        employeeNumber,
                        extraPaymentRegimeNumber,
                        request.startDate(),
                        request.endDate(),
                        request.prorated()
                )
        );

        return ResponseEntity.ok(extraPaymentRegimeResponseAssembler.toResponse(updated));
    }

    @DeleteMapping("/{extraPaymentRegimeNumber}")
    public ResponseEntity<Void> delete(
            @PathVariable String ruleSystemCode,
            @PathVariable String employeeTypeCode,
            @PathVariable String employeeNumber,
            @PathVariable Integer extraPaymentRegimeNumber
    ) {
        deleteExtraPaymentRegimeUseCase.delete(
                new DeleteExtraPaymentRegimeCommand(
                        ruleSystemCode,
                        employeeTypeCode,
                        employeeNumber,
                        extraPaymentRegimeNumber
                )
        );

        return ResponseEntity.noContent().build();
    }
}
