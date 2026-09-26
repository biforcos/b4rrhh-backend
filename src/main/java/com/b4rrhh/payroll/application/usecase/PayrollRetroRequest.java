package com.b4rrhh.payroll.application.usecase;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Hasta donde atras recalcula este lanzamiento ({@code backend#132}, paso 6 de
 * {@code b4rrhh/workspace#9}).
 *
 * <p>Va en la <b>peticion</b> y no en la empresa, y eso se decidio el 05/10. Una revision de convenio
 * —«todos desde enero»— no es una propiedad de la empresa: es una cosa que pasa una vez, en una corrida
 * concreta, y la siguiente no tiene por que arrastrarla. Ponerlo en la empresa habria convertido un
 * suceso en una configuracion, que es la forma de que alguien lo deje puesto.
 *
 * @param floorPeriodCode el <b>suelo obligatorio para todos</b>: todo empleado del lanzamiento
 *        recalcula desde aqui aunque no tenga ninguna marca. Opcional, y nulo es lo normal. Es la forma
 *        humana de una retro masiva en v1
 * @param limitPeriodCode el <b>limite duro</b>: nada mas atras de este periodo. Nulo significa que esta
 *        corrida no hace retro
 */
public record PayrollRetroRequest(String floorPeriodCode, String limitPeriodCode) {

    private static final DateTimeFormatter PERIODO = DateTimeFormatter.ofPattern("yyyyMM");

    public PayrollRetroRequest {
        floorPeriodCode = normalizar(floorPeriodCode, "retroFloorPeriodCode");
        limitPeriodCode = normalizar(limitPeriodCode, "retroLimitPeriodCode");

        // Un suelo mas antiguo que el limite es pedir dos cosas contrarias, y se rechaza aqui: en la
        // peticion y no al calcular. Si ganara una de las dos en silencio, el recibo diria que se
        // calculo desde enero y la mitad de los meses no estarian.
        if (floorPeriodCode != null && limitPeriodCode != null
                && floorPeriodCode.compareTo(limitPeriodCode) < 0) {
            throw new IllegalArgumentException(
                    "El suelo de la retro (" + floorPeriodCode + ") no puede ser mas antiguo que el"
                            + " limite (" + limitPeriodCode + "): son dos cosas contrarias y no hay"
                            + " forma de cumplir las dos");
        }

        // Un suelo sin limite es un suelo sin techo: pediria recalcular desde enero y a la vez no
        // recalcular nada, porque sin limite no hay retro.
        if (floorPeriodCode != null && limitPeriodCode == null) {
            throw new IllegalArgumentException(
                    "Un suelo de retro sin limite no se puede cumplir: sin limite esta corrida no hace"
                            + " retro, asi que el suelo no llegaria a ninguna parte");
        }
    }

    /**
     * Un lanzamiento <b>sin retro</b>, que es lo que era todo lanzamiento antes del {@code #132}.
     *
     * <p>No hace retro y <b>no se lo calla</b>: si la corrida encuentra empleados con marcas activas,
     * lo dice en sus mensajes ({@code RETRO_SKIPPED_NO_LIMIT}) con cuantos eran. Una corrida no puede
     * inventarse hasta donde atras tiene permiso para recalcular, pero tampoco puede dejar sin pagar un
     * atraso sin que nadie lo sepa.
     */
    public static PayrollRetroRequest none() {
        return new PayrollRetroRequest(null, null);
    }

    /** Si esta corrida puede recalcular el pasado. */
    public boolean allowsRetro() {
        return limitPeriodCode != null;
    }

    private static String normalizar(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String limpio = valor.trim();
        try {
            YearMonth.parse(limpio, PERIODO);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    campo + " tiene que ser un periodo en la forma yyyyMM: '" + valor + "'");
        }
        return limpio;
    }
}
