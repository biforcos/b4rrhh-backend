package com.b4rrhh.payroll_engine.execution.application.service;

import com.b4rrhh.payroll_engine.execution.domain.model.TechnicalConceptSegmentData;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * La base reguladora diaria que <b>viene del mes anterior</b>, o cero ({@code backend#128}).
 *
 * <p>Es la mitad de {@code BR_CC} que mira fuera del periodo. La otra mitad, {@code BR_ACT}, es la
 * base teorica de este mes, y las dos alimentan el mismo agregado: exactamente una de las dos vale
 * cero, asi que la suma es la que toca sin que ningun concepto tenga que preguntar por el caso. Es la
 * forma de las dos puertas de la prorrata (ADR-070), aplicada a otra cosa.
 *
 * <p>Vale cero en un tramo <b>trabajado</b>, y eso no es una perdida de informacion: quien lee la base
 * reguladora —la prestacion y la base durante la baja— tambien vale cero en un tramo trabajado. Lo que
 * se gana es que «tiene este empleado base reguladora» se contesta mirando el numero, sin cruzar nada
 * con las ausencias: en la semilla, los que tienen {@code BR_CC} distinto de cero son exactamente los
 * que tienen baja.
 *
 * <p>Lo que <b>no</b> hace esta clase es leer nada. La lectura del recibo de otro mes pasa por un solo
 * puerto, la resuelve la unidad antes de calcular, y este calculador recibe el numero hecho: un
 * calculador que abriera su propia consulta seria una segunda lectura sin filtro que vigilar (ADR-074).
 */
@Component
public class PreviousPeriodRegulatoryBaseConceptCalculator implements TechnicalConceptCalculator {

    @Override
    public String conceptCode() {
        return "BR_ANT";
    }

    @Override
    public BigDecimal resolve(TechnicalConceptSegmentData context) {
        if (!context.needsDailyRegulatoryBase()) {
            return BigDecimal.ZERO;
        }
        BigDecimal delMesAnterior = context.previousPeriodDailyContributionBase();
        return delMesAnterior == null ? BigDecimal.ZERO : delMesAnterior;
    }
}
