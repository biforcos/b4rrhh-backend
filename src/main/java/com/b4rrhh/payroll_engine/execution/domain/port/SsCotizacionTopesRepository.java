package com.b4rrhh.payroll_engine.execution.domain.port;

import com.b4rrhh.payroll_engine.execution.domain.model.SsCotizacionTope;

import java.time.LocalDate;
import java.util.Optional;

public interface SsCotizacionTopesRepository {

    /**
     * Los topes vigentes de una contingencia para un grupo de cotizacion ({@code backend#121}).
     *
     * <p>La contingencia esta en la firma porque las dos bases no se recortan igual: el tope
     * maximo es el mismo, pero el minimo de las contingencias profesionales lo fija la Orden de
     * cotizacion y <b>no es la base minima del grupo</b>. Sin ese parametro la pregunta «cual es
     * el minimo de este empleado» tenia una sola respuesta y hacian falta dos.
     *
     * @param contingencyCode {@code COMUNES} o {@code PROFESIONALES}.
     */
    Optional<SsCotizacionTope> findActive(
            String ruleSystemCode, String grupoCode, String periodType,
            String contingencyCode, LocalDate referenceDate);
}
