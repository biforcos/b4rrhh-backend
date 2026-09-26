package com.b4rrhh.employee.absence.infrastructure.web.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AbsenceResponse(
    String absenceTypeCode,
    LocalDate startDate,
    String startTime,
    LocalDate endDate,
    String endTime,
    /** Si la baja lleva derecho a prestacion ({@code backend#129}). */
    boolean benefitEntitled,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
