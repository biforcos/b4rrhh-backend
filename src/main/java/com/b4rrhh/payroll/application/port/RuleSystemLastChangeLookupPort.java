package com.b4rrhh.payroll.application.port;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Cuándo se tocó por última vez la reglamentación de un sistema de reglas ({@code backend#107}).
 *
 * <p>Existe para una sola pregunta, y es de comparación: <b>¿han cambiado las reglas desde que este
 * recibo se calculó?</b> Un recibo es lo que el motor calculó y no se toca por su cuenta
 * ({@code ADR-062}); lo que hace falta es que <i>diga</i> que quizá ya no refleja lo vigente.
 *
 * <p><b>Es mucho menos que reconstruir la reglamentación de entonces.</b> Aquí no hay huella ni
 * hash: una fecha contra el {@code calculated_at} que el recibo ya guarda desde la {@code V53}.
 *
 * <h3>Sobre-avisa a propósito</h3>
 *
 * <p>La fecha es del sistema de reglas entero, no del recibo. Un cambio en un concepto que este
 * empleado no usa levanta la marca igual. Es la dirección segura —<b>nunca dice fresco cuando está
 * rancio</b>— y por eso lo que se pinta con esto se redacta como <i>«puede que ya no refleje las
 * reglas actuales»</i> y no como una afirmación. Afinarlo exigiría saber qué conceptos tocan a qué
 * recibo, que es otro problema y no hace falta para éste.
 *
 * <h3>La frontera, y lo que se queda fuera</h3>
 *
 * <p>La fecha es el máximo de los {@code updated_at} de las tablas en las que vive la reglamentación
 * del motor: los objetos, los conceptos, sus operandos, sus alimentaciones, las asignaciones, las
 * vinculaciones y las filas de tabla. Eso cubre el gesto por el que esto existe —editar una fila de
 * tabla salarial— y todo lo que se le parece.
 *
 * <p>Lo que <b>no</b> cubre, dicho aquí porque callarlo sería peor:
 *
 * <ul>
 *   <li><b>Un borrado.</b> Una fila borrada se lleva su marca de tiempo con ella, así que borrar es
 *       el único cambio que puede dejar la marca abajo.
 *   <li><b>Una tabla sin {@code updated_at}</b>, como {@code payroll_engine.ss_cotizacion_topes}.
 *       Añadirle la columna no arreglaría nada mientras nada la mantenga: parecería cubierta.
 *   <li><b>Una escritura que no pase por la aplicación.</b> Un {@code update} a mano mueve el dato
 *       y no la marca.
 * </ul>
 *
 * <p>La alternativa era un sello por sistema de reglas movido por disparadores, y tiene un defecto
 * peor para lo que esto sirve: el reinicio nocturno de la demo recarga un volcado, los disparadores
 * se dispararían al restaurar y <b>los 873 recibos amanecerían avisando</b>. Una marca que sale
 * siempre no dice nada. Los {@code updated_at} se restauran como el dato que son.
 */
public interface RuleSystemLastChangeLookupPort {

    /**
     * @return la fecha del último cambio conocido en la reglamentación de ese sistema de reglas, o
     *         vacío si no hay ninguna reglamentación suya en la base
     */
    Optional<Instant> lastChangedAt(String ruleSystemCode);
}
