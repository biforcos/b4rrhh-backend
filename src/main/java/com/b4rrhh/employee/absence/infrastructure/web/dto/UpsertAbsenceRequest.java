package com.b4rrhh.employee.absence.infrastructure.web.dto;

import java.time.LocalDate;

/**
 * @param benefitEntitled si la baja lleva derecho a prestacion ({@code backend#129}). Omitirlo es
 *        dejarlo con derecho, que es el caso normal. Solo significa algo en {@code IT_COMMON}: en los
 *        demas tipos de ausencia no hay prestacion que tener derecho a.
 */
public record UpsertAbsenceRequest(LocalDate endDate, String endTime, Boolean benefitEntitled) {}
