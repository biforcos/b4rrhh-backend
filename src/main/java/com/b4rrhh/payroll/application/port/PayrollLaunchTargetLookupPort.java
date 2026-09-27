package com.b4rrhh.payroll.application.port;

import java.util.List;

/**
 * Lo que hace falta para decir <b>que no se ha encontrado</b> cuando un lanzamiento apunta a alguien que
 * no existe ({@code frontend#88}).
 *
 * <p>Un lanzamiento a un tipo o a un numero inventados no es algo que se descubra calculando: se contesta
 * al pedir, con un 400 que nombra lo que falta y, si es el tipo, los que hay.
 */
public interface PayrollLaunchTargetLookupPort {

    /** Los tipos de empleado del catalogo del sistema de reglas, ordenados. */
    List<String> findEmployeeTypeCodes(String ruleSystemCode);

    boolean employeeExists(String ruleSystemCode, String employeeTypeCode, String employeeNumber);
}
