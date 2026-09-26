package com.b4rrhh.payroll.application.usecase;

import com.b4rrhh.payroll.application.port.PreviousPeriodSource;

/**
 * Para que se calcula una unidad: para escribir su recibo o para saber cuanto vale hoy un mes cerrado
 * ({@code backend#131}, ADR-076).
 *
 * <p>El calculo es el mismo —el mismo grafo, los mismos tramos, las mismas reglas— y lo que cambia es
 * <b>donde acaba</b> y <b>de donde lee lo de otro mes</b>. Esas dos cosas van juntas a proposito: un
 * modo que escribiera el vigente pero leyera recibos daria atrasos mal calculados, y uno que escribiera
 * el recibo leyendo vigentes reescribiria un documento entregado con numeros de trabajo.
 */
public enum PayrollCalculationMode {

    /** El de siempre: el resultado es el <b>recibo</b> del periodo, y lo de otro mes se lee cerrado. */
    RECEIPT(PreviousPeriodSource.DEFINITIVE_RECEIPT),

    /**
     * El modo retro: el resultado es el <b>calculo vigente</b> de un mes cerrado, y <b>no se toca su
     * recibo</b>.
     *
     * <p>Lo de otro mes se lee del vigente si existe, porque en una retro se recalcula hacia delante
     * desde el mes mas antiguo tocado.
     */
    CURRENT_CALCULATION(PreviousPeriodSource.CURRENT_CALCULATION_FIRST);

    private final PreviousPeriodSource previousPeriodSource;

    PayrollCalculationMode(PreviousPeriodSource previousPeriodSource) {
        this.previousPeriodSource = previousPeriodSource;
    }

    /** De donde lee este modo lo que vale otro mes. */
    public PreviousPeriodSource previousPeriodSource() {
        return previousPeriodSource;
    }

    public boolean isRetro() {
        return this == CURRENT_CALCULATION;
    }
}
