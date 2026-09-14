package com.b4rrhh.payroll.application.usecase;

import com.b4rrhh.payroll.application.port.PayrollCalculationStep;
import com.b4rrhh.payroll.application.port.PayrollCalculationStepReadPort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Resuelve la dirección del recibo primero y sus pasos después ({@code backend#97}).
 *
 * <p>Los dos pasos van en este orden y no al revés: preguntar por los pasos de un id que no existe
 * devolvería lista vacía, y entonces «este recibo no existe» y «este recibo se calculó antes de la
 * {@code V129}» serían la misma respuesta. Quien resuelve la dirección es
 * {@link GetPayrollByBusinessKeyUseCase}, que ya normaliza y valida la clave de negocio: repetir
 * aquí ese recorte y ese {@code toUpperCase} sería tener dos sitios donde decidir qué dirección es
 * válida, y el día que uno cambie la ficha y los pasos dejarían de hablar del mismo recibo.
 */
@Service
public class ListPayrollCalculationStepsService implements ListPayrollCalculationStepsUseCase {

    private final GetPayrollByBusinessKeyUseCase getPayrollByBusinessKeyUseCase;
    private final PayrollCalculationStepReadPort payrollCalculationStepReadPort;

    public ListPayrollCalculationStepsService(
            GetPayrollByBusinessKeyUseCase getPayrollByBusinessKeyUseCase,
            PayrollCalculationStepReadPort payrollCalculationStepReadPort
    ) {
        this.getPayrollByBusinessKeyUseCase = getPayrollByBusinessKeyUseCase;
        this.payrollCalculationStepReadPort = payrollCalculationStepReadPort;
    }

    @Override
    public Optional<List<PayrollCalculationStep>> listByPayrollBusinessKey(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber,
            String payrollPeriodCode,
            String payrollTypeCode,
            Integer presenceNumber
    ) {
        return getPayrollByBusinessKeyUseCase.getByBusinessKey(
                ruleSystemCode,
                employeeTypeCode,
                employeeNumber,
                payrollPeriodCode,
                payrollTypeCode,
                presenceNumber
        ).map(payroll -> payrollCalculationStepReadPort.findStepsOf(payroll.getId()));
    }
}
