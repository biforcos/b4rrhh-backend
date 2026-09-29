package com.b4rrhh.payroll.year.application.model;

import java.time.LocalDate;

/** Un tramo de presencia que toca el año, entero. {@code endDate} nulo: sigue abierto. */
public record EmployeeYearPresence(int presenceNumber, LocalDate startDate, LocalDate endDate) {
}
