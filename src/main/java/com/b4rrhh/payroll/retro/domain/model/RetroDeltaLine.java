package com.b4rrhh.payroll.retro.domain.model;

import java.math.BigDecimal;

/**
 * Una linea de atraso: la diferencia de un concepto de un mes anterior ({@code backend#133}).
 *
 * @param originPeriodCode el mes al que pertenece esta linea. Es lo que va a
 *        {@code payroll_concept.origin_period_code}, la columna que existia desde la V53 y que no llenaba
 *        nadie
 * @param amount la diferencia, que puede ser <b>negativa</b>: unas horas que se borraron son dinero que
 *        se cobro de mas
 */
public record RetroDeltaLine(
        String originPeriodCode,
        String conceptCode,
        String conceptMnemonic,
        String conceptLabel,
        BigDecimal amount,
        BigDecimal quantity,
        BigDecimal rate,
        String conceptNatureCode,
        Integer displayOrder,
        String payslipSectionCode,
        String payslipSubsectionCode
) {
}
