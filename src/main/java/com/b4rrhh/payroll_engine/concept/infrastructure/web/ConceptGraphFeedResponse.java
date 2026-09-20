package com.b4rrhh.payroll_engine.concept.infrastructure.web;

import java.time.LocalDate;

/**
 * Una arista de alimentacion, con el concepto al que apunta ({@code designer#15}).
 *
 * <p>Las fechas viajan aunque el disenador dibuje igual lo vigente y lo que no: son lo que
 * distingue una alimentacion que ya caduco de una que empieza el mes que viene, y quitarlas aqui
 * obligaria a pedir el concepto suelto para saberlo.
 */
public record ConceptGraphFeedResponse(
        String conceptCode,
        String sourceObjectCode,
        boolean invertSign,
        LocalDate effectiveFrom,
        LocalDate effectiveTo
) {
}
