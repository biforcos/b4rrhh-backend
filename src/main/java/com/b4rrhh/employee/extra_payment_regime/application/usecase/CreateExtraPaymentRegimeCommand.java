package com.b4rrhh.employee.extra_payment_regime.application.usecase;

import java.time.LocalDate;

/**
 * Dar de alta un tramo de regimen de pagas extras.
 *
 * @param prorated si se prorratean. <b>Nulo significa «el que diga el convenio»</b>, y es como lo
 *                 pide la contratacion: el alta no elige regimen, copia el del convenio que le
 *                 aplica al empleado ({@code backend#117}). Por el API llega siempre con valor —
 *                 quien lo pide a mano lo esta eligiendo.
 */
public record CreateExtraPaymentRegimeCommand(
        String ruleSystemCode,
        String employeeTypeCode,
        String employeeNumber,
        LocalDate startDate,
        LocalDate endDate,
        Boolean prorated
) {
}
