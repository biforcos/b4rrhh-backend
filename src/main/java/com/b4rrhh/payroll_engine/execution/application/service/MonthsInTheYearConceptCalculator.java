package com.b4rrhh.payroll_engine.execution.application.service;

import com.b4rrhh.payroll_engine.execution.domain.model.TechnicalConceptSegmentData;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Los meses del ano: doce ({@code backend#119}).
 *
 * <p>Es el divisor de la prorrata. Existe como concepto para que <b>el nodo se vea en el
 * grafo</b> —quien mire la prorrata ve de que depende sin abrir el codigo—, pero el doce lo
 * pone esta clase: es {@code ENGINE_PROVIDED}, igual que {@code D02}, los dias del mes. El
 * valor es del calendario y lo provee el motor como provee los dias del mes (ADR-070 §1).
 *
 * <p>Y asi tiene que ser: los meses del ano no son parametrizacion. Nadie debe poder ponerle
 * catorce a este divisor para que las catorce pagas de un convenio «cuadren». Cuantas pagas
 * hay y cuanto valen si es catalogo ({@code PE_1} a {@code PE_4}, {@code backend#117}); el
 * doce entre el que se reparten, no.
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
