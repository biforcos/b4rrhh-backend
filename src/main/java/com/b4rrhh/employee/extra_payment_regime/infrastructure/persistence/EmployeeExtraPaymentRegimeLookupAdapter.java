package com.b4rrhh.employee.extra_payment_regime.infrastructure.persistence;

import com.b4rrhh.employee.employee.infrastructure.persistence.EmployeeEntity;
import com.b4rrhh.employee.shared.infrastructure.persistence.EmployeeBusinessKeyLookupSupport;
import com.b4rrhh.employee.extra_payment_regime.application.port.EmployeeExtraPaymentRegimeContext;
import com.b4rrhh.employee.extra_payment_regime.application.port.EmployeeExtraPaymentRegimeLookupPort;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class EmployeeExtraPaymentRegimeLookupAdapter implements EmployeeExtraPaymentRegimeLookupPort {

    private final EmployeeBusinessKeyLookupSupport employeeBusinessKeyLookupSupport;

    public EmployeeExtraPaymentRegimeLookupAdapter(EmployeeBusinessKeyLookupSupport employeeBusinessKeyLookupSupport) {
        this.employeeBusinessKeyLookupSupport = employeeBusinessKeyLookupSupport;
    }

    @Override
    public Optional<EmployeeExtraPaymentRegimeContext> findByBusinessKey(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber
    ) {
        return employeeBusinessKeyLookupSupport
                .findByBusinessKey(ruleSystemCode, employeeTypeCode, employeeNumber)
                .map(this::toContext);
    }

    @Override
    public Optional<EmployeeExtraPaymentRegimeContext> findByBusinessKeyForUpdate(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber
    ) {
        return employeeBusinessKeyLookupSupport
                .findByBusinessKeyForUpdate(ruleSystemCode, employeeTypeCode, employeeNumber)
                .map(this::toContext);
    }

    private EmployeeExtraPaymentRegimeContext toContext(EmployeeEntity employeeEntity) {
        return new EmployeeExtraPaymentRegimeContext(
                employeeEntity.getId(),
                employeeEntity.getRuleSystemCode(),
                employeeEntity.getEmployeeTypeCode(),
                employeeEntity.getEmployeeNumber()
        );
    }
}