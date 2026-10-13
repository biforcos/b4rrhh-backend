package com.b4rrhh.rulesystem.domain.exception;

/**
 * Una reglamentación ve las entidades de todas las capas que monta, pero sólo escribe en la suya,
 * la nacional (backend#166, ADR-077). Un país de {@code INT} lo ven ESP, FRA y PRT: corregirlo o
 * borrarlo desde ESP es tocar algo que no es de ESP, y borrarlo contando sólo las referencias de ESP
 * puede dejar a FRA con un código que ya no existe.
 *
 * <p>Es un conflicto con dónde está definida la entidad, y el mensaje dice a dónde ir: a
 * {@code /layers/{capa}/rule-entities}, que es por donde se mantiene (frontend#131).</p>
 */
public class RuleEntityIsMaintainedFromItsLayerException extends RuntimeException {

    public RuleEntityIsMaintainedFromItsLayerException(
            String ruleSystemCode,
            String layerCode,
            String ruleEntityTypeCode,
            String code
    ) {
        super(message(ruleSystemCode, layerCode, ruleEntityTypeCode, code));
    }

    private static String message(String ruleSystemCode, String layerCode, String ruleEntityTypeCode, String code) {
        String what = code == null ? "Rule entity type " + ruleEntityTypeCode : ruleEntityTypeCode + "/" + code;
        String path = "/layers/" + layerCode + "/rule-entities" + (code == null ? "" : "/" + ruleEntityTypeCode + "/" + code);
        return what + " is defined in layer " + layerCode + ", not in the national layer of " + ruleSystemCode
                + ", and is seen by every rule system that mounts " + layerCode
                + ". It is maintained at " + path + ".";
    }
}
