package com.b4rrhh.rulesystem.domain.model;

/**
 * Un grupo del menú de maestros (ADR-054 §5). Da el nombre y el orden que el menú
 * necesita y que en un check no cabrían; la clave ajena desde {@code rule_entity_type}
 * impone la clausura.
 */
public record RuleEntityTypeGroup(
        String code,
        String name,
        int displayOrder
) {
}
