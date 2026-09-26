package com.b4rrhh.payroll_engine.execution.application.service;

import com.b4rrhh.payroll_engine.execution.domain.model.TechnicalConceptSegmentData;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Uno si hay que usar la base reguladora <b>teorica de este mes</b>, cero si no ({@code backend#128}).
 *
 * <p>Es la puerta de la segunda mitad de {@code BR_CC}: multiplica a {@code BR_TEO}, que es la base
 * diaria que el propio grafo calcula a partir del precio del dia y de las pagas extras del convenio.
 * Vale uno cuando este tramo es de baja por enfermedad comun <b>y</b> no hay recibo cerrado del mes
 * anterior del que leer la base.
 *
 * <p>Su complementaria es {@code BR_ANT}, y las dos nunca valen algo a la vez: donde una aporta, la
 * otra es cero, y {@code BR_CC} es la suma. Que el motor no tenga que saber que existe un «caso sin
 * recibo anterior» es el punto entero — sabe sumar, y quien contesta que vale este coeficiente es este
 * calculador a partir del tramo.
 *
 * <p>El tercer caso —hay recibo del mes anterior y todavia puede cambiar— no aparece aqui, y no puede:
 * ese recibo no se calcula (ADR-074 §2). Si llegara hasta este punto, este coeficiente valdria uno y se
 * pagaria una prestacion sobre un numero inventado; no llega porque la unidad se para antes.
 */
@Component
public class NoPreviousPayrollGateConceptCalculator implements TechnicalConceptCalculator {

    @Override
    public String conceptCode() {
        return "J_SIN_ANT";
    }

    @Override
    public BigDecimal resolve(TechnicalConceptSegmentData context) {
        if (!context.needsDailyRegulatoryBase()) {
            return BigDecimal.ZERO;
        }
        return context.previousPeriodDailyContributionBase() == null
                ? BigDecimal.ONE
                : BigDecimal.ZERO;
    }
}
