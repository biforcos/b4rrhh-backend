package com.b4rrhh.payroll_engine.execution.application.service;

import com.b4rrhh.payroll_engine.execution.domain.model.SsCotizacionTope;
import com.b4rrhh.payroll_engine.execution.domain.model.TechnicalConceptSegmentData;
import com.b4rrhh.payroll_engine.execution.domain.port.SsCotizacionTopesRepository;

import java.math.BigDecimal;

/**
 * El tope minimo de una base de cotizacion, prorrateado al tramo.
 *
 * <p>Es el que hace falta declarar por contingencia ({@code backend#121}): el maximo es el mismo
 * para las dos bases, pero el minimo de las contingencias profesionales lo fija la Orden de
 * cotizacion y no es la base minima del grupo. Ver {@link TopeMaxCotizacionCalculator}.
 */
public class TopeMinCotizacionCalculator implements TechnicalConceptCalculator {

    private final String conceptCode;
    private final String contingencyCode;
    private final SsCotizacionTopesRepository topesRepository;

    public TopeMinCotizacionCalculator(
            String conceptCode, String contingencyCode, SsCotizacionTopesRepository topesRepository) {
        this.conceptCode = conceptCode;
        this.contingencyCode = contingencyCode;
        this.topesRepository = topesRepository;
    }

    @Override
    public String conceptCode() { return conceptCode; }

    @Override
    public BigDecimal resolve(TechnicalConceptSegmentData context) {
        BigDecimal baseMin = topesRepository.findActive(
                        context.ruleSystemCode(),
                        context.grupoCotizacionCode(),
                        context.tipoNomina(),
                        contingencyCode,
                        context.periodEnd())
                .map(SsCotizacionTope::baseMin)
                .orElseThrow(() -> new IllegalStateException(
                        "No ss_cotizacion_topes entry found for grupo=" + context.grupoCotizacionCode()
                        + " tipoNomina=" + context.tipoNomina()
                        + " contingencia=" + contingencyCode
                        + " referenceDate=" + context.periodEnd()));
        return SegmentProration.prorate(baseMin, context);
    }
}
