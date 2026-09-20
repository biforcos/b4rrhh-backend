package com.b4rrhh.payroll_engine.concept.infrastructure.web;

import com.b4rrhh.payroll_engine.concept.application.usecase.CreatePayrollConceptUseCase;
import com.b4rrhh.payroll_engine.concept.application.usecase.DeletePayrollConceptUseCase;
import com.b4rrhh.payroll_engine.concept.application.usecase.GetConceptLabelsUseCase;
import com.b4rrhh.payroll_engine.concept.application.usecase.ListPayrollConceptsUseCase;
import com.b4rrhh.payroll_engine.concept.application.usecase.UpdateConceptLabelCommand;
import com.b4rrhh.payroll_engine.concept.application.usecase.UpdateConceptLabelUseCase;
import com.b4rrhh.payroll_engine.concept.application.usecase.UpdateConceptSummaryCommand;
import com.b4rrhh.payroll_engine.concept.application.usecase.UpdateConceptSummaryUseCase;
import com.b4rrhh.payroll_engine.concept.domain.model.PayrollConcept;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/payroll-engine/{ruleSystemCode}/concepts")
public class PayrollConceptManagementController {

    private final CreatePayrollConceptUseCase createPayrollConceptUseCase;
    private final DeletePayrollConceptUseCase deletePayrollConceptUseCase;
    private final ListPayrollConceptsUseCase listPayrollConceptsUseCase;
    private final UpdateConceptSummaryUseCase updateConceptSummaryUseCase;
    private final UpdateConceptLabelUseCase updateConceptLabelUseCase;
    private final GetConceptLabelsUseCase getConceptLabelsUseCase;
    private final PayrollConceptManagementAssembler assembler;

    public PayrollConceptManagementController(
            CreatePayrollConceptUseCase createPayrollConceptUseCase,
            DeletePayrollConceptUseCase deletePayrollConceptUseCase,
            ListPayrollConceptsUseCase listPayrollConceptsUseCase,
            UpdateConceptSummaryUseCase updateConceptSummaryUseCase,
            UpdateConceptLabelUseCase updateConceptLabelUseCase,
            GetConceptLabelsUseCase getConceptLabelsUseCase,
            PayrollConceptManagementAssembler assembler
    ) {
        this.createPayrollConceptUseCase = createPayrollConceptUseCase;
        this.deletePayrollConceptUseCase = deletePayrollConceptUseCase;
        this.listPayrollConceptsUseCase = listPayrollConceptsUseCase;
        this.updateConceptSummaryUseCase = updateConceptSummaryUseCase;
        this.updateConceptLabelUseCase = updateConceptLabelUseCase;
        this.getConceptLabelsUseCase = getConceptLabelsUseCase;
        this.assembler = assembler;
    }

    @GetMapping
    public List<PayrollConceptDesignerResponse> list(@PathVariable String ruleSystemCode) {
        // Los nombres se piden una vez para toda la lista y no uno por concepto: el catalogo es
        // pequeno pero la forma «una lectura por fila» no lo es (backend#109).
        Map<String, String> labels = getConceptLabelsUseCase.byRuleSystemCode(ruleSystemCode);
        return listPayrollConceptsUseCase.listByRuleSystemCode(ruleSystemCode)
                .stream()
                .map(concept -> assembler.toResponse(concept, labels.get(concept.getConceptCode())))
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PayrollConceptDesignerResponse create(
            @PathVariable String ruleSystemCode,
            @Valid @RequestBody CreatePayrollConceptRequest request
    ) {
        // Un concepto nace sin nombre, y la respuesta lo dice con un nulo. Es el caso que va a
        // existir el dia que alguien anada un concepto y se olvide del literal.
        return withLabel(ruleSystemCode,
                createPayrollConceptUseCase.create(assembler.toCommand(ruleSystemCode, request)));
    }

    @PatchMapping("/{conceptCode}/summary")
    public PayrollConceptDesignerResponse updateSummary(
            @PathVariable String ruleSystemCode,
            @PathVariable String conceptCode,
            @RequestBody UpdateConceptSummaryRequest request
    ) {
        return withLabel(ruleSystemCode,
                updateConceptSummaryUseCase.update(
                        new UpdateConceptSummaryCommand(ruleSystemCode, conceptCode, request.summary())));
    }

    /**
     * Cambia el nombre del concepto en el catalogo.
     *
     * <p>No mueve ningun recibo ya calculado: el literal de una linea se congelo al calcularla.
     * Lo que cambia aqui es lo que dira el proximo calculo ({@code backend#109}).
     */
    @PatchMapping("/{conceptCode}/label")
    public PayrollConceptDesignerResponse updateLabel(
            @PathVariable String ruleSystemCode,
            @PathVariable String conceptCode,
            @RequestBody UpdateConceptLabelRequest request
    ) {
        PayrollConcept concept = updateConceptLabelUseCase.update(
                new UpdateConceptLabelCommand(ruleSystemCode, conceptCode, request.label()));
        return assembler.toResponse(concept, request.label());
    }

    private PayrollConceptDesignerResponse withLabel(String ruleSystemCode, PayrollConcept concept) {
        return assembler.toResponse(
                concept,
                getConceptLabelsUseCase.byRuleSystemCode(ruleSystemCode).get(concept.getConceptCode()));
    }

    @DeleteMapping("/{conceptCode}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable String ruleSystemCode,
            @PathVariable String conceptCode
    ) {
        deletePayrollConceptUseCase.delete(ruleSystemCode, conceptCode);
    }
}
