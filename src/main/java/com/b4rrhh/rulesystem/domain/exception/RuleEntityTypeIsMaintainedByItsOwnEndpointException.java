package com.b4rrhh.rulesystem.domain.exception;

import java.util.List;

/**
 * El tipo declara extensiones {@code required} en {@code rule_entity_extension} (ADR-053 §2),
 * así que una raíz suya no es un dato suelto: se da de alta por el endpoint propio de su
 * vertical, que sabe rellenar la extensión en la misma transacción (backend#88).
 *
 * <p>No es un 404 ni un conflicto: es una petición mal dirigida, y por eso el mensaje dice
 * <b>a dónde</b> ir. Un 400 que sólo dice «no» obliga a adivinar.</p>
 *
 * <p>El tipo puede no tener endpoint propio todavía —hoy {@code AGREEMENT} se siembra por
 * migración— y entonces el mensaje lo dice así, sin inventarse una ruta plausible.</p>
 */
public class RuleEntityTypeIsMaintainedByItsOwnEndpointException extends RuntimeException {

    public RuleEntityTypeIsMaintainedByItsOwnEndpointException(
            String ruleEntityTypeCode,
            List<String> requiredExtensionTables,
            String apiCollectionPath
    ) {
        super(message(ruleEntityTypeCode, requiredExtensionTables, apiCollectionPath));
    }

    private static String message(
            String ruleEntityTypeCode,
            List<String> requiredExtensionTables,
            String apiCollectionPath
    ) {
        String head = "Rule entity type " + ruleEntityTypeCode
                + " declares required extensions (" + String.join(", ", requiredExtensionTables)
                + ") and cannot be created through POST /rule-entities: the root would be left "
                + "without them. ";

        return head + (apiCollectionPath == null
                ? "This type has no creation endpoint of its own yet, so a root of it is only "
                + "seeded by migration. Adding one is what unblocks creating it through the API."
                : "Use POST " + apiCollectionPath + " instead, which writes the root and its "
                + "extensions in the same transaction.");
    }
}
