package com.b4rrhh.payroll.application.port;

import java.util.Objects;

/**
 * La fila de tabla de la que un paso leyo su valor ({@code backend#107}).
 *
 * <p>Es una <b>direccion</b>, y por eso lleva la tabla y no solo el numero de fila: la pantalla que
 * tiene que aterrizar en ella se abre por tabla y senala la fila dentro. El sistema de reglas no
 * va aqui porque ya lo trae el recibo al que pertenece el paso.
 *
 * <p>Lo que guarda es lo que el motor tenia delante al calcular, no lo que la misma busqueda
 * contestaria hoy. La diferencia importa justo cuando alguien ha tocado algo en medio, que es
 * cuando se hace la pregunta.
 */
public record TableRowOrigin(String tableCode, long rowId) {

    public TableRowOrigin {
        Objects.requireNonNull(tableCode, "tableCode");
    }
}
