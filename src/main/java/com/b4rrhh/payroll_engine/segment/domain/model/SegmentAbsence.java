package com.b4rrhh.payroll_engine.segment.domain.model;

/**
 * Por que existe un tramo de ausencia, y cuanta ausencia lleva detras ({@code backend#127}).
 *
 * <h2>Que hace aqui el segundo campo</h2>
 *
 * <p>El {@code backend#127} deja la ausencia partiendo el periodo y quitando dias, y para eso solo
 * hace falta saber <b>que el tramo es de ausencia</b>. Los dias acumulados los pide el
 * {@code backend#129}: la prestacion cambia de porcentaje en el dia 4, en el 16 y en el 21 de la
 * baja, y esos dias <b>se cuentan desde el inicio de la ausencia, que puede estar en otro mes</b>.
 * No es leer otro mes —eso es el {@code backend#128}—: es una fecha.
 *
 * <p>Va en el contexto del tramo y no como concepto del motor porque es un <b>dato del tramo</b>,
 * como el contrato o el regimen de pagas: lo resuelve quien arma el tramo, no lo calcula nadie. Los
 * conceptos {@code ENGINE_PROVIDED} que cuenten dias por tramo de porcentaje lo leeran de aqui.
 *
 * <h2>Solo esta cuando la ausencia no se paga</h2>
 *
 * <p>Un tramo tiene ausencia <b>si y solo si</b> su ausencia es de las que no se pagan
 * ({@code IT_COMMON}, {@code UNPAID_LEAVE}). Las vacaciones y los permisos retribuidos no parten el
 * periodo y no llegan hasta aqui: la decision esta escrita una vez, en
 * {@code CalculatePayrollUnitService} (ADR-073).
 *
 * @param absenceTypeCode el tipo de la ausencia del catalogo, que distingue la baja del permiso no
 *        retribuido: la primera pagara prestacion y cotizara ({@code backend#129}), el segundo no
 *        hace ninguna de las dos cosas
 * @param daysBeforeSegment dias de ESTA ausencia transcurridos antes del primer dia del tramo. Cero
 *        cuando la ausencia empieza con el tramo; doce si la baja empezo el 20 de agosto y el tramo
 *        arranca el 1 de septiembre
 */
public record SegmentAbsence(
        String absenceTypeCode,
        long daysBeforeSegment
) {

    /** La baja por enfermedad comun, que es el alcance de la v1 del paso 5. */
    public static final String IT_COMMON = "IT_COMMON";

    /** El permiso no retribuido: el caso mas simple del mismo mecanismo, dias que no se pagan. */
    public static final String UNPAID_LEAVE = "UNPAID_LEAVE";

    public SegmentAbsence {
        if (absenceTypeCode == null || absenceTypeCode.isBlank()) {
            throw new IllegalArgumentException("absenceTypeCode must not be null or blank");
        }
        if (daysBeforeSegment < 0) {
            throw new IllegalArgumentException(
                    "daysBeforeSegment must not be negative, got: " + daysBeforeSegment);
        }
    }

    /** Si es una baja por enfermedad comun, que es la unica que pagara prestacion y cotizara. */
    public boolean isCommonSickLeave() {
        return IT_COMMON.equals(absenceTypeCode);
    }
}
