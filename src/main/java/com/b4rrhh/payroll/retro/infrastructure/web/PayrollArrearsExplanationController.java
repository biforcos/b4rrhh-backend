package com.b4rrhh.payroll.retro.infrastructure.web;

import com.b4rrhh.payroll.retro.application.usecase.ExplainPayrollArrearsCommand;
import com.b4rrhh.payroll.retro.application.usecase.ExplainPayrollArrearsUseCase;
import com.b4rrhh.payroll.retro.infrastructure.web.dto.ArrearExplanationResponse;
import com.b4rrhh.payroll.retro.infrastructure.web.dto.ArrearPaidInResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * De donde sale cada linea de atraso de un recibo ({@code backend#134}).
 *
 * <p>Un endpoint por recibo y no uno por linea: quien lo pide es la pantalla del recibo, y las quiere
 * todas. Una llamada por linea seria una llamada por cada mes de origen para pintar una tabla.
 *
 * <p>Va junto a los pasos y no dentro de ellos, y eso es la decision: una linea de atraso <b>no viene de
 * ningun paso</b> de este calculo (su {@code mergedStepCount} es cero), asi que meterla en la lista de
 * pasos habria obligado a inventar pasos que no existen. Lo que tiene que contar son tres importes.
 */
@RestController
@RequestMapping("/payrolls/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}"
        + "/{payrollPeriodCode}/{payrollTypeCode}/{presenceNumber}/arrears-explanation")
public class PayrollArrearsExplanationController {

    private final ExplainPayrollArrearsUseCase explain;

    public PayrollArrearsExplanationController(ExplainPayrollArrearsUseCase explain) {
        this.explain = explain;
    }

    @GetMapping
    public List<ArrearExplanationResponse> explain(
            @PathVariable String ruleSystemCode,
            @PathVariable String employeeTypeCode,
            @PathVariable String employeeNumber,
            @PathVariable String payrollPeriodCode,
            @PathVariable String payrollTypeCode,
            @PathVariable Integer presenceNumber
    ) {
        return explain.explain(new ExplainPayrollArrearsCommand(
                        ruleSystemCode, employeeTypeCode, employeeNumber,
                        payrollPeriodCode, payrollTypeCode, presenceNumber))
                .stream()
                .map(e -> new ArrearExplanationResponse(
                        e.originPeriodCode(),
                        e.conceptCode(),
                        e.conceptLabel(),
                        e.lineAmount(),
                        e.currentValue(),
                        e.currentValueCalculatedAt(),
                        e.alreadyPaid(),
                        e.paidIn().stream()
                                .map(p -> new ArrearPaidInResponse(p.payrollPeriodCode(), p.amount()))
                                .toList(),
                        e.difference(),
                        e.addsUp()))
                .toList();
    }
}
