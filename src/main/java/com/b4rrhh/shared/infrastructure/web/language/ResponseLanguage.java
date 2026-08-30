package com.b4rrhh.shared.infrastructure.web.language;

import com.b4rrhh.rulesystem.translation.domain.model.LanguageCode;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * El idioma en el que se enseña una respuesta, resuelto una sola vez desde
 * {@code Accept-Language} y entregado a los ensambladores (ADR-052 §4; backend#23 §3).
 *
 * Vive en la capa web y no baja de ahí: ningún caso de uso ni puerto de aplicación sabe que
 * existen los idiomas. Sin cabecera, o con una que no tiene forma de idioma, se enseña el
 * literal base; nunca es un error.
 *
 * @param code idioma en BCP 47 corto ({@code es-ES}), o {@code null} para el literal base
 */
public record ResponseLanguage(String code) {

    public static ResponseLanguage base() {
        return new ResponseLanguage(null);
    }

    /**
     * El primer idioma de la cabecera, por peso, que tenga forma canónica:
     * {@code es-ES,es;q=0.9} da {@code es-ES}. Una cabecera ausente, vacía o inválida da el
     * literal base.
     */
    public static ResponseLanguage fromAcceptLanguage(String header) {
        if (header == null || header.isBlank()) {
            return base();
        }

        List<Locale.LanguageRange> ranges;
        try {
            ranges = Locale.LanguageRange.parse(header);
        } catch (IllegalArgumentException malformed) {
            return base();
        }

        return ranges.stream()
                .map(range -> Locale.forLanguageTag(range.getRange()))
                .map(ResponseLanguage::shortTag)
                .flatMap(Optional::stream)
                .findFirst()
                .map(ResponseLanguage::new)
                .orElseGet(ResponseLanguage::base);
    }

    private static Optional<String> shortTag(Locale locale) {
        if (locale.getLanguage().isEmpty()) {
            return Optional.empty();
        }
        String tag = locale.getCountry().isEmpty()
                ? locale.getLanguage()
                : locale.getLanguage() + "-" + locale.getCountry();
        return LanguageCode.canonical(tag);
    }
}
