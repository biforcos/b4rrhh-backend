package com.b4rrhh.employee.journey.application.usecase;

import java.time.LocalDate;
import java.util.Map;

/**
 * Un evento de la línea temporal: tipo, pista, fecha, estado y datos. Sin título ni
 * subtítulo: rotular el tipo de evento es vocabulario de interfaz y lo pone el cliente en su
 * idioma (ADR-052 §4, backend#40); lo que decían esas frases va entero en {@code details}.
 */
public record JourneyEventView(
        LocalDate eventDate,
        JourneyEventType eventType,
        JourneyTrackCode trackCode,
        JourneyEventStatus status,
        boolean isCurrent,
        Map<String, Object> details
) {
}