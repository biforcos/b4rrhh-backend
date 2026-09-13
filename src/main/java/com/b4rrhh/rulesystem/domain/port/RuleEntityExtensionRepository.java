package com.b4rrhh.rulesystem.domain.port;

import com.b4rrhh.rulesystem.domain.model.RuleEntityExtension;

import java.util.List;

public interface RuleEntityExtensionRepository {

    /** Todas las extensiones declaradas en el metamodelo, para agrupar por tipo. */
    List<RuleEntityExtension> findAll();

    /**
     * Las extensiones que el tipo declara obligatorias. Vacio es el caso comun: un maestro
     * simple no aparece en {@code rule_entity_extension} (ADR-053 §2).
     */
    List<RuleEntityExtension> findRequiredByRuleEntityTypeCode(String ruleEntityTypeCode);
}
