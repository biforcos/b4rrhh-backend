package com.b4rrhh.payroll.retro.application.usecase;

import java.math.BigDecimal;
import java.util.List;

/**
 * <b>De dónde sale una línea de atraso</b> ({@code backend#134}).
 *
 * <p>Es la condición de este camino aplicada al paso difícil: el recibo tiene que seguir explicándose
 * cuando la nómina deja de ser fácil. Y una línea de atraso no se explica con una travesía del grafo
 * —no viene de ningún paso de este cálculo— sino con <b>tres números</b>:
 *
 * <blockquote>
 * agosto vale hoy <b>X</b>, por agosto se ha pagado <b>Y</b>, esta línea es <b>X − Y</b>.
 * </blockquote>
 *
 * <p>La <b>Y</b> es lo pagado <b>antes de este recibo</b>: sin ella la diferencia de un recibo ya cerrado
 * saldría cero, porque su propia línea ya estaría contada como pagada.
 *
 * <p>Y con de dónde sale cada uno, que es la mitad que lo hace útil: la X del cálculo vigente de agosto,
 * con el instante en que se calculó; la Y del desglose de los recibos cerrados que pagaron algo
 * atribuido a agosto, <b>uno por uno</b>. «Por agosto se han pagado 59,40» no explica nada; «el recibo
 * de agosto pagó 0 y el de septiembre pagó 59,40 como atraso» sí.
 */
public interface ExplainPayrollArrearsUseCase {

    /** La explicación de todas las líneas de atraso de un recibo, o la lista vacía si no lleva. */
    List<ArrearExplanation> explain(ExplainPayrollArrearsCommand command);

    /**
     * @param originPeriodCode el mes al que pertenece la línea
     * @param currentValue lo que ese mes vale hoy para ese concepto: la <b>X</b>
     * @param currentValueCalculatedAt cuándo se calculó ese vigente, o {@code null} si no hay vigente —y
     *        entonces la línea es de una corrida anterior y esto lo dice en vez de callarlo
     * @param alreadyPaid lo pagado atribuido a ese mes: la <b>Y</b>
     * @param paidIn el desglose de la <b>Y</b>, un renglón por recibo que pagó algo
     * @param difference {@code X − Y}, que es el importe de la línea
     */
    record ArrearExplanation(
            String originPeriodCode,
            String conceptCode,
            String conceptLabel,
            BigDecimal lineAmount,
            BigDecimal currentValue,
            java.time.Instant currentValueCalculatedAt,
            BigDecimal alreadyPaid,
            List<PaidIn> paidIn,
            BigDecimal difference
    ) {

        /**
         * Si los tres números cuadran con el importe de la línea.
         *
         * <p>Que pueda no cuadrar no es un defecto de esta explicación: es lo que pasa cuando esta línea
         * se pagó en una corrida anterior y desde entonces el vigente se ha vuelto a pisar. Entonces la
         * diferencia de hoy no es el importe de esta línea, y <b>decirlo es mejor que esconderlo</b>: la
         * pantalla lo puede marcar en vez de enseñar tres números que no suman.
         */
        public boolean addsUp() {
            return difference != null && lineAmount != null
                    && difference.compareTo(lineAmount) == 0;
        }
    }

    /**
     * @param payrollPeriodCode el recibo que pagó
     * @param amount cuánto pagó atribuido al mes de origen. En el propio mes es su línea normal; en un
     *        mes posterior es una línea de atraso
     */
    record PaidIn(String payrollPeriodCode, BigDecimal amount) {
    }
}
