package com.b4rrhh.payroll.infrastructure.persistence;

import com.b4rrhh.payroll.domain.model.CalculationClaim;
import com.b4rrhh.payroll.domain.port.CalculationClaimRepository;
import org.springframework.stereotype.Component;

@Component
public class CalculationClaimPersistenceAdapter implements CalculationClaimRepository {

    private final SpringDataCalculationClaimRepository springDataCalculationClaimRepository;

    public CalculationClaimPersistenceAdapter(SpringDataCalculationClaimRepository springDataCalculationClaimRepository) {
        this.springDataCalculationClaimRepository = springDataCalculationClaimRepository;
    }

    @Override
    public CalculationClaim save(CalculationClaim calculationClaim) {
        CalculationClaimEntity entity = new CalculationClaimEntity();
        entity.setId(calculationClaim.id());
        entity.setCalculationRun(runReference(calculationClaim.runId()));
        entity.setRuleSystemCode(calculationClaim.ruleSystemCode());
        entity.setEmployeeTypeCode(calculationClaim.employeeTypeCode());
        entity.setEmployeeNumber(calculationClaim.employeeNumber());
        entity.setPayrollPeriodCode(calculationClaim.payrollPeriodCode());
        entity.setPayrollTypeCode(calculationClaim.payrollTypeCode());
        entity.setPresenceNumber(calculationClaim.presenceNumber());
        entity.setClaimedAt(calculationClaim.claimedAt());
        entity.setClaimedBy(calculationClaim.claimedBy());

        CalculationClaimEntity saved = springDataCalculationClaimRepository.save(entity);
        return new CalculationClaim(
                saved.getId(),
                saved.getCalculationRun().getId(),
                saved.getRuleSystemCode(),
                saved.getEmployeeTypeCode(),
                saved.getEmployeeNumber(),
                saved.getPayrollPeriodCode(),
                saved.getPayrollTypeCode(),
                saved.getPresenceNumber(),
                saved.getClaimedAt(),
                saved.getClaimedBy()
        );
    }

    @Override
    public void deleteById(Long id) {
        springDataCalculationClaimRepository.deleteById(id);
    }

    @Override
    public void deleteByRunId(Long runId) {
        springDataCalculationClaimRepository.deleteByCalculationRunId(runId);
    }

    @Override
    public long deleteAll() {
        // Se cuenta antes de borrar porque el numero es el diagnostico: un arranque que
        // dice "borrados 871 claims" esta contando una corrida entera que murio a medias.
        long claims = springDataCalculationClaimRepository.count();
        springDataCalculationClaimRepository.deleteAllInBatch();
        return claims;
    }

    private CalculationRunEntity runReference(Long runId) {
        CalculationRunEntity run = new CalculationRunEntity();
        run.setId(runId);
        return run;
    }
}