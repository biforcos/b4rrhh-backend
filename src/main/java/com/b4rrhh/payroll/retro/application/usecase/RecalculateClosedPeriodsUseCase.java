package com.b4rrhh.payroll.retro.application.usecase;

import com.b4rrhh.payroll.retro.domain.model.CurrentCalculation;

import java.util.List;

public interface RecalculateClosedPeriodsUseCase {

    RecalculateClosedPeriodsResult recalculate(RecalculateClosedPeriodsCommand command);

    /**
     * @param written   los vigentes escritos, del mes mas antiguo al mas reciente
     * @param notCalculated los meses que no se pudieron calcular, con su motivo. <b>No se paran los
     *        siguientes</b> cuando uno falla, y hay que decir por que: los meses de un tramo no son
     *        independientes —agosto lee julio— pero tampoco lo son todos entre si, y abortar el tramo
     *        entero por un mes dejaria sin recalcular meses que si se podian
     */
    record RecalculateClosedPeriodsResult(
            List<CurrentCalculation> written,
            List<NotCalculatedMonth> notCalculated
    ) {
        public record NotCalculatedMonth(String payrollPeriodCode, String reason) {
        }
    }
}
