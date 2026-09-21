package com.b4rrhh.payroll_engine.execution.application.service;

import com.b4rrhh.payroll_engine.execution.domain.model.TechnicalConceptSegmentData;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Los meses del ano: doce ({@code backend#119}).
 *
 * <p>Es el divisor de la prorrata, y existe como concepto en vez de como constante dentro de
 * un resolutor porque en este motor <b>lo que interviene en un calculo se ve en el grafo</b>.
 * Escondido en Java, la pregunta «de donde sale este numero» tendria una respuesta que solo
 * esta en el codigo, que es justo lo que el camino 1 quito de en medio.
 *
 * <p>No depende del tramo: doce son doce en enero y en un mes partido. Por eso es
 * {@code PERIOD} y un concepto de tramo lo puede leer, que es la direccion permitida
 * (ADR-058).
 */
@Component
public class MonthsInTheYearConceptCalculator implements TechnicalConceptCalculator {

    @Override
    public String conceptCode() {
        return "P_MESES_ANO";
    }

    @Override
    public BigDecimal resolve(TechnicalConceptSegmentData context) {
        return BigDecimal.valueOf(12);
    }
}
