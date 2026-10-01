package com.b4rrhh.rulesystem.domain.model;

import java.util.List;

/**
 * Las capas con las que nace una reglamentación (ADR-077, backend#156).
 *
 * <p>Monta las dos compartidas, {@code COM} (nivel 1) e {@code INT} (nivel 2), y trae tres
 * propias: la nacional con su mismo código (nivel 3), {@code NOM_<código>} (nivel 4, la ley de
 * nómina) y {@code NOM_<código>_EMP} (nivel 5, el esquema de la empresa). Las dos últimas nacen
 * vacías.</p>
 */
public final class DefaultRuleSystemLayers {

    public static final String COMMON = "COM";
    public static final String INTERNATIONAL = "INT";

    private DefaultRuleSystemLayers() {
    }

    public static String national(String ruleSystemCode) {
        return ruleSystemCode;
    }

    public static String payroll(String ruleSystemCode) {
        return "NOM_" + ruleSystemCode;
    }

    public static String companyPayroll(String ruleSystemCode) {
        return "NOM_" + ruleSystemCode + "_EMP";
    }

    /** Las tres que la reglamentación trae suyas, en orden de nivel. */
    public static List<String> ownLayerCodes(String ruleSystemCode) {
        return List.of(national(ruleSystemCode), payroll(ruleSystemCode), companyPayroll(ruleSystemCode));
    }
}
