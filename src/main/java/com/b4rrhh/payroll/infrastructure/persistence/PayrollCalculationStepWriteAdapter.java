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
 * <p><b>Y sin {@code flush}, a propósito.</b> Ésta es la única entidad de {@code payroll} con
 * clave asignada, así que es la única cuyo {@code insert} queda diferido hasta el vaciado de la
 * sesión: las demás llevan {@code identity} y Hibernate tiene que insertarlas al persistir para
 * sacar el id. El commit de la transacción las vacía, y está comprobado contra la aplicación
 * arrancada, por los dos caminos —lanzamiento y recálculo puntual—: 35 pasos en la base en los
 * dos. Un {@code flush} aquí no haría la escritura más duradera; sólo adelantaría el momento.
 *
 * <p>Lo que sí hay que saber, y por eso se escribe: <b>dentro de una transacción que no hace
 * commit, esos 35 {@code insert} no existen para nadie que lea por JDBC</b>. Es el caso de los
 * tests de {@code @TestWebSobreEsquemaReal}, que van en transacción y se deshacen al terminar; por
 * eso el test de integración vacía la sesión antes de contar filas, y lo dice ahí (ADR-062).
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
        entity.setPayslipLineNumber(step.payslipLineNumber());
        return entity;
    }
}
