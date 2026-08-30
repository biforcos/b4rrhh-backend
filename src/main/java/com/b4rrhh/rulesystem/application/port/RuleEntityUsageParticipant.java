package com.b4rrhh.rulesystem.application.port;

/**
 * Cada vertical que guarda códigos de catálogo por texto declara aquí dónde los guarda, y el
 * adaptador de {@code rulesystem} sólo suma. Mismo patrón que {@code HireParticipant}
 * (ADR-047): un vertical nuevo se engancha solo con implementarlo, sin tocar ningún registro
 * central que alguien tenga que acordarse de mantener (backend#28).
 */
public interface RuleEntityUsageParticipant {

    /** Nombre del recurso en plural, como sale en la API: {@code presences}. */
    String resource();

    /**
     * Cuántas filas del recurso, históricas incluidas, referencian el código. Cero si el tipo
     * no es de los que este recurso guarda.
     */
    long countReferences(String ruleSystemCode, String ruleEntityTypeCode, String code);
}
