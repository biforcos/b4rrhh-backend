package com.b4rrhh.rulesystem.translation.application.usecase;

import java.util.List;

/**
 * El informe de cobertura de un idioma: por tipo, cuántos códigos tienen traducción,
 * cuántos no, y cuáles son los que faltan. Es la herramienta de quien va a traducir, y lo
 * que permite que la respuesta normal caiga al literal base sin marcar nada (ADR-052 §5).
 */
public record RuleEntityTranslationCoverage(String languageCode, List<TypeCoverage> types) {

    public record TypeCoverage(
            String ruleEntityTypeCode,
            long total,
            long translated,
            long missing,
            List<MissingCode> missingCodes
    ) {
    }

    public record MissingCode(String ruleSystemCode, String code, String name) {
    }
}
