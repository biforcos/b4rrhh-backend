package com.b4rrhh.rulesystem.translation.domain.model;

/**
 * El nombre de un {@code rule_entity_type} en un idioma concreto (backend#152). Es la gemela
 * de {@link RuleEntityTranslation}: un tipo no es una {@code rule_entity}, así que no cabe en
 * su tabla. Cuelga del código del tipo, que es global —no hay reglamentación que separar— y
 * es su clave de negocio (ADR-054 §1).
 */
public record RuleEntityTypeTranslation(
        String ruleEntityTypeCode,
        String languageCode,
        String name
) {
}
