package com.b4rrhh.payroll_engine.concept.infrastructure.persistence;

import com.b4rrhh.payroll_engine.concept.domain.model.OperandRole;
import com.b4rrhh.payroll_engine.concept.domain.model.PayrollConceptOperand;
import com.b4rrhh.payroll_engine.concept.domain.port.PayrollConceptOperandRepository;
import com.b4rrhh.payroll_engine.object.domain.model.PayrollObject;
import com.b4rrhh.payroll_engine.object.domain.model.PayrollObjectTypeCode;
import com.b4rrhh.payroll_engine.object.infrastructure.persistence.PayrollObjectEntity;
import com.b4rrhh.payroll_engine.object.infrastructure.persistence.SpringDataPayrollObjectRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PayrollConceptOperandPersistenceAdapter implements PayrollConceptOperandRepository {

    private final SpringDataPayrollConceptOperandRepository operandRepository;
    private final SpringDataPayrollObjectRepository objectRepository;

    public PayrollConceptOperandPersistenceAdapter(
            SpringDataPayrollConceptOperandRepository operandRepository,
            SpringDataPayrollObjectRepository objectRepository
    ) {
        this.operandRepository = operandRepository;
        this.objectRepository = objectRepository;
    }

    @Override
    public PayrollConceptOperand save(PayrollConceptOperand operand) {
        PayrollObjectEntity targetEntity = requireObject(
                operand.getTargetObject().getRuleSystemCode(),
                operand.getTargetObject().getObjectCode()
        );
        PayrollObjectEntity sourceEntity = requireObject(
                operand.getSourceObject().getRuleSystemCode(),
                operand.getSourceObject().getObjectCode()
        );
        PayrollConceptOperandEntity entity = toEntity(operand, targetEntity, sourceEntity);
        return toDomain(operandRepository.save(entity));
    }

    @Override
    public List<PayrollConceptOperand> findByTarget(String ruleSystemCode, String conceptCode) {
        return operandRepository
                .findByTargetObject_RuleSystemCodeAndTargetObject_ObjectCode(ruleSystemCode, conceptCode)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<PayrollConceptOperand> findByRuleSystemCodeAndConceptCode(
            String ruleSystemCode, String conceptCode) {
        return operandRepository
                .findByRuleSystemCodeAndConceptCode(ruleSystemCode, conceptCode)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<PayrollConceptOperand> findAllByRuleSystemCode(String ruleSystemCode) {
        return operandRepository
                .findAllByRuleSystemCodeFetchingObjects(ruleSystemCode)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void deleteAllByRuleSystemCodeAndConceptCode(String ruleSystemCode, String conceptCode) {
        operandRepository.deleteAllByRuleSystemCodeAndConceptCode(ruleSystemCode, conceptCode);
    }

    private PayrollObjectEntity requireObject(String ruleSystemCode, String objectCode) {
        return objectRepository
                .findByRuleSystemCodeAndObjectTypeCodeAndObjectCode(
                        ruleSystemCode,
                        PayrollObjectTypeCode.CONCEPT.name(),
                        objectCode
                )
                .orElseThrow(() -> new IllegalStateException(
                        "PayrollObject not found for " + ruleSystemCode + "/CONCEPT/" + objectCode));
    }

    private PayrollConceptOperandEntity toEntity(
            PayrollConceptOperand domain,
            PayrollObjectEntity targetEntity,
            PayrollObjectEntity sourceEntity
    ) {
        PayrollConceptOperandEntity entity = new PayrollConceptOperandEntity();
        entity.setTargetObject(targetEntity);
        entity.setOperandRole(domain.getOperandRole().name());
        entity.setSourceObject(sourceEntity);
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }

    private PayrollConceptOperand toDomain(PayrollConceptOperandEntity entity) {
        return new PayrollConceptOperand(
                entity.getId(),
                toPayrollObject(entity.getTargetObject()),
                OperandRole.valueOf(entity.getOperandRole()),
                toPayrollObject(entity.getSourceObject()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private PayrollObject toPayrollObject(PayrollObjectEntity obj) {
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
