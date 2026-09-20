package com.b4rrhh.payroll_engine.concept.domain.port;

import com.b4rrhh.payroll_engine.concept.domain.model.PayrollConceptFeedRelation;

import java.time.LocalDate;
import java.util.List;

public interface PayrollConceptFeedRelationRepository {

    PayrollConceptFeedRelation save(PayrollConceptFeedRelation feedRelation);

    List<PayrollConceptFeedRelation> findActiveByTargetObjectId(Long targetObjectId, LocalDate referenceDate);

    /**
     * Returns every feed relation whose target concept matches the given business key,
     * ordered by source object code. Returns an empty list when no feed is configured.
     */
    List<PayrollConceptFeedRelation> findByRuleSystemCodeAndTargetConceptCode(
            String ruleSystemCode, String conceptCode);

    /**
     * Returns every feed relation active on the reference date whose target object belongs to
     * the given rule system. This is the bulk read the execution metamodel is loaded with:
     * one call per execution instead of one per concept and unit.
     */
    List<PayrollConceptFeedRelation> findAllActiveByRuleSystemCode(
            String ruleSystemCode, LocalDate referenceDate);

    /**
     * Todas las alimentaciones declaradas en el sistema de reglas, <b>sin filtrar por fecha</b>,
     * ordenadas por concepto destino. Es la lectura del grafo entero del disenador
     * ({@code designer#15}): una consulta en lugar de una por concepto.
     *
     * <p>No confundir con {@link #findAllActiveByRuleSystemCode}: aquella responde que alimenta a
     * que <b>hoy</b>, que es lo que el motor necesita para calcular. Esta responde que hay
     * declarado, que es lo que el disenador dibuja, y por eso incluye lo que aun no esta vigente
     * y lo que ya caduco. Son dos preguntas y confundirlas dejaria aristas sin pintar.
     */
    List<PayrollConceptFeedRelation> findAllByRuleSystemCode(String ruleSystemCode);

    /**
     * Removes every feed relation whose target concept matches the given business key.
     * The operation is a no-op when no feed exists; it never raises.
     */
    void deleteAllByRuleSystemCodeAndTargetConceptCode(
            String ruleSystemCode, String conceptCode);
}
