package com.b4rrhh.rulesystem.translation.domain.model;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * El formato del idioma de una traducción: BCP 47 corto, {@code es-ES}, {@code fr-FR} o
 * {@code en}. Es el mismo formato que exige el check de {@code rule_entity_translation}, y
 * todo lo que entra por la aplicación pasa por aquí para que no convivan {@code es},
 * {@code es_ES} y {@code es-ES} (ADR-052 §1).
 */
public final class LanguageCode {

    private static final Pattern CANONICAL = Pattern.compile("^[a-z]{2}(-[A-Z]{2})?$");

    private LanguageCode() {
    }

    /**
     * Normaliza un código a su forma canónica: {@code ES-es}, {@code es_ES} y {@code es-ES}
     * dan {@code es-ES}. Vacío si no tiene forma de idioma.
     */
    public static Optional<String> canonical(String raw) {
        if (raw == null) {
            return Optional.empty();
        }

        String[] parts = raw.trim().replace('_', '-').split("-");
        if (parts.length > 2) {
            return Optional.empty();
        }
        String language = parts[0].toLowerCase(Locale.ROOT);
        String candidate = parts.length > 1 && !parts[1].isEmpty()
                ? language + "-" + parts[1].toUpperCase(Locale.ROOT)
                : language;

        return CANONICAL.matcher(candidate).matches()
                ? Optional.of(candidate)
                : Optional.empty();
    }
}
