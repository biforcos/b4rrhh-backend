package com.b4rrhh.payroll_engine.execution.application.service;

import com.b4rrhh.payroll_engine.execution.domain.model.TechnicalConceptSegmentData;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Los dias de baja de este tramo, todos ({@code backend#129}).
 *
 * <p>Es la cantidad de la <b>base durante la baja</b>: durante la incapacidad temporal se sigue
 * cotizando, y se cotiza desde el <b>primer</b> dia, tambien los tres que no se pagan. Por eso este
 * contador no mira los tramos de porcentaje y los de la prestacion si.
 *
 * <p>Vale cero sin derecho a prestacion: sin derecho no hay prestacion <b>ni base durante la baja</b>,
 * los dias se quitan igual y el recibo lo dice (ADR-075).
 */
@Component
public class SickLeaveDaysConceptCalculator implements TechnicalConceptCalculator {

    @Override
    public String conceptCode() {
        return "D_IT_0";
    }

    @Override
    public BigDecimal resolve(TechnicalConceptSegmentData context) {
        return context.needsDailyRegulatoryBase()
                ? BigDecimal.valueOf(context.daysInSegment())
                : BigDecimal.ZERO;
    }
}
