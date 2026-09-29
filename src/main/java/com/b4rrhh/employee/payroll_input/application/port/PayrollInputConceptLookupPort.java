package com.b4rrhh.employee.payroll_input.application.port;

import java.util.Optional;

/**
 * Cómo se calcula un concepto de nómina, para saber si admite entradas (b4rrhh/backend#142).
 * Vacío si el concepto no existe en el sistema de reglas.
 */
public interface PayrollInputConceptLookupPort {

    Optional<String> calculationTypeOf(String ruleSystemCode, String conceptCode);
}
