package com.b4rrhh.payroll_engine.concept.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Cuantos decimales lleva el resultado de un concepto, y como se redondea (backend#61).
 *
 * <h2>Por que son dos propiedades del concepto y no dos constantes del motor</h2>
 *
 * Porque una nomina paga <b>dias</b>, y el decimal aparece donde se aplica una tasa —el precio
 * derivado, los porcentajes— y solo ahi. <b>El concepto es el sitio donde se aplica una tasa</b>,
 * asi que es el sitio donde se declara con cuantos decimales se queda. Hasta el backend#61 esto
 * eran tres constantes repartidas por los resolutores, todas a 2 y {@code HALF_UP}, y no habia
 * forma de decir que el precio por dia necesita seis y los dias ninguno.
 *
 * <h2>El invariante: nada se redondea dos veces</h2>
 *
 * El motor redondea <b>una vez</b>, al terminar de evaluar el concepto, y los resolutores devuelven
 * el valor exacto. El valor de periodo de un concepto {@code SEGMENT} es la suma de sus valores de
 * tramo <b>ya redondeados</b>, y nadie vuelve a redondear encima.
 *
 * <p>Eso es lo que hace que no exista el problema del residuo: la suma de las partes <b>es</b> el
 * total por construccion, y no hay centimos sobrantes que repartir a la ultima linea ni conceptos
 * de ajuste que inventar. Lo sujeta {@code NadaSeRedondeaDosVecesTest}.
 *
 * <h2>Lo que NO decide</h2>
 *
 * La precision que se enseña. Se enseña la que se uso: el paso guardado lleva el valor con sus
 * decimales declarados, y las pantallas lo pintan asi. Lo prohibido es enseñar {@code 33,33}
 * habiendo usado {@code 33,333333}.
 *
 * @param scale decimales del resultado. Cero es un valor legitimo —los dias son enteros—.
 * @param mode  como se resuelve el empate. {@code HALF_UP} es el de la nomina espanola.
 */
public record ConceptRounding(int scale, RoundingMode mode) {

    /**
     * Lo que el motor hacia antes de que esto existiera, y lo que hereda un concepto que no lo
     * declare.
     *
     * <p>No es un valor neutro elegido al azar: es exactamente el {@code setScale(2, HALF_UP)} que
     * estaba repetido en {@code RateByQuantityOperandResolver}, {@code PercentageConceptResolver} y
     * el caso {@code AGGREGATE} del motor. Manteniendolo, declarar las propiedades no cambio ni un
     * importe salvo donde se declaro otra cosa a proposito.
     */
    public static final ConceptRounding DEFAULT = new ConceptRounding(2, RoundingMode.HALF_UP);

    public ConceptRounding {
        if (scale < 0) {
            throw new IllegalArgumentException("El numero de decimales de un concepto no puede ser negativo: " + scale);
        }
        if (mode == null) {
            throw new IllegalArgumentException("Un concepto necesita un modo de redondeo");
        }
    }

    /** El valor con los decimales declarados. Es el unico redondeo que se aplica al concepto. */
    public BigDecimal apply(BigDecimal value) {
        return value.setScale(scale, mode);
    }
}
