package com.b4rrhh.payroll.year.application.model;

import java.util.List;

/**
 * El año natural de un empleado, resumido para la tira del {@code frontend#109}
 * (b4rrhh/backend#151): lo que hace falta para pintar doce meses de una vez.
 */
public record EmployeeYear(
        int year,
        List<EmployeeYearPresence> presences,
        List<EmployeeYearMonth> months,
        List<EmployeeYearAbsence> absences
) {
    public EmployeeYear {
        presences = List.copyOf(presences);
        months = List.copyOf(months);
        absences = List.copyOf(absences);
    }
}
