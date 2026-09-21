package com.b4rrhh.employee.extra_payment_regime.application.service;

import java.time.LocalDate;

public interface ExtraPaymentRegimePresenceConsistencyValidator {

    void validatePeriodWithinPresence(
            Long employeeId,
            LocalDate startDate,
            LocalDate endDate,
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber
    );
}