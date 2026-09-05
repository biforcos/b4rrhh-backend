package com.b4rrhh.payroll_engine.concept.infrastructure.web;

import com.b4rrhh.payroll_engine.concept.application.usecase.CreatePayrollConceptCommand;
import com.b4rrhh.payroll_engine.concept.domain.model.CalculationType;
import com.b4rrhh.payroll_engine.concept.domain.model.ExecutionScope;
import com.b4rrhh.payroll_engine.concept.domain.model.FunctionalNature;
import com.b4rrhh.payroll_engine.concept.domain.model.PayrollConcept;
import org.springframework.stereotype.Component;

@Component
public class PayrollConceptManagementAssembler {

    public PayrollConceptDesignerResponse toResponse(PayrollConcept concept) {
        return new PayrollConceptDesignerResponse(
                concept.getRuleSystemCode(),
                concept.getConceptCode(),
                concept.getConceptMnemonic(),
                concept.getCalculationType().name(),
                concept.getFunctionalNature().name(),
                concept.getExecutionScope().name(),
                concept.getPayslipOrderCode(),
                concept.getSummary()
        );
    }

    public CreatePayrollConceptCommand toCommand(String ruleSystemCode, CreatePayrollConceptRequest request) {
        return new CreatePayrollConceptCommand(
                ruleSystemCode,
                request.conceptCode(),
                request.conceptMnemonic(),
                parseEnum(CalculationType.class, request.calculationType(), "calculationType"),
                parseEnum(FunctionalNature.class, request.functionalNature(), "functionalNature"),
                parseEnum(ExecutionScope.class, request.executionScope(), "executionScope"),
                request.payslipOrderCode(),
                request.summary()
        );
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumType, String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        try {
            return Enum.valueOf(enumType, value);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "Invalid " + fieldName + ": '" + value + "'"
            );
        }
    }
}
