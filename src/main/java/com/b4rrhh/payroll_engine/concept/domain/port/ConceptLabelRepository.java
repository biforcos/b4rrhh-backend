package com.b4rrhh.payroll_engine.concept.domain.port;

import com.b4rrhh.payroll_engine.concept.domain.model.ConceptLabel;

import java.util.Map;
import java.util.Optional;

/**
 * Los nombres de los conceptos de un sistema de reglas ({@code backend#109}).
 */
public interface ConceptLabelRepository {

    /**
     * Los nombres de todos los conceptos del sistema de reglas en ese idioma, indexados por codigo
     * de concepto.
     *
     * <p>Se pide entero y una vez. Quien calcula un recibo necesita el nombre de cada concepto del
     * plan, y preguntarlos de uno en uno es la forma de convertir un catalogo de 38 en 38 lecturas
     * por unidad de calculo.
     *
     * <p><b>Un concepto sin nombre no aparece en el mapa.</b> No hay entrada vacia ni cadena en
     * blanco que disimule: quien lo lea decide que ensenar, y lo que ensena es el mnemonico.
     */
    Map<String, String> findLabelsByRuleSystemCode(String ruleSystemCode, String languageCode);

    /** El nombre de un concepto, si lo tiene. */
    Optional<ConceptLabel> findByBusinessKey(String ruleSystemCode, String conceptCode, String languageCode);

    /**
     * Escribe el nombre de un concepto, creandolo o sustituyendo el que hubiera.
     *
     * @throws com.b4rrhh.payroll_engine.concept.domain.exception.PayrollConceptNotFoundException
     *         si el concepto no existe: un nombre sin concepto al que colgarse no es un nombre.
     */
    ConceptLabel save(String ruleSystemCode, ConceptLabel label);
}
