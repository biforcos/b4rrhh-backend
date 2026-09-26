package com.b4rrhh.payroll.application.usecase;

import com.b4rrhh.payroll.domain.model.Payroll;
import com.b4rrhh.payroll.retro.domain.model.CurrentCalculation;

public interface CalculatePayrollUnitUseCase {

    /** Calcula la unidad y escribe su <b>recibo</b>, que es el camino de siempre. */
    Payroll calculate(CalculatePayrollUnitCommand command);

    /**
     * Calcula un mes <b>cerrado</b> y escribe su <b>calculo vigente</b>, sin tocar su recibo
     * ({@code backend#131}, ADR-076).
     *
     * <p>Es el mismo calculo con otro final: el mismo grafo, los mismos tramos y las reglas vigentes en
     * ese mes. Lo que cambia es que el resultado no es un documento y que lo que lee de otro mes lo lee
     * del vigente.
     *
     * <p>Dos metodos y no un parametro a proposito: los tipos de retorno son distintos porque las cosas
     * son distintas. Un {@code Payroll} que a veces no es un recibo seria el primer paso para que
     * alguien lo guarde como si lo fuera.
     *
     * @return el vigente escrito, o <b>vacio</b> si ese mes no se ha podido calcular — y entonces no se
     *         ha escrito nada en ninguna parte, empezando por el recibo
     */
    CurrentCalculationOutcome calculateCurrent(CalculatePayrollUnitCommand command);

    /**
     * Lo que sale de recalcular un mes cerrado.
     *
     * @param calculation el vigente escrito, o {@code null} si no se pudo calcular
     * @param notCalculatedReason el motivo cuando no se pudo, o {@code null}. Va aqui y <b>no</b> a un
     *        recibo {@code NOT_VALID} como en el camino normal: en modo retro el recibo de aquel mes
     *        esta entregado y no se toca ni para decir que algo ha ido mal. Quien lanza lo cuenta en
     *        los mensajes de la corrida
     */
    record CurrentCalculationOutcome(CurrentCalculation calculation, String notCalculatedReason) {

        public static CurrentCalculationOutcome written(CurrentCalculation calculation) {
            return new CurrentCalculationOutcome(calculation, null);
        }

        public static CurrentCalculationOutcome notCalculated(String reason) {
            return new CurrentCalculationOutcome(null, reason);
        }

        public boolean wasCalculated() {
            return calculation != null;
        }
    }
}
