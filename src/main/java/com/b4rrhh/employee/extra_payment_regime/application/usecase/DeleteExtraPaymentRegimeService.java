package com.b4rrhh.employee.extra_payment_regime.application.usecase;

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
 * Removes an occurrence (ADR-057, decision 3). Removing the last one
 * reopens the previous one: it is the "oops" and it is safe. Removing one
 * in the middle would leave a gap, and the invariant rejects it naming the
 * neighbours the user would have to stretch first.
 */
@Service
public class DeleteExtraPaymentRegimeService implements DeleteExtraPaymentRegimeUseCase {

    private final ExtraPaymentRegimeRepository extraPaymentRegimeRepository;
    private final EmployeeExtraPaymentRegimeLookupPort employeeExtraPaymentRegimeLookupPort;
    private final ExtraPaymentRegimeTimelineService extraPaymentRegimeTimelineService;

    public DeleteExtraPaymentRegimeService(
            ExtraPaymentRegimeRepository extraPaymentRegimeRepository,
            EmployeeExtraPaymentRegimeLookupPort employeeExtraPaymentRegimeLookupPort,
            ExtraPaymentRegimeTimelineService extraPaymentRegimeTimelineService
    ) {
        this.extraPaymentRegimeRepository = extraPaymentRegimeRepository;
        this.employeeExtraPaymentRegimeLookupPort = employeeExtraPaymentRegimeLookupPort;
        this.extraPaymentRegimeTimelineService = extraPaymentRegimeTimelineService;
    }

    @Override
    @Transactional
    public void delete(DeleteExtraPaymentRegimeCommand command) {
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

        ExtraPaymentRegime existing = extraPaymentRegimeRepository
                .findByEmployeeIdAndExtraPaymentRegimeNumber(employee.employeeId(), normalizedExtraPaymentRegimeNumber)
                .orElseThrow(() -> new ExtraPaymentRegimeNotFoundException(
                        normalizedRuleSystemCode,
                        normalizedEmployeeTypeCode,
                        normalizedEmployeeNumber,
                        normalizedExtraPaymentRegimeNumber
                ));

        ExtraPaymentRegimePlan plan = extraPaymentRegimeTimelineService.planRemove(employee.employeeId(), existing);
        extraPaymentRegimeTimelineService.requireAccepted(
                plan,
                normalizedRuleSystemCode,
                normalizedEmployeeTypeCode,
                normalizedEmployeeNumber
        );

        if (plan.adjustsAnOccurrence()) {
            ExtraPaymentRegime previous = extraPaymentRegimeRepository
                    .findByEmployeeIdAndExtraPaymentRegimeNumber(
                            employee.employeeId(),
                            plan.adjustedOccurrence().extraPaymentRegimeNumber()
                    )
                    .orElseThrow(() -> new IllegalStateException(
                            "Planned occurrence vanished: extraPaymentRegimeNumber="
                                    + plan.adjustedOccurrence().extraPaymentRegimeNumber()
                    ));
            extraPaymentRegimeRepository.save(previous.adjustEndDate(plan.adjustedOccurrence().after().endDate()));
        }

        extraPaymentRegimeRepository.delete(existing);
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
