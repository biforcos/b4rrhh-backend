package com.b4rrhh.payroll_engine.execution.application.service;

import com.b4rrhh.payroll_engine.execution.domain.model.TechnicalConceptSegmentData;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Uno si en este tramo las pagas extras se prorratean, cero si no ({@code backend#119}).
 *
 * <p>Es la puerta por la que la prorrata entra en los devengos. Multiplicando por uno o por
 * cero, el motor no tiene que saber que existe un regimen de pagas extras: sabe multiplicar,
 * y quien contesta que vale este coeficiente es este calculador a partir del tramo.
 *
 * <p>La alternativa era una condicion en {@code concept_assignment}, y no vale: la asignacion
 * se resuelve UNA VEZ por periodo —el plan de conceptos se arma antes de partir el mes— y el
 * regimen puede cambiar a mitad de mes. Un recibo partido necesita las dos respuestas, y la
 * asignacion solo sabe dar una (ADR-070).
 */
@Component
public class ProratedExtraPaymentsGateConceptCalculator implements TechnicalConceptCalculator {

    @Override
    public String conceptCode() {
        return "J_PRORRATEADAS";
    }

    @Override
    public BigDecimal resolve(TechnicalConceptSegmentData context) {
        return context.extraPaymentsProrated() ? BigDecimal.ONE : BigDecimal.ZERO;
    }
}
