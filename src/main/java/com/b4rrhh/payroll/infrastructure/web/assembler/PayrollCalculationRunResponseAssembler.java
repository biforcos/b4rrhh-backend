package com.b4rrhh.payroll.infrastructure.web.assembler;

import com.b4rrhh.payroll.domain.model.CalculationRun;
import com.b4rrhh.payroll.infrastructure.web.dto.PayrollCalculationRunResponse;
import org.springframework.stereotype.Component;

@Component
public class PayrollCalculationRunResponseAssembler {

    public PayrollCalculationRunResponse toResponse(CalculationRun run) {
        return new PayrollCalculationRunResponse(
                run.id(),
                run.status(),
                run.ruleSystemCode(),
                run.payrollPeriodCode(),
                run.payrollTypeCode(),
                run.calculationEngineCode(),
                run.calculationEngineVersion(),
                run.totalCandidates(),
                run.totalEligible(),
                run.totalClaimed(),
                run.totalSkippedNotEligible(),
                run.totalSkippedAlreadyClaimed(),
                run.totalCalculated(),
                run.totalNotValid(),
                run.totalErrors(),
                run.requestedAt(),
                run.requestedBy(),
                run.startedAt(),
                run.finishedAt()
        );
    }
}