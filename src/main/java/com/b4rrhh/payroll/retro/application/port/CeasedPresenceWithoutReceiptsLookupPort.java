package com.b4rrhh.payroll.retro.application.port;

import java.util.Set;

/**
 * Las presencias de un empleado a las que <b>no les queda ningun recibo</b> que pueda pagar una marca
 * ({@code backend#139}): cesaron, y el recibo del mes de su cese ya esta cerrado.
 *
 * <p>Una marca activa de una de esas presencias no la va a pagar nadie. El plan de retro es por
 * presencia (ADR-076, {@code backend#133}), y pagarla en la presencia nueva seria un finiquito
 * complementario, que es otro recibo y otro camino (ADR-076 §8). Lo que queda es decirlo.
 */
public interface CeasedPresenceWithoutReceiptsLookupPort {

    Set<Integer> findPresenceNumbers(String ruleSystemCode, String employeeTypeCode, String employeeNumber);
}
