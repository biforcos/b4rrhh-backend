package com.b4rrhh.rulesystem.application.port;

import com.b4rrhh.rulesystem.domain.model.RuleEntityReference;

import java.util.List;

public interface RuleEntityUsageCheckPort {

    /**
     * Qué recursos de negocio referencian el código y cuántas veces. Vacío si nada lo usa.
     * Cuenta el histórico entero: un código que se usó alguna vez no se puede borrar sin que
     * ese histórico se lea mal para siempre.
     */
    List<RuleEntityReference> findReferences(
            String ruleSystemCode,
            String ruleEntityTypeCode,
            String code
    );
}
