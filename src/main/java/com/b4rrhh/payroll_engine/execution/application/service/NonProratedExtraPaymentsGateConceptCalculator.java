package com.b4rrhh.payroll_engine.execution.application.service;

import com.b4rrhh.payroll_engine.execution.domain.model.TechnicalConceptSegmentData;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * El complemento de {@link ProratedExtraPaymentsGateConceptCalculator}: la puerta de la
 * prorrata que <b>cotiza y no se paga</b> ({@code backend#119}).
 *
 * <p>Son dos conceptos y no uno con el signo cambiado porque lo que hace cierta la invariante
 * del paso 4 es que <b>los dos alimenten a {@code B01}</b> y que sus coeficientes sumen uno
 * siempre. Escrito asi, {@code B01} recibe la prorrata exactamente una vez sea cual sea el
 * regimen: la base no sabe si se pago, <b>por construccion</b> y no por cuidado.
 */
@Component
public class NonProratedExtraPaymentsGateConceptCalculator implements TechnicalConceptCalculator {

    @Override
    public String conceptCode() {
        return "J_NO_PRORRATEADAS";
    }

    @Override
    public BigDecimal resolve(TechnicalConceptSegmentData context) {
        return context.extraPaymentsProrated() ? BigDecimal.ZERO : BigDecimal.ONE;
    }
}
