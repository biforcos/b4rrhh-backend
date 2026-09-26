package com.b4rrhh.payroll.retro.application.usecase;

/**
 * @param discardedBy   quien lo decide. Lo rellena la capa web con el sujeto del token
 * @param discardReason por que. Obligatorio: sin motivo el recibo no puede contar que la habia
 */
public record DiscardRetroMarkCommand(
        Long id,
        String discardedBy,
        String discardReason
) {
}
