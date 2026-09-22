package com.b4rrhh.payroll_engine.execution.application.service;

import com.b4rrhh.payroll_engine.execution.domain.model.TechnicalConceptSegmentData;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.temporal.ChronoUnit;

/**
 * La parte de un tope que le toca a un tramo (ADR-048).
 *
 * <p>Estaba escrita dos veces, identica, en los dos calculadores de topes. Con cuatro
 * calculadores —dos bases por dos topes, {@code backend#121}— habrian sido cuatro copias, y la
 * cuarta es la que se queda atras.
 *
 * <p>Los topes son <b>lo unico</b> que se calcula por tramo y se acumula; todos los conceptos de
 * cotizacion son de periodo. Si alguien cambia eso sin saberlo, el recorte deja de cuadrar.
 */
final class SegmentProration {

    private SegmentProration() {
    }

    static BigDecimal prorate(BigDecimal base, TechnicalConceptSegmentData ctx) {
        if ("DIARIO".equals(ctx.tipoNomina())) {
            return base.multiply(BigDecimal.valueOf(ctx.daysInSegment())).setScale(2, RoundingMode.HALF_UP);
        }
        long daysInPeriod = ChronoUnit.DAYS.between(ctx.periodStart(), ctx.periodEnd()) + 1;
        return base.multiply(BigDecimal.valueOf(ctx.daysInSegment()))
                .divide(BigDecimal.valueOf(daysInPeriod), 2, RoundingMode.HALF_UP);
    }
}
