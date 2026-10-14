package com.b4rrhh.employee.presence.application.usecase;

import com.b4rrhh.employee.shared.application.port.PresenceRectification;

import java.time.LocalDate;

/**
 * Rectificar la fecha de alta o de cese de una presencia (backend#172).
 *
 * @param dryRun       sólo decir lo que movería, sin escribir: lo que enseña la pantalla antes de confirmar
 * @param rectifiedBy  quién, para el rastro
 */
public record RectifyPresenceDateCommand(
        String ruleSystemCode,
        String employeeTypeCode,
        String employeeNumber,
        Integer presenceNumber,
        PresenceRectification.Field field,
        LocalDate newDate,
        boolean dryRun,
        String rectifiedBy
) {
}
