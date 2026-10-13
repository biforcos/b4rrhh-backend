package com.b4rrhh.rulesystem.application.usecase;

import java.time.LocalDate;

/**
 * Las normalizaciones de los casos de uso por capa (frontend#131): las mismas reglas que los
 * servicios por reglamentación, que cada uno lleva en privado.
 */
final class LayerRuleEntityInput {

    private LayerRuleEntityInput() {
    }

    static String requiredCode(String fieldName, String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim().toUpperCase();
    }

    static String optionalCode(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim().toUpperCase();
    }

    static LocalDate requiredDate(String fieldName, LocalDate value) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value;
    }

    static String requiredName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("name is required");
        }
        String normalized = name.trim();
        if (normalized.length() > 100) {
            throw new IllegalArgumentException("name exceeds max length 100");
        }
        return normalized;
    }

    static String description(String description) {
        if (description == null) {
            return null;
        }
        String normalized = description.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        if (normalized.length() > 500) {
            throw new IllegalArgumentException("description exceeds max length 500");
        }
        return normalized;
    }
}
