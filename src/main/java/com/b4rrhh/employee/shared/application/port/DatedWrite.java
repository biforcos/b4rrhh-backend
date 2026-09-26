package com.b4rrhh.employee.shared.application.port;

import java.time.LocalDate;

/**
 * Una escritura con fecha, tal y como la cuenta quien la hizo ({@code backend#130}).
 *
 * @param ruleSystemCode   sistema de reglas del empleado
 * @param employeeTypeCode tipo de empleado
 * @param employeeNumber   numero de empleado
 * @param periodCode       el periodo al que la escritura llega, en la forma {@code 202608}
 * @param verticalCode     que vertical la hizo, para que la ficha pueda decir «una ausencia» y la
 *                         checklist del ciclo pueda agrupar
 * @param table            la tabla que se escribio, con esquema
 * @param rowId            la fila, o nulo si la escritura fue un borrado y ya no existe, o si la
 *                         vertical no tiene id surrogado
 * @param rowKey           la clave de negocio de la fila en texto, para las verticales que no tienen
 *                         id: una entrada de nomina se identifica por concepto y periodo
 */
public record DatedWrite(
        String ruleSystemCode,
        String employeeTypeCode,
        String employeeNumber,
        String periodCode,
        String verticalCode,
        String table,
        Long rowId,
        String rowKey
) {

    /**
     * Una escritura fechada por la <b>fecha mas antigua a la que alcanza</b>.
     *
     * <p>La fecha mas antigua y no cualquiera de las suyas: una ausencia del 28 de agosto al 4 de
     * septiembre mueve los dos meses, y quien recalcula lo hace hacia delante desde el mas antiguo
     * (el {@code #131}), asi que decir agosto ya dice septiembre. Al contrario no.
     *
     * <p>La conversion de fecha a periodo vive aqui y no en cada vertical: es una linea, y repetida
     * catorce veces seria una linea con catorce sitios donde equivocarse.
     */
    public static DatedWrite on(
            LocalDate earliestAffectedDate,
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber,
            String verticalCode,
            String table,
            Long rowId
    ) {
        return new DatedWrite(
                ruleSystemCode, employeeTypeCode, employeeNumber,
                periodCodeOf(earliestAffectedDate),
                verticalCode, table, rowId, null);
    }

    /**
     * Lo mismo, para las verticales cuya identidad <b>no es un id surrogado</b>.
     *
     * <p>El contrato, la clasificacion laboral y la distribucion de coste se identifican por empleado
     * y fecha de inicio, y sus modelos de dominio no tienen id a proposito. La marca guarda entonces
     * esa clave en texto, para que la ficha pueda decir de que fila se trataba.
     */
    public static DatedWrite on(
            LocalDate earliestAffectedDate,
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber,
            String verticalCode,
            String table,
            String rowKey
    ) {
        return new DatedWrite(
                ruleSystemCode, employeeTypeCode, employeeNumber,
                periodCodeOf(earliestAffectedDate),
                verticalCode, table, null, rowKey);
    }

    /**
     * Una escritura que ya viene con su periodo, sin fecha de por medio.
     *
     * <p>Es el caso de las entradas de nomina ({@code employee.employee_payroll_input}): su columna
     * no es una fecha, es el periodo. No se le pone una fecha ficticia para poder usar el otro
     * constructor.
     */
    public static DatedWrite forPeriod(
            int period,
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber,
            String verticalCode,
            String table,
            String rowKey
    ) {
        return new DatedWrite(
                ruleSystemCode, employeeTypeCode, employeeNumber,
                String.valueOf(period),
                verticalCode, table, null, rowKey);
    }

    /** {@code 2026-08-15} es {@code 202608}. */
    public static String periodCodeOf(LocalDate date) {
        return String.format("%04d%02d", date.getYear(), date.getMonthValue());
    }
}
