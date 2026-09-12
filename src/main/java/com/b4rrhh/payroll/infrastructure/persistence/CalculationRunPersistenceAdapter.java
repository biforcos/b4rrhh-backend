package com.b4rrhh.payroll.infrastructure.persistence;

import com.b4rrhh.payroll.domain.model.CalculationRun;
import com.b4rrhh.payroll.domain.port.CalculationRunRepository;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
public class CalculationRunPersistenceAdapter implements CalculationRunRepository {

    private final SpringDataCalculationRunRepository springDataCalculationRunRepository;

    public CalculationRunPersistenceAdapter(SpringDataCalculationRunRepository springDataCalculationRunRepository) {
        this.springDataCalculationRunRepository = springDataCalculationRunRepository;
    }

    @Override
    public CalculationRun save(CalculationRun calculationRun) {
        return toDomain(springDataCalculationRunRepository.save(toEntity(calculationRun)));
    }

    @Override
    public Optional<CalculationRun> findById(Long id) {
        return springDataCalculationRunRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<CalculationRun> findByStatusIn(Collection<String> statuses) {
        return springDataCalculationRunRepository.findByStatusIn(statuses).stream()
                .map(this::toDomain)
                .toList();
    }

    private CalculationRun toDomain(CalculationRunEntity entity) {
        return new CalculationRun(
                entity.getId(),
                entity.getRuleSystemCode(),
                entity.getPayrollPeriodCode(),
                entity.getPayrollTypeCode(),
                entity.getCalculationEngineCode(),
                entity.getCalculationEngineVersion(),
                entity.getRequestedAt(),
                entity.getRequestedBy(),
                entity.getStatus(),
                entity.getTargetSelectionJson(),
                entity.getTotalCandidates(),
                entity.getTotalEligible(),
                entity.getTotalClaimed(),
                entity.getTotalSkippedNotEligible(),
                entity.getTotalSkippedAlreadyClaimed(),
                entity.getTotalCalculated(),
                entity.getTotalNotValid(),
                entity.getTotalErrors(),
                entity.getStartedAt(),
                entity.getFinishedAt(),
                entity.getSummaryJson(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private CalculationRunEntity toEntity(CalculationRun calculationRun) {
        CalculationRunEntity entity = new CalculationRunEntity();
        entity.setId(calculationRun.id());
        entity.setRuleSystemCode(calculationRun.ruleSystemCode());
        entity.setPayrollPeriodCode(calculationRun.payrollPeriodCode());
        entity.setPayrollTypeCode(calculationRun.payrollTypeCode());
        entity.setCalculationEngineCode(calculationRun.calculationEngineCode());
        entity.setCalculationEngineVersion(calculationRun.calculationEngineVersion());
        entity.setRequestedAt(calculationRun.requestedAt());
        entity.setRequestedBy(calculationRun.requestedBy());
        entity.setStatus(calculationRun.status());
        entity.setTargetSelectionJson(calculationRun.targetSelectionJson());
        entity.setTotalCandidates(calculationRun.totalCandidates());
        entity.setTotalEligible(calculationRun.totalEligible());
        entity.setTotalClaimed(calculationRun.totalClaimed());
        entity.setTotalSkippedNotEligible(calculationRun.totalSkippedNotEligible());
        entity.setTotalSkippedAlreadyClaimed(calculationRun.totalSkippedAlreadyClaimed());
        entity.setTotalCalculated(calculationRun.totalCalculated());
        entity.setTotalNotValid(calculationRun.totalNotValid());
        entity.setTotalErrors(calculationRun.totalErrors());
        entity.setStartedAt(calculationRun.startedAt());
        entity.setFinishedAt(calculationRun.finishedAt());
        entity.setSummaryJson(calculationRun.summaryJson());
        entity.setCreatedAt(calculationRun.createdAt());
        entity.setUpdatedAt(calculationRun.updatedAt());
        return entity;
    }
}