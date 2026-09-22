package com.b4rrhh.payroll_engine.execution.domain.port;

import com.b4rrhh.payroll_engine.execution.domain.model.SsTarifaPrimaAt;

import java.time.LocalDate;
import java.util.Optional;

/**
 * La tarifa de primas de accidentes de trabajo, leida del catalogo ({@code backend#122}).
 *
 * <p>Hermano de {@link SsCotizacionTiposRepository} y de {@link SsCotizacionTopesRepository}:
 * resuelve la misma clase de pregunta —<b>que numero manda en esta fecha</b>— con una diferencia
 * que no se puede esconder en la firma.
 *
 * <h2>La entrada mas especifica gana</h2>
 *
 * <p>La tarifa no lista todos los CNAE: lista entradas de dos, tres o cuatro digitos, y una
 * entrada cubre a todos los CNAE que empiezan por ella salvo a los que estan listados aparte. Asi
 * que la busqueda es por <b>prefijo mas largo</b> y no por igualdad: el CNAE {@code 4719} de las
 * empresas de la demo encuentra su tipo en la entrada {@code 47}, y el dia que alguien siembre el
 * {@code 4781} —que la tarifa lista aparte— ese ganara para las empresas que lo tengan.
 *
 * <p>Buscar por igualdad habria funcionado sembrando una fila por CNAE usado, y habria dejado
 * <b>sin tipo</b> a la primera empresa con un CNAE que nadie hubiera sembrado. Con el prefijo, lo
 * que hace falta sembrar es la tarifa, no la lista de empresas.
 */
public interface SsTarifaPrimasAtRepository {

    /**
     * La entrada de la tarifa que cubre a este CNAE en esta fecha, o vacio si ninguna lo cubre.
     */
    Optional<SsTarifaPrimaAt> findForCnae(String ruleSystemCode, String cnaeCode, LocalDate referenceDate);
}
