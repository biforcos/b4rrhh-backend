package com.b4rrhh.employee.presence.infrastructure.web.dto;

import java.time.LocalDate;

/** La fecha que tenía que haber sido y, con {@code dryRun}, sólo preguntar qué movería (backend#172). */
public record RectifyPresenceDateRequest(LocalDate newDate, Boolean dryRun) {
}
