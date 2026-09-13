package com.b4rrhh.payroll_engine.metamodel.domain.port;

import com.b4rrhh.payroll_engine.metamodel.domain.model.RuleSystemMetamodel;

import java.time.LocalDate;

/**
 * Puerto de salida para leer la reglamentación de un sistema de reglas de una vez.
 *
 * <p>Se llama <strong>una vez por ejecución</strong>, no una vez por unidad: lo que
 * devuelve es el dato contra el que se calcula toda la corrida. Ver
 * {@link RuleSystemMetamodel} para por qué eso es una regla y no una optimización.
 */
public interface RuleSystemMetamodelRepository {

    /**
     * Carga los conceptos del sistema de reglas con sus operandos, las alimentaciones
     * vigentes en {@code referenceDate} y las asignaciones válidas en esa misma fecha.
     *
     * @param ruleSystemCode sistema de reglas de la ejecución
     * @param referenceDate  fecha contra la que se resuelven las vigencias — la de la
     *                       ejecución, la misma para todas sus unidades
     */
    RuleSystemMetamodel load(String ruleSystemCode, LocalDate referenceDate);
}
