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
     * Removes every feed relation whose target concept matches the given business key.
     * The operation is a no-op when no feed exists; it never raises.
     */
    void deleteAllByRuleSystemCodeAndTargetConceptCode(
            String ruleSystemCode, String conceptCode);
}
