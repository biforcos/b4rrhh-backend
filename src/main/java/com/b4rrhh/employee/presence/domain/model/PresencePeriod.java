package com.b4rrhh.employee.presence.domain.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Las fechas de una presencia, que es todo lo que el estado del empleado necesita de ella. Un fin
 * nulo es una presencia abierta; el fin, cuando lo hay, es el último día de la presencia.
 */
public record PresencePeriod(LocalDate startDate, LocalDate endDate) {

    public PresencePeriod {
        Objects.requireNonNull(startDate, "startDate is required");
    }

    boolean covers(LocalDate date) {
        return !startDate.isAfter(date) && (endDate == null || !endDate.isBefore(date));
    }
}
