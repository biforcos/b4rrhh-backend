package com.b4rrhh.payroll.application.port;

/**
 * De donde se lee lo que otro mes vale, y es lo unico que cambia entre el calculo normal y el retro
 * ({@code backend#131}, ADR-076 §3).
 *
 * <p>El puerto es el mismo y el candado es el mismo
 * ({@code OnlyOnePortReadsAPayrollOfAnotherPeriodTest}): sigue habiendo <b>un solo camino</b> por el
 * que un calculo mira fuera de su periodo. Lo que cambia es la fuente, y cambia porque la pregunta es
 * distinta.
 */
public enum PreviousPeriodSource {

    /**
     * El calculo normal: <b>solo el recibo cerrado</b> (ADR-074).
     *
     * <p>Un numero que todavia puede cambiar no se lee: la prestacion saldria de una base que manana
     * es otra, y el recibo ya estaria entregado.
     */
    DEFINITIVE_RECEIPT,

    /**
     * El modo retro: <b>el vigente si existe, y si no el recibo cerrado</b>.
     *
     * <p>Porque en una retro se recalcula hacia delante desde el mes mas antiguo tocado: cuando le
     * toca a agosto, el vigente de julio ya existe y es lo que julio vale <i>ahora</i>. Leer el recibo
     * de julio seria calcular agosto con el julio que ya sabemos que ha cambiado — y el atraso de
     * agosto saldria mal por construccion.
     *
     * <p>Y si no hay vigente de julio es porque julio no entraba en el tramo: entonces su recibo
     * cerrado <b>es</b> lo que julio vale, y se lee ese.
     */
    CURRENT_CALCULATION_FIRST
}
