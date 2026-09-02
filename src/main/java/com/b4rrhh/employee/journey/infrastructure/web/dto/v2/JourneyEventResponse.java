package com.b4rrhh.employee.journey.infrastructure.web.dto.v2;

import java.time.LocalDate;
import java.util.Map;

public record JourneyEventResponse(
        LocalDate eventDate,
        String eventType,
        String trackCode,
        String status,
        boolean isCurrent,
        Map<String, Object> details
) {
}