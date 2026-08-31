package com.b4rrhh.rulesystem.infrastructure.web.dto;

/**
 * Una extensión declarada del tipo, tal como viaja con él (ADR-053 §2, frontend#33).
 * {@code table_name} no se expone: es fontanería del esquema, no algo que una pantalla
 * deba conocer.
 */
public record RuleEntityTypeExtensionResponse(
        String extensionCode,
        String cardinality,
        boolean required
) {
}
