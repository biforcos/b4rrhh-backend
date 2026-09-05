package com.b4rrhh.payroll_engine.concept.application.usecase;

import com.b4rrhh.payroll_engine.concept.domain.model.CalculationType;
import com.b4rrhh.payroll_engine.concept.domain.model.ExecutionScope;
import com.b4rrhh.payroll_engine.concept.domain.model.FunctionalNature;

public record CreatePayrollConceptCommand(
        String ruleSystemCode,
        String conceptCode,
        String conceptMnemonic,
        CalculationType calculationType,
        FunctionalNature functionalNature,
        ExecutionScope executionScope,
        String payslipOrderCode,
        String summary
) {
}
