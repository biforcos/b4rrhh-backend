package com.b4rrhh.employee.extra_payment_regime.application.usecase;

import com.b4rrhh.employee.extra_payment_regime.application.port.EmployeeExtraPaymentRegimeContext;
import com.b4rrhh.employee.extra_payment_regime.application.port.EmployeeExtraPaymentRegimeLookupPort;
import com.b4rrhh.employee.extra_payment_regime.application.service.ExtraPaymentRegimePresenceConsistencyValidator;
import com.b4rrhh.employee.extra_payment_regime.domain.exception.ExtraPaymentRegimeEmployeeNotFoundException;
import com.b4rrhh.employee.extra_payment_regime.domain.exception.ExtraPaymentRegimeNotFoundException;
import com.b4rrhh.employee.extra_payment_regime.domain.model.ExtraPaymentRegime;
import com.b4rrhh.employee.extra_payment_regime.domain.port.ExtraPaymentRegimeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CloseExtraPaymentRegimeService implements CloseExtraPaymentRegimeUseCase {

    private final ExtraPaymentRegimeRepository extraPaymentRegimeRepository;
    private final EmployeeExtraPaymentRegimeLookupPort employeeExtraPaymentRegimeLookupPort;
    private final ExtraPaymentRegimePresenceConsistencyValidator extraPaymentRegimePresenceConsistencyValidator;

    public CloseExtraPaymentRegimeService(
            ExtraPaymentRegimeRepository extraPaymentRegimeRepository,
            EmployeeExtraPaymentRegimeLookupPort employeeExtraPaymentRegimeLookupPort,
            ExtraPaymentRegimePresenceConsistencyValidator extraPaymentRegimePresenceConsistencyValidator
    ) {
        this.extraPaymentRegimeRepository = extraPaymentRegimeRepository;
        this.employeeExtraPaymentRegimeLookupPort = employeeExtraPaymentRegimeLookupPort;
        this.extraPaymentRegimePresenceConsistencyValidator = extraPaymentRegimePresenceConsistencyValidator;
    }

    @Override
    @Transactional
    public ExtraPaymentRegime close(CloseExtraPaymentRegimeCommand command) {
        String normalizedRuleSystemCode = normalizeRuleSystemCode(command.ruleSystemCode());
        String normalizedEmployeeTypeCode = normalizeEmployeeTypeCode(command.employeeTypeCode());
        String normalizedEmployeeNumber = normalizeEmployeeNumber(command.employeeNumber());
        Integer normalizedExtraPaymentRegimeNumber = normalizeExtraPaymentRegimeNumber(command.extraPaymentRegimeNumber());

        EmployeeExtraPaymentRegimeContext employee = employeeExtraPaymentRegimeLookupPort
                .findByBusinessKeyForUpdate(
                        normalizedRuleSystemCode,
                        normalizedEmployeeTypeCode,
                        normalizedEmployeeNumber
                )
                .orElseThrow(() -> new ExtraPaymentRegimeEmployeeNotFoundException(
                        normalizedRuleSystemCode,
                        normalizedEmployeeTypeCode,
                        normalizedEmployeeNumber
                ));

        ExtraPaymentRegime existing = extraPaymentRegimeRepository.findByEmployeeIdAndExtraPaymentRegimeNumber(
                        employee.employeeId(),
                        normalizedExtraPaymentRegimeNumber
                )
                .orElseThrow(() -> new ExtraPaymentRegimeNotFoundException(
                        normalizedRuleSystemCode,
                        normalizedEmployeeTypeCode,
                        normalizedEmployeeNumber,
                        normalizedExtraPaymentRegimeNumber
                ));

        ExtraPaymentRegime closed = existing.close(command.endDate());

        extraPaymentRegimePresenceConsistencyValidator.validatePeriodWithinPresence(
                employee.employeeId(),
                closed.getStartDate(),
                closed.getEndDate(),
                normalizedRuleSystemCode,
                normalizedEmployeeTypeCode,
                normalizedEmployeeNumber
        );

        return extraPaymentRegimeRepository.save(closed);
    }

    private String normalizeRuleSystemCode(String ruleSystemCode) {
        if (ruleSystemCode == null || ruleSystemCode.trim().isEmpty()) {
            throw new IllegalArgumentException("ruleSystemCode is required");
        }

        return ruleSystemCode.trim().toUpperCase();
    }

    private String normalizeEmployeeTypeCode(String employeeTypeCode) {
        if (employeeTypeCode == null || employeeTypeCode.trim().isEmpty()) {
            throw new IllegalArgumentException("employeeTypeCode is required");
        }

        return employeeTypeCode.trim().toUpperCase();
    }

    private String normalizeEmployeeNumber(String employeeNumber) {
        if (employeeNumber == null || employeeNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("employeeNumber is required");
        }

        return employeeNumber.trim();
    }

    private Integer normalizeExtraPaymentRegimeNumber(Integer extraPaymentRegimeNumber) {
        if (extraPaymentRegimeNumber == null || extraPaymentRegimeNumber <= 0) {
            throw new IllegalArgumentException("extraPaymentRegimeNumber must be a positive integer");
        }

        return extraPaymentRegimeNumber;
    }
}