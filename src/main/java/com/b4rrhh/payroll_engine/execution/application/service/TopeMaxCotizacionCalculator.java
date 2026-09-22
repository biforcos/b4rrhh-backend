package com.b4rrhh.payroll_engine.execution.application.service;

import com.b4rrhh.payroll_engine.execution.domain.model.SsCotizacionTope;
import com.b4rrhh.payroll_engine.execution.domain.model.TechnicalConceptSegmentData;
import com.b4rrhh.payroll_engine.execution.domain.port.SsCotizacionTopesRepository;

import java.math.BigDecimal;

/**
 * El tope maximo de una base de cotizacion, prorrateado al tramo.
 *
 * <p>Deja de ser un {@code @Component} con el codigo de concepto escrito dentro y pasa a llevar
 * su contingencia en el constructor ({@code backend#121}): hay dos bases con topes —comunes y
 * profesionales— y el dia que la clase decidiera por su cuenta cual de las dos mira, habria que
 * escribir una clase mas por cada base. Quien empareja concepto y contingencia es
 * {@link SsCotizacionTopeCalculators}, igual que {@link SsCotizacionRateCalculators} empareja
 * concepto y tipo desde el {@code backend#105}.
 */
public class TopeMaxCotizacionCalculator implements TechnicalConceptCalculator {

    private final String conceptCode;
    private final String contingencyCode;
    private final SsCotizacionTopesRepository topesRepository;

    public TopeMaxCotizacionCalculator(
            String conceptCode, String contingencyCode, SsCotizacionTopesRepository topesRepository) {
        this.conceptCode = conceptCode;
        this.contingencyCode = contingencyCode;
        this.topesRepository = topesRepository;
    }

    @Override
    public String conceptCode() { return conceptCode; }

    @Override
    public BigDecimal resolve(TechnicalConceptSegmentData context) {
        BigDecimal baseMax = topesRepository.findActive(
                        context.ruleSystemCode(),
                        context.grupoCotizacionCode(),
                        context.tipoNomina(),
                        contingencyCode,
                        context.periodEnd())
                .map(SsCotizacionTope::baseMax)
                .orElseThrow(() -> new IllegalStateException(
                        "No ss_cotizacion_topes entry found for grupo=" + context.grupoCotizacionCode()
                        + " tipoNomina=" + context.tipoNomina()
                        + " contingencia=" + contingencyCode
                        + " referenceDate=" + context.periodEnd()));
        return SegmentProration.prorate(baseMax, context);
    }
}
