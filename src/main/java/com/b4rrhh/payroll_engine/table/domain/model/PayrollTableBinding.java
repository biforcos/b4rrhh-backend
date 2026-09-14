package com.b4rrhh.payroll_engine.table.domain.model;

/**
 * Quien lee una tabla y con que rol.
 *
 * El rol de vinculacion -BASE_SALARY_TABLE, AGREEMENT_PLUS_TABLE- es una
 * ranura: el motor lo coge como nombre y lo resuelve contra
 * payroll_object_binding hasta dar con la tabla que tiene los valores. Por eso
 * el rol vive aqui dentro, pegado a la tabla que ata, y no como una entidad
 * suelta que se pudiera confundir con una tabla (backend#95).
 *
 * Una vinculacion inactiva se conserva y se dice: es la explicacion de una
 * tabla que dejo de leerse, y esconderla convierte eso en un misterio.
 */
public record PayrollTableBinding(
        String ownerTypeCode,
        String ownerCode,
        String bindingRoleCode,
        boolean active
) {
}
