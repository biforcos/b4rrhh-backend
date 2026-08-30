package com.b4rrhh.rulesystem.translation.domain.model;

/**
 * El literal de un {@code rule_entity} en un idioma concreto. Cuelga del id de la fila, no
 * de {@code (tipo, código)}: la clave de {@code rule_entity} ya es por reglamentación, y
 * traducir por código afirmaría algo sobre la regulación, no sobre el idioma (ADR-052 §1).
 */
public record RuleEntityTranslation(
        Long ruleEntityId,
        String languageCode,
        String name,
        String description
) {
}
