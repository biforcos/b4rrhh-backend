package com.b4rrhh.payroll_engine.concept.infrastructure.web;

public record PayrollConceptDesignerResponse(
        String ruleSystemCode,
        String conceptCode,
        String conceptMnemonic,
        String calculationType,
        String functionalNature,
        String executionScope,
        String payslipOrderCode,
        String summary
) {
}
