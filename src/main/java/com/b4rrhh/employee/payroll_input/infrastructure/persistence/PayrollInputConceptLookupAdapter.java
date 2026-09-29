package com.b4rrhh.employee.payroll_input.infrastructure.persistence;

import com.b4rrhh.employee.payroll_input.application.port.PayrollInputConceptLookupPort;
import com.b4rrhh.payroll_engine.concept.domain.port.PayrollConceptRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class PayrollInputConceptLookupAdapter implements PayrollInputConceptLookupPort {

    private final PayrollConceptRepository payrollConceptRepository;

    public PayrollInputConceptLookupAdapter(PayrollConceptRepository payrollConceptRepository) {
        this.payrollConceptRepository = payrollConceptRepository;
    }

    @Override
    public Optional<String> calculationTypeOf(String ruleSystemCode, String conceptCode) {
        return payrollConceptRepository.findByBusinessKey(ruleSystemCode, conceptCode)
                .map(concept -> String.valueOf(concept.getCalculationType()));
    }
}
