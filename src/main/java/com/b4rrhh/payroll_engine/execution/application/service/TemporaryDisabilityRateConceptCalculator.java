package com.b4rrhh.payroll_engine.execution.application.service;

import com.b4rrhh.payroll_engine.execution.domain.model.ItPrestacionTramo;
import com.b4rrhh.payroll_engine.execution.domain.model.TechnicalConceptSegmentData;
import com.b4rrhh.payroll_engine.execution.domain.port.ItPrestacionTramoRepository;

import java.math.BigDecimal;

/**
 * El porcentaje de la base reguladora que se paga en un tramo de la baja ({@code backend#129}).
 *
 * <p>Sale de la misma fila que los dias del tramo, y por eso hay <b>uno por tramo</b> y no uno por
 * porcentaje: hoy dos tramos valen los dos el 60 % —del dia 4 al 15 a cargo de la empresa y del 16 al
 * 20 en pago delegado— y compartir un concepto entre los dos ataria el importe del segundo a la fila
 * del primero. El dia que una norma cambie uno y no el otro, cambiaria el que no toca.
 *
 * <p>Es {@code PERIOD} y no de tramo a proposito: un porcentaje legal no depende de como se parta el
 * mes. Lo que depende del tramo son los dias, y esos los cuenta
 * {@link TemporaryDisabilityDaysConceptCalculator}.
 */
public class TemporaryDisabilityRateConceptCalculator implements TechnicalConceptCalculator {

    private final String conceptCode;
    private final String absenceTypeCode;
    private final String tramoCode;
    private final ItPrestacionTramoRepository tramos;

    public TemporaryDisabilityRateConceptCalculator(
            String conceptCode,
            String absenceTypeCode,
            String tramoCode,
            ItPrestacionTramoRepository tramos
    ) {
        this.conceptCode = conceptCode;
        this.absenceTypeCode = absenceTypeCode;
        this.tramoCode = tramoCode;
        this.tramos = tramos;
    }

    @Override
    public String conceptCode() {
        return conceptCode;
    }

    @Override
    public BigDecimal resolve(TechnicalConceptSegmentData context) {
        return tramos.findActive(context.ruleSystemCode(), absenceTypeCode, tramoCode,
                        context.periodEnd())
                .map(ItPrestacionTramo::percentage)
                .orElseThrow(() -> new IllegalStateException(
                        "No hay tramo de prestacion " + tramoCode + " para " + absenceTypeCode
                        + " vigente el " + context.periodEnd() + ": el catalogo no declara a que"
                        + " porcentaje se paga esta baja"));
    }
}
