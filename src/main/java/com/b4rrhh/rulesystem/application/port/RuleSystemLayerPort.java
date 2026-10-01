package com.b4rrhh.rulesystem.application.port;

/**
 * Las capas de una reglamentación (ADR-077, backend#156).
 *
 * <p>El esquema exige que toda reglamentación monte exactamente una capa por nivel, y lo
 * comprueba al confirmar la transacción. Por eso crear una reglamentación y montar sus capas
 * tiene que ir en la misma.</p>
 */
public interface RuleSystemLayerPort {

    boolean layerExists(String layerCode);

    /** Crea las capas propias y monta las cinco; ver {@link com.b4rrhh.rulesystem.domain.model.DefaultRuleSystemLayers}. */
    void assembleDefaultLayers(String ruleSystemCode, String ruleSystemName);
}
