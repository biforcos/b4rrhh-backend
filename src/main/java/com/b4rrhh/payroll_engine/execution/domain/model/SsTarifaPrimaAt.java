package com.b4rrhh.payroll_engine.execution.domain.model;

import java.math.BigDecimal;

/**
 * Una entrada de la tarifa de primas de accidentes de trabajo ({@code backend#122}).
 *
 * <p>Los dos tipos van por separado porque la norma los declara por separado —incapacidad
 * temporal e incapacidad permanente, muerte y supervivencia— y porque la cobertura de cada uno se
 * puede tener concertada con una entidad distinta. Lo que se paga es la suma, y sumarlos es de
 * quien calcula la cuota, no de quien guarda la tarifa.
 *
 * @param cnaeCode el CNAE de la entrada, que puede ser mas corto que el de la empresa.
 */
public record SsTarifaPrimaAt(
        String cnaeCode,
        String activityName,
        BigDecimal tipoIt,
        BigDecimal tipoIms
) {

    /** El tipo que se aplica: los dos sumados. */
    public BigDecimal tipoTotal() {
        return tipoIt.add(tipoIms);
    }
}
