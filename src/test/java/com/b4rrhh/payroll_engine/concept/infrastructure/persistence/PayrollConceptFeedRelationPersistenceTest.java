package com.b4rrhh.payroll_engine.concept.infrastructure.persistence;

import com.b4rrhh.support.TestSobreEsquemaReal;
import com.b4rrhh.payroll_engine.object.infrastructure.persistence.PayrollObjectEntity;
import com.b4rrhh.payroll_engine.object.infrastructure.persistence.SpringDataPayrollObjectRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestSobreEsquemaReal
// Los codigos de objeto que usa el test no estan entre los que siembran las
// migraciones.
class PayrollConceptFeedRelationPersistenceTest {

    @Autowired
    private SpringDataPayrollObjectRepository objectRepository;

    @Autowired
    private SpringDataPayrollConceptFeedRelationRepository feedRelationRepository;

    @Test
    void persistsFeedRelationWithNullableEffectiveTo() {
        PayrollObjectEntity source = saveObject("ESP", "CONCEPT", "SALBASE");
        PayrollObjectEntity target = saveObject("ESP", "CONCEPT", "IRPF");

        PayrollConceptFeedRelationEntity relation = buildRelation(
                source, target, "FEED_BY_SOURCE", null,
                LocalDate.of(2025, 1, 1), null
        );
        feedRelationRepository.saveAndFlush(relation);

        List<PayrollConceptFeedRelationEntity> results = feedRelationRepository
                .findActiveByTargetObjectId(target.getId(), LocalDate.of(2025, 6, 1));

        assertEquals(1, results.size());
        assertEquals("FEED_BY_SOURCE", results.get(0).getFeedMode());
        assertNull(results.get(0).getEffectiveTo());
    }

    @Test
    void persistsFeedRelationWithFeedValue() {
        PayrollObjectEntity source = saveObject("ESP", "CONCEPT", "SALBASE2");
        PayrollObjectEntity target = saveObject("ESP", "CONCEPT", "IRPF2");

        PayrollConceptFeedRelationEntity relation = buildRelation(
                source, target, "FEED_BY_SOURCE", new BigDecimal("0.35"),
                LocalDate.of(2025, 1, 1), null
        );
        feedRelationRepository.saveAndFlush(relation);

        List<PayrollConceptFeedRelationEntity> results = feedRelationRepository
                .findActiveByTargetObjectId(target.getId(), LocalDate.of(2025, 3, 1));

        assertEquals(1, results.size());
        assertEquals(0, new BigDecimal("0.35").compareTo(results.get(0).getFeedValue()));
    }

    @Test
    void doesNotReturnExpiredRelations() {
        PayrollObjectEntity source = saveObject("ESP", "CONCEPT", "SALBASE3");
        PayrollObjectEntity target = saveObject("ESP", "CONCEPT", "IRPF3");

        PayrollConceptFeedRelationEntity expired = buildRelation(
                source, target, "FEED_BY_SOURCE", null,
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31)
        );
        feedRelationRepository.saveAndFlush(expired);

        List<PayrollConceptFeedRelationEntity> results = feedRelationRepository
                .findActiveByTargetObjectId(target.getId(), LocalDate.of(2025, 6, 1));

        assertTrue(results.isEmpty());
    }

    @Test
    void feedRelationWithMissingSourceFails() {
        PayrollObjectEntity target = saveObject("ESP", "CONCEPT", "TARGET_ONLY");

        PayrollConceptFeedRelationEntity relation = new PayrollConceptFeedRelationEntity();
        relation.setSourceObject(null);
        relation.setTargetObject(target);
        relation.setFeedMode("FEED_BY_SOURCE");
        relation.setEffectiveFrom(LocalDate.of(2025, 1, 1));

        assertThrows(Exception.class, () -> feedRelationRepository.saveAndFlush(relation));
    }

    private PayrollObjectEntity saveObject(String ruleSystemCode, String objectTypeCode, String objectCode) {
        PayrollObjectEntity entity = new PayrollObjectEntity();
        entity.setRuleSystemCode(ruleSystemCode);
        entity.setObjectTypeCode(objectTypeCode);
        entity.setObjectCode(objectCode);
        return objectRepository.saveAndFlush(entity);
    }

    private PayrollConceptFeedRelationEntity buildRelation(
            PayrollObjectEntity source,
            PayrollObjectEntity target,
            String feedMode,
            BigDecimal feedValue,
            LocalDate effectiveFrom,
            LocalDate effectiveTo
    ) {
        PayrollConceptFeedRelationEntity entity = new PayrollConceptFeedRelationEntity();
        entity.setSourceObject(source);
        entity.setTargetObject(target);
        entity.setFeedMode(feedMode);
        entity.setFeedValue(feedValue);
        entity.setEffectiveFrom(effectiveFrom);
        entity.setEffectiveTo(effectiveTo);
        return entity;
    }
}
