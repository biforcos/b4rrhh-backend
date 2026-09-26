package com.b4rrhh.employee.absence.application.usecase;

import java.time.LocalDate;

public record UpsertAbsenceCommand(
    String ruleSystemCode,
    String employeeTypeCode,
    String employeeNumber,
    String absenceTypeCode,
    LocalDate startDate,
    int startTime,
    LocalDate endDate,
    Integer endTime,
    /**
     * Si la baja lleva derecho a prestacion ({@code backend#129}).
     *
     * <p>Nulo quiere decir «no me lo digas, deja el que corresponda»: con derecho, que es el caso
     * normal. Es {@code Boolean} y no {@code boolean} para poder distinguir «no lo mando» de «lo
     * mando en false», que en una peticion de actualizacion no son lo mismo.
     */
    Boolean benefitEntitled
) {

    /** El derecho efectivo: con derecho salvo que digan lo contrario. */
    public boolean benefitEntitledOrDefault() {
        return benefitEntitled == null || benefitEntitled;
    }
}
