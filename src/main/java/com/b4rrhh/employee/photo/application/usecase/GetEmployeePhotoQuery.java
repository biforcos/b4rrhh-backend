package com.b4rrhh.employee.photo.application.usecase;

/**
 * @param version el UUID de la clave, que el enlace lleva como {@code ?v=}. Hace de llave: sin él, o
 *                con uno viejo, no hay foto (backend#177).
 */
public record GetEmployeePhotoQuery(
        String ruleSystemCode,
        String employeeTypeCode,
        String employeeNumber,
        String version
) {
}
