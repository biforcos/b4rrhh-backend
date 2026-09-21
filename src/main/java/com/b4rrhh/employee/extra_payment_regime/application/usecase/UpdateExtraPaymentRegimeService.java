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

import java.time.LocalDate;

/**
 * Corrige una ocurrencia: sus fechas, su regimen, o las dos cosas. No se mueve nada mas
 * (ADR-057, decision 3): si las fechas corregidas dejan un hueco o un solape, el plan las rechaza
 * y nombra lo que el usuario tendria que estirar.
 */
@Service
public class UpdateExtraPaymentRegimeService implements UpdateExtraPaymentRegimeUseCase {

    private final ExtraPaymentRegimeRepository extraPaymentRegimeRepository;
    private final EmployeeExtraPaymentRegimeLookupPort employeeExtraPaymentRegimeLookupPort;
    private final ExtraPaymentRegimeTimelineService extraPaymentRegimeTimelineService;

    public UpdateExtraPaymentRegimeService(
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
    public ExtraPaymentRegime update(UpdateExtraPaymentRegimeCommand command) {
        String normalizedRuleSystemCode = normalizeRuleSystemCode(command.ruleSystemCode());
        String normalizedEmployeeTypeCode = normalizeEmployeeTypeCode(command.employeeTypeCode());
        String normalizedEmployeeNumber = normalizeEmployeeNumber(command.employeeNumber());
        Integer normalizedNumber = normalizeExtraPaymentRegimeNumber(command.extraPaymentRegimeNumber());
        LocalDate normalizedStartDate = normalizeStartDate(command.startDate());
        boolean normalizedProrated = normalizeProrated(command.prorated());

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
                .findByEmployeeIdAndExtraPaymentRegimeNumber(employee.employeeId(), normalizedNumber)
                .orElseThrow(() -> new ExtraPaymentRegimeNotFoundException(
                        normalizedRuleSystemCode,
                        normalizedEmployeeTypeCode,
                        normalizedEmployeeNumber,
                        normalizedNumber
                ));

        ExtraPaymentRegime updated = ExtraPaymentRegime.rehydrate(
                existing.getId(),
                existing.getEmployeeId(),
                existing.getExtraPaymentRegimeNumber(),
                normalizedStartDate,
                command.endDate(),
                normalizedProrated,
                existing.getCreatedAt(),
                null
        );

        ExtraPaymentRegimePlan plan = extraPaymentRegimeTimelineService.planCorrect(
                employee.employeeId(),
                existing,
                new DateRange(updated.getStartDate(), updated.getEndDate())
        );
        extraPaymentRegimeTimelineService.requireAccepted(
                plan,
                normalizedRuleSystemCode,
                normalizedEmployeeTypeCode,
                normalizedEmployeeNumber
        );

        return extraPaymentRegimeRepository.save(updated);
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

    private LocalDate normalizeStartDate(LocalDate startDate) {
        if (startDate == null) {
            throw new IllegalArgumentException("startDate is required");
        }
        return startDate;
    }

    /**
     * Corregir SI exige decir el regimen. Una correccion que lo omitiera tendria que elegir entre
     * dejar el que habia y volver a copiar el del convenio, y las dos son decisiones invisibles
     * sobre lo que se le paga a una persona.
     */
    private boolean normalizeProrated(Boolean prorated) {
        if (prorated == null) {
            throw new IllegalArgumentException("prorated is required");
        }
        return prorated;
    }
}
