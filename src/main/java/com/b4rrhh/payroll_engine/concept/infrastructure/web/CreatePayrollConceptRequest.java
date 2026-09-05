package com.b4rrhh.payroll_engine.concept.infrastructure.web;

import jakarta.validation.constraints.NotBlank;

public record CreatePayrollConceptRequest(
        @NotBlank String conceptCode,
        @NotBlank String conceptMnemonic,
        @NotBlank String calculationType,
        @NotBlank String functionalNature,
        @NotBlank String executionScope,
        String payslipOrderCode,
        String summary
) {
}
