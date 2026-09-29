package com.b4rrhh.payroll.year.application.port;

import com.b4rrhh.payroll.year.application.model.EmployeeYearAbsence;
import com.b4rrhh.payroll.year.application.model.EmployeeYearPresence;

import java.util.List;

/**
 * Lo que el año de un empleado toma del contexto del empleado (b4rrhh/backend#151). Sale de las
 * mismas consultas que sirven la presencia, las ausencias y las entradas en sus pantallas: el año
 * compone, no vuelve a calcular.
 */
public interface EmployeeYearFactsPort {

    /** Todas sus presencias. Si el empleado no existe, {@code EmployeeYearEmployeeNotFoundException}. */
    List<EmployeeYearPresence> presences(String ruleSystemCode, String employeeTypeCode, String employeeNumber);

    /** Todas sus ausencias. */
    List<EmployeeYearAbsence> absences(String ruleSystemCode, String employeeTypeCode, String employeeNumber);

    /** Los conceptos de sus entradas de nómina de un período {@code yyyyMM}, uno por entrada. */
    List<String> payrollInputConcepts(String ruleSystemCode, String employeeTypeCode, String employeeNumber, int period);
}
