package com.b4rrhh.payroll_engine.concept.infrastructure.persistence;

import com.b4rrhh.payroll_engine.concept.domain.model.CalculationType;
import com.b4rrhh.payroll_engine.concept.domain.model.ExecutionScope;
import com.b4rrhh.payroll_engine.concept.domain.model.FunctionalNature;
import com.b4rrhh.payroll_engine.concept.domain.model.PayrollConcept;
import com.b4rrhh.payroll_engine.concept.domain.port.PayrollConceptRepository;
import com.b4rrhh.payroll_engine.object.domain.model.PayrollObject;
import com.b4rrhh.payroll_engine.object.domain.model.PayrollObjectTypeCode;
import com.b4rrhh.payroll_engine.object.infrastructure.persistence.PayrollObjectEntity;
import com.b4rrhh.payroll_engine.object.infrastructure.persistence.SpringDataPayrollObjectRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class PayrollConceptPersistenceAdapter implements PayrollConceptRepository {

    private final SpringDataPayrollConceptRepository conceptRepository;
    private final SpringDataPayrollObjectRepository objectRepository;

    public PayrollConceptPersistenceAdapter(
            SpringDataPayrollConceptRepository conceptRepository,
            SpringDataPayrollObjectRepository objectRepository
    ) {
        this.conceptRepository = conceptRepository;
        this.objectRepository = objectRepository;
    }

    @Override
    public PayrollConcept save(PayrollConcept concept) {
        PayrollObjectEntity objectEntity = objectRepository
                .findByRuleSystemCodeAndObjectTypeCodeAndObjectCode(
                        concept.getRuleSystemCode(),
                        PayrollObjectTypeCode.CONCEPT.name(),
                        concept.getConceptCode()
                )
                .orElseThrow(() -> new IllegalStateException(
                        "Cannot save PayrollConcept: base PayrollObject not found for "
                                + concept.getRuleSystemCode() + "/" + concept.getConceptCode()
                ));

        PayrollConceptEntity entity = toEntity(concept, objectEntity);
        PayrollConceptEntity saved = conceptRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<PayrollConcept> findByBusinessKey(String ruleSystemCode, String conceptCode) {
        return conceptRepository
                .findByRuleSystemCodeAndConceptCode(ruleSystemCode, conceptCode)
                .map(this::toDomain);
    }

    @Override
    public boolean existsByBusinessKey(String ruleSystemCode, String conceptCode) {
        return conceptRepository.existsByRuleSystemCodeAndConceptCode(ruleSystemCode, conceptCode);
    }

    @Override
    public List<PayrollConcept> findAllByCodes(String ruleSystemCode, Collection<String> conceptCodes) {
        if (conceptCodes == null || conceptCodes.isEmpty()) {
            return List.of();
        }
        return conceptRepository.findAllByRuleSystemCodeAndConceptCodes(ruleSystemCode, conceptCodes)
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PayrollConcept> findAllByRuleSystemCode(String ruleSystemCode) {
        return conceptRepository.findAllByRuleSystemCode(ruleSystemCode)
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteByBusinessKey(String ruleSystemCode, String conceptCode) {
        conceptRepository.deleteByRuleSystemCodeAndConceptCode(ruleSystemCode, conceptCode);
    }

    private PayrollConceptEntity toEntity(PayrollConcept domain, PayrollObjectEntity objectEntity) {
        PayrollConceptEntity entity = new PayrollConceptEntity();
        entity.setPayrollObject(objectEntity);
        entity.setConceptMnemonic(domain.getConceptMnemonic());
        entity.setCalculationType(domain.getCalculationType().name());
        entity.setFunctionalNature(domain.getFunctionalNature().name());
        entity.setPayslipOrderCode(domain.getPayslipOrderCode());
        entity.setExecutionScope(domain.getExecutionScope().name());
        entity.setSummary(domain.getSummary());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }

    private PayrollConcept toDomain(PayrollConceptEntity entity) {
        PayrollObjectEntity obj = entity.getPayrollObject();
        PayrollObject domainObject = new PayrollObject(
                obj.getId(),
                obj.getRuleSystemCode(),
                PayrollObjectTypeCode.valueOf(obj.getObjectTypeCode()),
                obj.getObjectCode(),
                obj.getCreatedAt(),
                obj.getUpdatedAt()
        );
        return new PayrollConcept(
                domainObject,
                entity.getConceptMnemonic(),
                CalculationType.valueOf(entity.getCalculationType()),
                FunctionalNature.valueOf(entity.getFunctionalNature()),
                entity.getPayslipOrderCode(),
                ExecutionScope.valueOf(entity.getExecutionScope()),
                entity.getSummary(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
