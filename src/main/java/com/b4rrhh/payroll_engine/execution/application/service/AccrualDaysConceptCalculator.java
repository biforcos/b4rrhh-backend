package com.b4rrhh.payroll_engine.execution.application.service;

import com.b4rrhh.payroll_engine.execution.domain.model.TechnicalConceptSegmentData;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * {@code ENGINE_PROVIDED} calculator for concept D01 — días de devengo mensual.
 *
 * <p>Returns the number of days in the segment that count toward monthly accrual,
 * capped at 30 per payroll convention. Employees active for the full month in a
 * 31-day month still accrue 30 days; partial-month employees accrue actual days.
 *
 * <p>Formula: {@code min(daysInSegment, 30)}
 *
 * <h2>Y cero en el tramo de ausencia ({@code backend#127})</h2>
 *
 * <p>Un tramo de baja por enfermedad comun o de permiso no retribuido <b>no devenga dias</b>. Con
 * eso solo, todo lo que cuelga de los dias cae solo: el {@code 101}, y con el las cuatro pagas
 * extras y la prorrata, que son agregados suyos. No hace falta tocar ningun otro calculador.
 *
 * <p>La decision de cuales ausencias llegan hasta aqui no esta en esta clase: un tramo trae ausencia
 * si y solo si es de las que no se pagan, y eso lo resuelve quien arma el tramo (ADR-073). Aqui la
 * pregunta es una sola —«tiene ausencia este tramo»— y por eso no hay una lista de tipos que
 * mantener en dos sitios.
 */
@Component
public class AccrualDaysConceptCalculator implements TechnicalConceptCalculator {

    private static final long PAYROLL_MONTH_CAP = 30L;

    @Override
    public String conceptCode() {
        return "D01";
    }

    @Override
    public BigDecimal resolve(TechnicalConceptSegmentData context) {
        if (context.isUnpaidAbsenceSegment()) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(Math.min(context.daysInSegment(), PAYROLL_MONTH_CAP));
    }
}
