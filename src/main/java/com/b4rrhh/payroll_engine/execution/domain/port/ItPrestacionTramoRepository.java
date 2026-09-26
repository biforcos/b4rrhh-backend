package com.b4rrhh.payroll_engine.execution.domain.port;

import com.b4rrhh.payroll_engine.execution.domain.model.ItPrestacionTramo;

import java.time.LocalDate;
import java.util.Optional;

public interface ItPrestacionTramoRepository {

    /**
     * El tramo vigente de la prestacion por incapacidad temporal ({@code backend#129}).
     *
     * <p>El tipo de ausencia esta en la firma porque los tramos no son los mismos: la enfermedad comun
     * empieza el cuarto dia y el accidente de trabajo el dia siguiente a la baja, y cuando el accidente
     * entre, sera una fila mas y no una clase mas.
     *
     * @param tramoCode el codigo del tramo, que es lo que ata cada concepto del motor a su fila
     */
    Optional<ItPrestacionTramo> findActive(
            String ruleSystemCode,
            String absenceTypeCode,
            String tramoCode,
            LocalDate referenceDate);
}
