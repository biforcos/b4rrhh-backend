package com.b4rrhh.employee.extra_payment_regime.application.service;

import com.b4rrhh.employee.extra_payment_regime.application.port.ExtraPaymentRegimePresenceConsistencyPort;
import com.b4rrhh.employee.extra_payment_regime.domain.exception.ExtraPaymentRegimeOutsidePresencePeriodException;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class DefaultExtraPaymentRegimePresenceConsistencyValidator implements ExtraPaymentRegimePresenceConsistencyValidator {

    private final ExtraPaymentRegimePresenceConsistencyPort extraPaymentRegimePresenceConsistencyPort;

    public DefaultExtraPaymentRegimePresenceConsistencyValidator(
            ExtraPaymentRegimePresenceConsistencyPort extraPaymentRegimePresenceConsistencyPort
    ) {
        this.extraPaymentRegimePresenceConsistencyPort = extraPaymentRegimePresenceConsistencyPort;
    }

    @Override
    public void validatePeriodWithinPresence(
            Long employeeId,
            LocalDate startDate,
            LocalDate endDate,
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber
    ) {
        if (!extraPaymentRegimePresenceConsistencyPort.existsPresenceContainingPeriod(employeeId, startDate, endDate)) {
            throw new ExtraPaymentRegimeOutsidePresencePeriodException(
                    ruleSystemCode,
                    employeeTypeCode,
                    employeeNumber,
                    startDate,
                    endDate
            );
        }
    }
}