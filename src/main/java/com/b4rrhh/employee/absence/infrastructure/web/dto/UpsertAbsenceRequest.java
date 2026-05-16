package com.b4rrhh.employee.absence.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;

public record UpsertAbsenceRequest(
    @JsonProperty("endDate") LocalDate endDate,
    @JsonProperty("endTime") String endTime
) {}
