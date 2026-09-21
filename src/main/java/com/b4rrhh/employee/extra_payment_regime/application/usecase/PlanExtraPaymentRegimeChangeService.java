package com.b4rrhh.employee.extra_payment_regime.application.usecase;

import com.b4rrhh.employee.temporal.support.DateRange;
import com.b4rrhh.employee.extra_payment_regime.application.model.ExtraPaymentRegimePlan;
import com.b4rrhh.employee.extra_payment_regime.application.port.EmployeeExtraPaymentRegimeContext;
import com.b4rrhh.employee.extra_payment_regime.application.port.EmployeeExtraPaymentRegimeLookupPort;
import com.b4rrhh.employee.extra_payment_regime.application.service.ExtraPaymentRegimeTimelineService;
import com.b4rrhh.employee.extra_payment_regime.domain.exception.ExtraPaymentRegimeEmployeeNotFoundException;
import com.b4rrhh.employee.extra_payment_regime.domain.exception.ExtraPaymentRegimeNotFoundException;
import com.b4rrhh.employee.extra_payment_regime.domain.model.ExtraPaymentRegime;
import com.b4rrhh.employee.extra_payment_regime.domain.port.ExtraPaymentRegimeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Answers what an add, a removal or a correction would do to the series
 * without applying it (ADR-057, decision 6). Rejected plans come back as
 * plans, not as errors: the screen shows the gap or the overlap and the
 * user decides.
 */
@Service
public class PlanExtraPaymentRegimeChangeService implements PlanExtraPaymentRegimeChangeUseCase {

    private final ExtraPaymentRegimeRepository extraPaymentRegimeRepository;
    private final EmployeeExtraPaymentRegimeLookupPort employeeExtraPaymentRegimeLookupPort;
    private final ExtraPaymentRegimeTimelineService extraPaymentRegimeTimelineService;

    public PlanExtraPaymentRegimeChangeService(
            ExtraPaymentRegimeRepository extraPaymentRegimeRepository,
            EmployeeExtraPaymentRegimeLookupPort employeeExtraPaymentRegimeLookupPort,
            ExtraPaymentRegimeTimelineService extraPaymentRegimeTimelineService
    ) {
        this.extraPaymentRegimeRepository = extraPaymentRegimeRepository;
        this.employeeExtraPaymentRegimeLookupPort = employeeExtraPaymentRegimeLookupPort;
        this.extraPaymentRegimeTimelineService = extraPaymentRegimeTimelineService;
    }

    @Override
    @Transactional(readOnly = true)
    public ExtraPaymentRegimePlan plan(PlanExtraPaymentRegimeChangeCommand command) {
        String normalizedRuleSystemCode = normalizeRuleSystemCode(command.ruleSystemCode());
        String normalizedEmployeeTypeCode = normalizeEmployeeTypeCode(command.employeeTypeCode());
        String normalizedEmployeeNumber = normalizeEmployeeNumber(command.employeeNumber());
        if (command.operation() == null) {
            throw new IllegalArgumentException("operation is required");
        }

        EmployeeExtraPaymentRegimeContext employee = employeeExtraPaymentRegimeLookupPort
                .findByBusinessKey(
                        normalizedRuleSystemCode,
                        normalizedEmployeeTypeCode,
                        normalizedEmployeeNumber
                )
                .orElseThrow(() -> new ExtraPaymentRegimeEmployeeNotFoundException(
                        normalizedRuleSystemCode,
                        normalizedEmployeeTypeCode,
                        normalizedEmployeeNumber
                ));

        return switch (command.operation()) {
            case ADD -> extraPaymentRegimeTimelineService.planAdd(
                    employee.employeeId(),
                    requireDates(command)
            );
            case REMOVE -> extraPaymentRegimeTimelineService.planRemove(
                    employee.employeeId(),
                    requireOccurrence(command, employee)
            );
            case CORRECT -> extraPaymentRegimeTimelineService.planCorrect(
                    employee.employeeId(),
                    requireOccurrence(command, employee),
                    requireDates(command)
            );
        };
    }

    private ExtraPaymentRegime requireOccurrence(PlanExtraPaymentRegimeChangeCommand command, EmployeeExtraPaymentRegimeContext employee) {
        if (command.extraPaymentRegimeNumber() == null || command.extraPaymentRegimeNumber() <= 0) {
            throw new IllegalArgumentException("extraPaymentRegimeNumber must be a positive integer");
        }

        return extraPaymentRegimeRepository
                .findByEmployeeIdAndExtraPaymentRegimeNumber(employee.employeeId(), command.extraPaymentRegimeNumber())
                .orElseThrow(() -> new ExtraPaymentRegimeNotFoundException(
                        employee.ruleSystemCode(),
                        employee.employeeTypeCode(),
                        employee.employeeNumber(),
                        command.extraPaymentRegimeNumber()
                ));
    }

    private static DateRange requireDates(PlanExtraPaymentRegimeChangeCommand command) {
        if (command.startDate() == null) {
            throw new IllegalArgumentException("startDate is required");
        }

        return new DateRange(command.startDate(), command.endDate());
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
}
