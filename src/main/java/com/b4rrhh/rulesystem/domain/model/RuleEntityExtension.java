package com.b4rrhh.rulesystem.domain.model;

/**
 * Una extensión declarada de un tipo de entidad (ADR-053 §2): la fila de
 * {@code rule_entity_extension} que dice que el tipo tiene más que código y literal.
 * La ausencia de filas significa «sólo raíz», y es el caso común.
 *
 * <p>De aquí deriva la navegación su pertenencia (ADR-053 §7): un tipo con extensiones
 * tiene pantalla propia; uno sin ellas vive en Catálogos.</p>
 *
 * <p>{@code cardinality} conserva la forma de la tabla ({@code 1:1} | {@code 1:N});
 * el check de la V106 es quien la restringe.</p>
 */
public record RuleEntityExtension(
        String ruleEntityTypeCode,
        String extensionCode,
        String tableName,
        String cardinality,
        boolean required
) {
}
