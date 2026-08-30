package com.b4rrhh.rulesystem.translation.application.service;

import com.b4rrhh.rulesystem.domain.port.RuleEntityRepository;
import com.b4rrhh.rulesystem.translation.domain.model.LanguageCode;
import com.b4rrhh.rulesystem.translation.domain.model.RuleEntityTranslation;
import com.b4rrhh.rulesystem.translation.domain.port.RuleEntityTranslationRepository;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Optional;

/**
 * El único sitio donde un código de catálogo se convierte en literal (ADR-052 §3).
 *
 * El idioma llega como argumento: quien lo llama —la capa web— ya lo ha resuelto desde
 * {@code Accept-Language}, y así el resolutor se prueba sin montar una petición. Si hay
 * traducción para ese idioma se devuelve; si no la hay, o no llega idioma, se cae al literal
 * base de {@code rule_entity.name} sin marcar nada (ADR-052 §5). Los huecos se ven en el
 * informe de cobertura, no aquí.
 */
@Service
public class RuleEntityLabelResolver {

    private final RuleEntityRepository ruleEntityRepository;
    private final RuleEntityTranslationRepository ruleEntityTranslationRepository;

    public RuleEntityLabelResolver(
            RuleEntityRepository ruleEntityRepository,
            RuleEntityTranslationRepository ruleEntityTranslationRepository
    ) {
        this.ruleEntityRepository = ruleEntityRepository;
        this.ruleEntityTranslationRepository = ruleEntityTranslationRepository;
    }

    /**
     * El nombre del código en el idioma pedido, o el literal base si no está traducido.
     * Vacío si el código no existe o no tiene literal: el mismo contrato que tenían los
     * {@code findCatalogName} de los adaptadores.
     *
     * @param languageCode idioma en BCP 47 corto ({@code es-ES}); {@code null} o un valor
     *                     sin forma de idioma significa «el literal base».
     */
    public Optional<String> resolveName(
            String ruleSystemCode,
            String ruleEntityTypeCode,
            String code,
            String languageCode
    ) {
        String normalizedRuleSystemCode = normalizeToUppercase(ruleSystemCode);
        String normalizedCode = normalizeToUppercase(code);
        if (normalizedRuleSystemCode == null || ruleEntityTypeCode == null || normalizedCode == null) {
            return Optional.empty();
        }

        return ruleEntityRepository
                .findByBusinessKey(normalizedRuleSystemCode, ruleEntityTypeCode, normalizedCode)
                .flatMap(entity -> translatedName(entity.getId(), languageCode)
                        .or(() -> nonBlank(entity.getName())));
    }

    private Optional<String> translatedName(Long ruleEntityId, String languageCode) {
        return LanguageCode.canonical(languageCode)
                .flatMap(language -> ruleEntityTranslationRepository
                        .findByRuleEntityIdAndLanguageCode(ruleEntityId, language))
                .map(RuleEntityTranslation::name)
                .flatMap(RuleEntityLabelResolver::nonBlank);
    }

    private static Optional<String> nonBlank(String value) {
        if (value == null) {
            return Optional.empty();
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? Optional.empty() : Optional.of(trimmed);
    }

    private static String normalizeToUppercase(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
