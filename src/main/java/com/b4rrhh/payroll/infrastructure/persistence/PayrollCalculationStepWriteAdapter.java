package com.b4rrhh.payroll.infrastructure.persistence;

import com.b4rrhh.payroll.application.port.PayrollCalculationStep;
import com.b4rrhh.payroll.application.port.PayrollCalculationStepWritePort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Escribe los pasos de un recibo recién calculado ({@code backend#93}).
 *
 * <p>Con {@code persist} y no con un {@code save} de Spring Data: la clave es asignada
 * —{@code (payroll_id, execution_order)}— y un {@code save} tendría que preguntar primero si la
 * fila existe, que son 35 o 39 selects por recibo para insertar 35 o 39. Aquí sabemos que son
 * altas: el recibo acaba de nacer.
 *
 * <p><b>Y con {@code flush} al final, que no es adorno.</b> Una clave asignada hace que el
 * {@code insert} sea diferido —a diferencia de las líneas del recibo, que llevan {@code identity}
 * y se insertan al persistir—, y medido: en el recálculo puntual, donde la transacción la abre
 * {@code RecalculatePayrollService} y no el cálculo de la unidad, los pasos se quedaban pendientes
 * y no llegaban a la base. El recibo salía con sus 14 líneas y con cero pasos. Flushear aquí
 * también hace que una violación de la clave o del {@code check} del ámbito salte en esta llamada
 * y no en un commit lejano.
 */
@Component
public class PayrollCalculationStepWriteAdapter implements PayrollCalculationStepWritePort {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public void writeStepsOf(long payrollId, List<PayrollCalculationStep> steps) {
        for (PayrollCalculationStep step : steps) {
            entityManager.persist(toEntity(payrollId, step));
        }
        entityManager.flush();
    }

    private PayrollCalculationStepEntity toEntity(long payrollId, PayrollCalculationStep step) {
        PayrollCalculationStepEntity entity = new PayrollCalculationStepEntity();
        entity.setPayrollId(payrollId);
        entity.setExecutionOrder(step.executionOrder());
        entity.setConceptCode(step.conceptCode());
        entity.setConceptMnemonic(step.conceptMnemonic());
        entity.setCalculationType(step.calculationType());
        entity.setFunctionalNature(step.functionalNature());
        entity.setExecutionScope(step.executionScope());
        entity.setSegmentStartDate(step.segmentStartDate());
        entity.setSegmentEndDate(step.segmentEndDate());
        entity.setAmount(step.amount());
        entity.setQuantity(step.quantity());
        entity.setRate(step.rate());
        entity.setPayslipOrderCode(step.payslipOrderCode());
        return entity;
    }
}
