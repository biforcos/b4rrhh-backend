package com.b4rrhh.payroll_engine.execution.domain.port;

import java.time.LocalDate;
import java.util.Optional;

/**
 * En que modalidad de desempleo cotiza un contrato ({@code backend#124}).
 *
 * <p>Devuelve {@code INDEFINIDA} o {@code DETERMINADA}, que es el sufijo con el que se busca el
 * tipo en {@code ss_cotizacion_tipos}. Vacio cuando el contrato no tiene modalidad declarada para
 * esa fecha: quien pregunta tiene que parar la corrida, no elegir una por defecto.
 */
public interface SsDesempleoModalidadRepository {

    Optional<String> findModalidad(String ruleSystemCode, String contractCode, LocalDate referenceDate);
}
