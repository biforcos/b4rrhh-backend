package com.b4rrhh.employee.lifecycle.domain.exception;

/**
 * Un alta que llega con un motivo de entrada que no es {@code HIRING} (b4rrhh/backend#143).
 *
 * <p>El alta es una contratación por construcción. La readmisión tiene su propio flujo, y
 * {@code TRANSFER_IN} no tiene ninguno todavía: está en el catálogo porque hay presencias que lo
 * llevan, pero nadie ha definido qué es un traslado de entrada, así que no se ofrece.
 */
public class HireEmployeeEntryReasonNotHiringException extends RuntimeException {

    public HireEmployeeEntryReasonNotHiringException(String entryReasonCode) {
        super(messageFor(entryReasonCode));
    }

    private static String messageFor(String entryReasonCode) {
        if ("REHIRE".equals(entryReasonCode)) {
            return "Un alta entra siempre como contratación (HIRING). Para volver a dar de alta a "
                    + "alguien que ya estuvo en la empresa está la readmisión, desde su ficha.";
        }
        return "Un alta entra siempre como contratación (HIRING). El motivo " + entryReasonCode
                + " no tiene flujo todavía y no se puede usar en el alta.";
    }
}
