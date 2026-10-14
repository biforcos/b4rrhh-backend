package com.b4rrhh.employee.presence.infrastructure.web.dto;

import java.time.LocalDate;
import java.util.List;

/** Lo que se movió, o se movería, al rectificar una presencia (backend#172). */
public record RectifyPresenceDateResponse(
        Integer presenceNumber,
        String field,
        LocalDate oldDate,
        LocalDate newDate,
        boolean dryRun,
        List<Move> moves
) {

    public record Move(
            String vertical,
            String label,
            LocalDate startDate,
            LocalDate endDate,
            LocalDate newStartDate,
            LocalDate newEndDate
    ) {
    }
}
