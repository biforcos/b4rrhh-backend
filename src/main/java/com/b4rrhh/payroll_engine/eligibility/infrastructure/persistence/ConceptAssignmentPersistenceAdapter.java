package com.b4rrhh.payroll_engine.eligibility.infrastructure.persistence;

import com.b4rrhh.payroll_engine.eligibility.domain.model.ConceptAssignment;
import com.b4rrhh.payroll_engine.eligibility.domain.model.EmployeeAssignmentContext;
import com.b4rrhh.payroll_engine.eligibility.domain.port.ConceptAssignmentRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public class ConceptAssignmentPersistenceAdapter implements ConceptAssignmentRepository {

    private final SpringDataConceptAssignmentRepository springDataRepo;

    public ConceptAssignmentPersistenceAdapter(SpringDataConceptAssignmentRepository springDataRepo) {
        this.springDataRepo = springDataRepo;
    }

    @Override
    public ConceptAssignment save(ConceptAssignment assignment) {
        ConceptAssignmentEntity entity = toEntity(assignment);
        ConceptAssignmentEntity saved = springDataRepo.save(entity);
        return toDomain(saved);
    }

    @Override
    public List<ConceptAssignment> findApplicableAssignments(EmployeeAssignmentContext context, LocalDate referenceDate) {
        return springDataRepo.findCandidates(
                context.getRuleSystemCode(),
                referenceDate,
                context.getCompanyCode(),
                context.getAgreementCode(),
                context.getEmployeeTypeCode()
        ).stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConceptAssignment> findAllValidByRuleSystemCode(String ruleSystemCode, LocalDate referenceDate) {
        return springDataRepo.findAllValidByRuleSystemCode(ruleSystemCode, referenceDate)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConceptAssignment> findAllByRuleSystemCode(String ruleSystemCode) {
        return springDataRepo.findAllByRuleSystemCode(ruleSystemCode)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConceptAssignment> findAllByRuleSystemCodeAndConceptCode(String ruleSystemCode, String conceptCode) {
        return springDataRepo.findAllByRuleSystemCodeAndConceptCode(ruleSystemCode, conceptCode)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ConceptAssignment> findByIdAndRuleSystemCode(Long id, String ruleSystemCode) {
        if (id == null) return Optional.empty();
        return springDataRepo.findByIdAndRuleSystemCode(id, ruleSystemCode)
                .map(this::toDomain);
    }

    @Override
    public void deleteById(Long id) {
        if (id == null) {
            return;
        }
        springDataRepo.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByIdAndRuleSystemCode(Long id, String ruleSystemCode) {
        if (id == null) {
            return false;
        }
        return springDataRepo.existsByIdAndRuleSystemCode(id, ruleSystemCode);
    }

    private ConceptAssignmentEntity toEntity(ConceptAssignment domain) {
        ConceptAssignmentEntity e = new ConceptAssignmentEntity();
        if (domain.getId() != null) {
            e.setId(domain.getId());
        }
        e.setRuleSystemCode(domain.getRuleSystemCode());
        e.setConceptCode(domain.getConceptCode());
        e.setCompanyCode(domain.getCompanyCode());
        e.setAgreementCode(domain.getAgreementCode());
        e.setEmployeeTypeCode(domain.getEmployeeTypeCode());
        e.setValidFrom(domain.getValidFrom());
        e.setValidTo(domain.getValidTo());
        e.setPriority(domain.getPriority());
        if (domain.getCreatedAt() != null) e.setCreatedAt(domain.getCreatedAt());
        return e;
    }

    private ConceptAssignment toDomain(ConceptAssignmentEntity e) {
        return new ConceptAssignment(
                e.getId(),
                e.getRuleSystemCode(),
                e.getConceptCode(),
                e.getCompanyCode(),
                e.getAgreementCode(),
                e.getEmployeeTypeCode(),
                e.getValidFrom(),
                e.getValidTo(),
                e.getPriority(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }
}
