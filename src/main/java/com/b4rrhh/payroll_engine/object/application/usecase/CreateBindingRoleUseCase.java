package com.b4rrhh.payroll_engine.object.application.usecase;

import com.b4rrhh.payroll_engine.object.domain.model.PayrollObject;

/**
 * Crea una ranura de tabla: un objeto del grafo de tipo TABLE cuyo codigo el motor usa
 * como rol de vinculacion (ADR-063).
 *
 * <p><b>No crea una tabla.</b> Se llamaba asi hasta el backend#98 y era el nombre lo que
 * estaba mal: lo que esto crea no tiene filas ni importes, y no aparecera en
 * {@code GET /payroll-engine/{ruleSystemCode}/tables} hasta que una vinculacion por convenio
 * le ate una tabla que si los tenga.
 */
public interface CreateBindingRoleUseCase {

    PayrollObject create(CreateBindingRoleCommand command);
}
