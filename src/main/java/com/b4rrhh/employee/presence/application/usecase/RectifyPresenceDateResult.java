package com.b4rrhh.employee.presence.application.usecase;

import com.b4rrhh.employee.shared.application.port.PresenceRectification;
import com.b4rrhh.employee.shared.application.port.PresenceRectificationPlan;

import java.time.LocalDate;
import java.util.List;

/** Lo que se movió (o se movería, si era un ensayo) al rectificar una presencia (backend#172). */
public record RectifyPresenceDateResult(
        Integer presenceNumber,
        PresenceRectification.Field field,
        LocalDate oldDate,
        LocalDate newDate,
        boolean dryRun,
        List<PresenceRectificationPlan.Move> moves
) {
}
