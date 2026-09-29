package com.b4rrhh.employee.lifecycle.application.model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * El empleado al que ya pertenece un documento de identidad (b4rrhh/backend#141).
 *
 * <p>Es lo que el alta necesita para negarse bien: no basta con «ya existe», hay que decir quién
 * es y en qué estado está, porque de eso depende el camino bueno. Si está cesado, volver es una
 * readmisión; si está de alta, no hay nada que hacer.
 *
 * @param ceasedOn el último día de su última presencia cerrada, o {@code null} si nunca cesó
 */
public record IdentifierOwner(
        String employeeTypeCode,
        String employeeNumber,
        boolean active,
        LocalDate ceasedOn
) {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** «Este DNI ya es EMP000123 (cesado el 13/05/2026)», o «…, que está de alta». */
    public String alreadyIs(String identifierTypeCode, String identifierValue) {
        String who = "Este " + documentName(identifierTypeCode, identifierValue) + " ya es " + employeeNumber;
        if (active || ceasedOn == null) {
            return who + ", que está de alta";
        }
        return who + " (cesado el " + ceasedOn.format(DAY) + ")";
    }

    private static String documentName(String identifierTypeCode, String identifierValue) {
        if ("NATIONAL_ID".equals(identifierTypeCode)) {
            return identifierValue != null && identifierValue.matches("^[XYZ].*") ? "NIE" : "DNI";
        }
        if ("PASSPORT".equals(identifierTypeCode)) {
            return "pasaporte";
        }
        return "documento";
    }
}
