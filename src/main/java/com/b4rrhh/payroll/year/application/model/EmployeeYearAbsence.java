package com.b4rrhh.payroll.year.application.model;

import java.time.LocalDate;

/**
 * Una ausencia que toca el año, entera aunque empiece o acabe fuera de él: la tira las pinta
 * cruzando meses, y recortarlas al año sería mentir en los bordes. {@code endDate} nulo: abierta.
 */
public record EmployeeYearAbsence(
        String absenceTypeCode,
        LocalDate startDate,
        LocalDate endDate,
        boolean benefitEntitled
) {
}
