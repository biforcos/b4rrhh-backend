package com.b4rrhh.payroll_engine.concept.application.usecase;

import java.util.Map;

public interface GetConceptLabelsUseCase {

    /**
     * Los nombres de los conceptos del sistema de reglas, indexados por codigo de concepto.
     *
     * <p>Un concepto sin nombre <b>no esta en el mapa</b>: quien lo lea ensena su mnemonico.
     */
    Map<String, String> byRuleSystemCode(String ruleSystemCode);
}
