package com.b4rrhh.payroll.application.usecase;

/**
 * La cuenta de un cierre masivo, y el entregable del {@code backend#102}.
 *
 * <h2>Por que los contadores no son adorno</h2>
 *
 * Porque {@code NOT_VALID → DEFINITIVE} no existe, y no existe por una razon: cerrar dice «esto ya
 * no se toca», y un recibo invalido no tiene nada que no tocar. Asi que un cierre de 873 cerrara
 * 870 y dejara fuera tres, y el unico sitio donde eso se puede explicar sin un texto de ayuda es en
 * la cuenta: <b>un contador que separa «no apta por su estado» de todo lo demas ensena la maquina
 * de estados en vez de explicarla.</b> Meterlas en un «fallidas» generico seria decir que algo salio
 * mal, y no salio mal nada.
 *
 * <h2>Que cuenta cada uno</h2>
 *
 * <ol>
 *   <li><b>{@code totalCandidates}</b> — las unidades que salieron del selector de objetivo:
 *       una por presencia relevante, no una por empleado.</li>
 *   <li><b>{@code totalFound}</b> — de esas, las que tenian recibo.</li>
 *   <li><b>{@code totalFinalized}</b> — cerradas aqui. Se cierra desde {@code CALCULATED} y desde
 *       {@code EXPLICIT_VALIDATED}, que son las dos que el {@code backend#90} admite.</li>
 *   <li><b>{@code totalSkippedAlreadyDefinitive}</b> — ya estaban cerradas. No pide nada de nadie:
 *       es lo esperable al repetir el cierre de un periodo.</li>
 *   <li><b>{@code totalSkippedNotEligibleByStatus}</b> — tenian recibo y su estado no admite el
 *       cierre; hoy eso es exactamente {@code NOT_VALID}. <b>Este es el que vale.</b></li>
 *   <li><b>{@code totalSkippedNotFound}</b> — la unidad no tenia recibo. No se calculo, o se
 *       calculo para otro tipo de nomina.</li>
 * </ol>
 *
 * <p><b>La particion:</b> {@code totalCandidates = totalFinalized + totalSkippedAlreadyDefinitive +
 * totalSkippedNotEligibleByStatus + totalSkippedNotFound}, y {@code totalFound} es
 * {@code totalCandidates - totalSkippedNotFound}. Como en {@code CalculationRun}, {@code totalFound}
 * no es un cajon: es una etapa, y sumarlo con los demas cuenta dos veces.
 *
 * <p>No lleva {@code statusReasonCode}, y la ausencia es deliberada: el cierre no da un motivo,
 * conserva el que el recibo tuviera. Invalidar si lo pide, porque invalidar es una decision sobre
 * algo que estaba bien.
 */
public record BulkFinalizePayrollResult(
        String ruleSystemCode,
        String payrollPeriodCode,
        String payrollTypeCode,
        int totalCandidates,
        int totalFound,
        int totalFinalized,
        int totalSkippedAlreadyDefinitive,
        int totalSkippedNotEligibleByStatus,
        int totalSkippedNotFound
) {
}
