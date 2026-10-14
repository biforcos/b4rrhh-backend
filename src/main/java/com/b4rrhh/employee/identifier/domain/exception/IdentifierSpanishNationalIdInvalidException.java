package com.b4rrhh.employee.identifier.domain.exception;

/**
 * Un DNI o un NIE de España mal escrito, con el motivo en palabras (backend#171): «El DNI 12345678A no
 * es válido: la letra debería ser Z.» Es lo que lee quien lo tecleó, así que el código funcional no va
 * en el mensaje.
 */
public class IdentifierSpanishNationalIdInvalidException extends IdentifierValueInvalidException {

    public static final String FUNCTIONAL_CODE = "INVALID_SPANISH_NATIONAL_ID";

    public IdentifierSpanishNationalIdInvalidException(String identifierValue, String reason) {
        super("El " + documentName(identifierValue) + " " + identifierValue + " no es válido: " + reason + ".");
    }

    private static String documentName(String identifierValue) {
        return identifierValue != null && identifierValue.matches("^[XYZ].*") ? "NIE" : "DNI";
    }
}
