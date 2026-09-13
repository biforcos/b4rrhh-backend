package com.b4rrhh.payroll_engine.concept.infrastructure.persistence;

import com.b4rrhh.payroll_engine.concept.domain.model.FeedMode;
import com.b4rrhh.payroll_engine.concept.domain.model.PayrollConceptFeedRelation;
import com.b4rrhh.payroll_engine.concept.domain.port.PayrollConceptFeedRelationRepository;
import com.b4rrhh.payroll_engine.object.domain.model.PayrollObject;
import com.b4rrhh.payroll_engine.object.domain.model.PayrollObjectTypeCode;
import com.b4rrhh.payroll_engine.object.infrastructure.persistence.SpringDataPayrollObjectRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class PayrollConceptFeedRelationPersistenceAdapter implements PayrollConceptFeedRelationRepository {

    private final SpringDataPayrollConceptFeedRelationRepository feedRelationRepository;
    private final SpringDataPayrollObjectRepository objectRepository;

    public PayrollConceptFeedRelationPersistenceAdapter(
            SpringDataPayrollConceptFeedRelationRepository feedRelationRepository,
            SpringDataPayrollObjectRepository objectRepository
    ) {
        this.feedRelationRepository = feedRelationRepository;
        this.objectRepository = objectRepository;
    }

    @Override
    public PayrollConceptFeedRelation save(PayrollConceptFeedRelation relation) {
        var sourceEntity = objectRepository.findById(relation.getSourceObject().getId())
                .orElseThrow(() -> new IllegalStateException(
                        "Source PayrollObject not found with id: " + relation.getSourceObject().getId()
                ));
        var targetEntity = objectRepository.findById(relation.getTargetObject().getId())
                .orElseThrow(() -> new IllegalStateException(
                        "Target PayrollObject not found with id: " + relation.getTargetObject().getId()
                ));

        PayrollConceptFeedRelationEntity entity = new PayrollConceptFeedRelationEntity();
        entity.setSourceObject(sourceEntity);
        entity.setTargetObject(targetEntity);
        entity.setFeedMode(relation.getFeedMode().name());
        entity.setFeedValue(relation.getFeedValue());
        entity.setInvertSign(relation.isInvertSign());
        entity.setEffectiveFrom(relation.getEffectiveFrom());
        entity.setEffectiveTo(relation.getEffectiveTo());
        entity.setCreatedAt(relation.getCreatedAt());
        entity.setUpdatedAt(relation.getUpdatedAt());

        PayrollConceptFeedRelationEntity saved = feedRelationRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public List<PayrollConceptFeedRelation> findActiveByTargetObjectId(Long targetObjectId, LocalDate referenceDate) {
        return feedRelationRepository
                .findActiveByTargetObjectId(targetObjectId, referenceDate)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<PayrollConceptFeedRelation> findByRuleSystemCodeAndTargetConceptCode(
            String ruleSystemCode, String conceptCode) {
        return feedRelationRepository
                .findByRuleSystemCodeAndTargetConceptCode(ruleSystemCode, conceptCode)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<PayrollConceptFeedRelation> findAllActiveByRuleSystemCode(
            String ruleSystemCode, LocalDate referenceDate) {
        return feedRelationRepository
                .findAllActiveByRuleSystemCodeFetchingObjects(ruleSystemCode, referenceDate)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void deleteAllByRuleSystemCodeAndTargetConceptCode(String ruleSystemCode, String conceptCode) {
        feedRelationRepository.deleteAllByRuleSystemCodeAndTargetConceptCode(ruleSystemCode, conceptCode);
    }

    private PayrollConceptFeedRelation toDomain(PayrollConceptFeedRelationEntity entity) {
        PayrollObject source = toPayrollObjectDomain(entity.getSourceObject());
        PayrollObject target = toPayrollObjectDomain(entity.getTargetObject());
        return new PayrollConceptFeedRelation(
                entity.getId(),
                source,
                target,
                FeedMode.valueOf(entity.getFeedMode()),
                entity.getFeedValue(),
                entity.isInvertSign(),
                entity.getEffectiveFrom(),
                entity.getEffectiveTo(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private PayrollObject toPayrollObjectDomain(
            com.b4rrhh.payroll_engine.object.infrastructure.persistence.PayrollObjectEntity obj
    ) {
        return new PayrollObject(
                obj.getId(),
                obj.getRuleSystemCode(),
                PayrollObjectTypeCode.valueOf(obj.getObjectTypeCode()),
                obj.getObjectCode(),
                obj.getCreatedAt(),
                obj.getUpdatedAt()
        );
    }
}
