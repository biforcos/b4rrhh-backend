package com.b4rrhh.employee.absence.infrastructure.web.dto;

import java.time.LocalDate;

public record UpsertAbsenceRequest(LocalDate endDate, String endTime) {}
