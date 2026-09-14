package com.b4rrhh.payroll.infrastructure.persistence;

import com.b4rrhh.payroll.application.port.PayrollCalculationStep;
import com.b4rrhh.payroll.application.port.PayrollCalculationStepReadPort;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Lee los pasos de un recibo, en orden de ejecución y sin tocar nada más ({@code backend#97}).
 *
 * <p>Por consulta propia y no por el agregado {@code PayrollEntity}: los pasos no son líneas de
 * recibo y no cuelgan de él —no hay {@code @ManyToOne} hacia el recibo, sólo la clave ajena con
 * {@code on delete cascade}—, así que traerlos con la ficha los metería en cada apertura para que
 * casi nadie los mire. La Valorización es un cajón que se abre a demanda y ése es el momento de
 * pedirlos.
 */
@Component
public class PayrollCalculationStepReadAdapter implements PayrollCalculationStepReadPort {

    private final SpringDataPayrollCalculationStepRepository repository;

    public PayrollCalculationStepReadAdapter(SpringDataPayrollCalculationStepRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<PayrollCalculationStep> findStepsOf(long payrollId) {
        return repository.findByPayrollIdOrderByExecutionOrderAsc(payrollId).stream()
                .map(PayrollCalculationStepReadAdapter::toStep)
                .toList();
    }

    private static PayrollCalculationStep toStep(PayrollCalculationStepEntity entity) {
        return new PayrollCalculationStep(
                entity.getExecutionOrder(),
                entity.getConceptCode(),
                entity.getConceptMnemonic(),
                entity.getCalculationType(),
                entity.getFunctionalNature(),
                entity.getExecutionScope(),
                entity.getSegmentStartDate(),
                entity.getSegmentEndDate(),
                entity.getAmount(),
                entity.getQuantity(),
                entity.getRate(),
                entity.getPayslipOrderCode()
        );
    }
}
