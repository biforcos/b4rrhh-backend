package com.b4rrhh.rulesystem.domain.exception;

/**
 * Una raíz tiene declarada una extensión {@code required} en {@code rule_entity_extension}
 * (ADR-053 §2) y no existe la fila. No es un caso de negocio ni un 404: es una inconsistencia
 * de datos que la guardia 3 (backend#33) impide en la base sembrada, así que si suena es que
 * la invariante se ha roto — y debe sonar como tal, un 500 con la raíz en el log, nunca
 * taparse con datos inventados (backend#34).
 */
public class RequiredExtensionMissingException extends RuntimeException {

    public RequiredExtensionMissingException(Long ruleEntityId, String extensionTable) {
        super("Required extension missing: " + extensionTable + " has no row for rule_entity " + ruleEntityId);
    }
}
