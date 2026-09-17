package com.b4rrhh.payroll.application.service;

import com.b4rrhh.payroll.domain.model.Payroll;

/**
 * Si las reglas han cambiado desde que un recibo se calculó ({@code backend#107}).
 *
 * <p>Va en el recibo y no en el flujo de la edición, y eso es una decisión, no una comodidad: una
 * edición afecta a <b>todos</b> los recibos del sistema de reglas, y decirlo sólo al que acaba de
 * editar dejaría a los demás rancios en silencio. Quien abra cualquiera de ellos tiene que verlo.
 *
 * <p>Deja de decirlo al recalcular sin que nadie lo apague: la comparación es contra el
 * {@code calculated_at}, y recalcular lo mueve por delante del cambio.
 */
public interface PayrollRuleFreshness {

    /**
     * @return {@code true} si la reglamentación de su sistema de reglas se tocó después de
     *         calcularse este recibo
     */
    boolean rulesChangedSinceCalculation(Payroll payroll);
}
