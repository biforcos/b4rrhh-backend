package com.b4rrhh.payroll.retro.application.port;

/** Si el empleado existe, para no contestar «no tiene marcas» de uno que no existe (b4rrhh/backend#144). */
public interface RetroMarkEmployeeLookupPort {

    boolean exists(String ruleSystemCode, String employeeTypeCode, String employeeNumber);
}
