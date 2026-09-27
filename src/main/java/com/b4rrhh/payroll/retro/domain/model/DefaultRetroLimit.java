package com.b4rrhh.payroll.retro.domain.model;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

/**
 * Hasta donde atras recalcula quien no lo dice ({@code backend#136}).
 *
 * <p>Es el valor que Operaciones <b>propone</b> al lanzar ({@code frontend#85}), y aqui es el que usa el
 * recalculo de un recibo, que no pregunta: la operativa normal es corregir un dato de un empleado y
 * recalcularle desde su recibo, y un recibo que obligara a elegir un limite para eso seria un formulario
 * que nadie rellena bien. Quien quiera otro limite va a Operaciones.
 *
 * <p>No es un tope del sistema, y por eso lleva su motivo escrito al lado, el mismo que ensena la
 * pantalla: doce meses porque dentro de ese ano la correccion se arregla con una liquidacion
 * complementaria a la Seguridad Social y con la retencion del mes en que se paga; mas atras entra el
 * ejercicio fiscal ya declarado, y eso no lo resuelve una nomina.
 */
public final class DefaultRetroLimit {

    public static final int MONTHS_BACK = 12;

    public static final String REASON =
            "Doce meses atras: dentro de ese ano la correccion se arregla con una liquidacion complementaria"
                    + " a la Seguridad Social y con la retencion del mes en que se paga. Mas atras entra el"
                    + " ejercicio fiscal ya declarado, y eso no lo resuelve una nomina.";

    private static final DateTimeFormatter PERIODO = DateTimeFormatter.ofPattern("yyyyMM");

    private DefaultRetroLimit() {
    }

    /** El limite para un periodo: doce meses antes, {@code 202609 -> 202509}. */
    public static String forPeriod(String payrollPeriodCode) {
        return YearMonth.parse(payrollPeriodCode, PERIODO).minusMonths(MONTHS_BACK).format(PERIODO);
    }
}
