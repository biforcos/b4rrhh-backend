package com.b4rrhh.employee.extra_payment_regime.application.port;

import java.util.Optional;

public interface EmployeeExtraPaymentRegimeLookupPort {

    Optional<EmployeeExtraPaymentRegimeContext> findByBusinessKey(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber
    );

    Optional<EmployeeExtraPaymentRegimeContext> findByBusinessKeyForUpdate(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber
    );
}