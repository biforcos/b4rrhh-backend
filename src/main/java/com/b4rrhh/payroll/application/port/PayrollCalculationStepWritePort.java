package com.b4rrhh.payroll.application.port;

import java.util.List;

/**
 * Escribe los pasos con los que se calculó un recibo. Y sólo escribe.
 *
 * <p><b>No hay método de lectura, y esa es la mitad del motivo de que este puerto exista.</b> Los
 * pasos todavía no los sirve nadie: el contrato no cambia en el {@code backend#93} y pintarlos en
 * la Valorización es el paso siguiente. Un puerto con un {@code findByPayrollId} devolvería lista
 * vacía cuando nadie la hubiera llamado nunca, y «vacío» no se distingue de «este recibo no tiene
 * pasos». Es la forma que llevamos toda la semana quitando —{@code WorkCenterProfile.empty()}, el
 * {@code company_profile}, el {@code ofDefault()} fiscal—, y aquí se evita por construcción: que el
 * código diga «esto no se lee todavía» no teniendo por dónde leerlo vale más que un comentario.
 *
 * <p>Cuando haya quien los lea, el puerto de lectura se añade entonces, con su consumidor delante.
 */
public interface PayrollCalculationStepWritePort {

    /**
     * Guarda los pasos de un recibo recién calculado.
     *
     * <p>No borra nada primero, a propósito: un recálculo borra el recibo anterior y crea uno
     * nuevo ({@code CalculatePayrollService}), y la {@code on delete cascade} se lleva sus pasos
     * con él. Si alguna vez se escribieran dos veces los pasos del mismo recibo, la clave
     * {@code (payroll_id, execution_order)} lo pararía con un error en vez de dejar el recibo con
     * pasos de dos cálculos.
     */
    void writeStepsOf(long payrollId, List<PayrollCalculationStep> steps);
}
