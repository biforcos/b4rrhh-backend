package com.b4rrhh.rulesystem.translation.application.port;

import java.util.List;
import java.util.Map;

/**
 * Lo que el informe de cobertura necesita leer (ADR-052 §5). Cuenta todas las filas de
 * {@code rule_entity}, activas o no: una traducción cuelga de la fila, no de su vigencia.
 */
public interface RuleEntityTranslationCoverageReadPort {

    /** Cuántas filas de {@code rule_entity} hay por tipo. */
    Map<String, Long> countRuleEntitiesByType();

    /** Las filas de {@code rule_entity} sin traducción en ese idioma, ordenadas por tipo, capa y código. */
    List<UntranslatedRuleEntity> findUntranslated(String languageCode);

    /** Una fila, no una resolución: de la capa en la que vive, aunque la monten varias reglamentaciones (backend#157). */
    record UntranslatedRuleEntity(String ruleEntityTypeCode, String layerCode, int level, String code, String name) {
    }
}
