package com.b4rrhh.payroll.infrastructure.web.assembler;

import com.b4rrhh.payroll.application.port.PayrollCalculationStep;
import com.b4rrhh.payroll.infrastructure.web.dto.PayrollCalculationStepResponse;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Pasa los pasos a DTO sin reordenar y sin agrupar ({@code backend#97}).
 *
 * <p>Lo que llega ya viene en orden de ejecución desde la consulta, y aquí sólo se traduce: un
 * {@code sorted()} o un {@code Collectors.toMap} por concepto en este punto sería el único sitio
 * del camino capaz de destruir lo que el endpoint sirve.
 */
@Component
public class PayrollCalculationStepResponseAssembler {

    public List<PayrollCalculationStepResponse> toResponse(List<PayrollCalculationStep> steps) {
        return steps.stream().map(PayrollCalculationStepResponseAssembler::toItem).toList();
    }

    private static PayrollCalculationStepResponse toItem(PayrollCalculationStep step) {
        return new PayrollCalculationStepResponse(
                step.executionOrder(),
                step.conceptCode(),
                step.conceptMnemonic(),
                step.calculationType(),
                step.functionalNature(),
                step.executionScope(),
                step.segmentStartDate(),
                step.segmentEndDate(),
                step.amount(),
                step.quantity(),
                step.rate(),
                step.payslipOrderCode(),
                step.payslipLineNumber()
        );
    }
}
