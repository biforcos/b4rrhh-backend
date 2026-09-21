package com.b4rrhh.employee.extra_payment_regime.application.usecase;

import com.b4rrhh.employee.temporal.support.DateRange;
import com.b4rrhh.employee.extra_payment_regime.application.model.ExtraPaymentRegimePlan;
import com.b4rrhh.employee.extra_payment_regime.application.port.AgreementExtraPaymentProrationLookupPort;
import com.b4rrhh.employee.extra_payment_regime.application.port.ExtraPaymentRegimeAgreementContext;
import com.b4rrhh.employee.extra_payment_regime.application.port.ExtraPaymentRegimeAgreementContextLookupPort;
import com.b4rrhh.employee.extra_payment_regime.application.port.EmployeeExtraPaymentRegimeContext;
import com.b4rrhh.employee.extra_payment_regime.application.port.EmployeeExtraPaymentRegimeLookupPort;
import com.b4rrhh.employee.extra_payment_regime.application.service.ExtraPaymentRegimeTimelineService;
import com.b4rrhh.employee.extra_payment_regime.domain.exception.ExtraPaymentRegimeEmployeeNotFoundException;
import com.b4rrhh.employee.extra_payment_regime.domain.exception.ExtraPaymentRegimeNumberConflictException;
import com.b4rrhh.employee.extra_payment_regime.domain.model.ExtraPaymentRegime;
import com.b4rrhh.employee.extra_payment_regime.domain.port.ExtraPaymentRegimeRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Da de alta un tramo de regimen de pagas extras.
 *
 * <p>Si el mandato no dice regimen, se copia el del convenio que le aplica al empleado en la fecha
 * de inicio ({@code backend#117}). Es lo que hace el alta, y es una COPIA: si el convenio cambia
 * el ano que viene, este empleado no cambia solo.
 */
@Service
public class CreateExtraPaymentRegimeService implements CreateExtraPaymentRegimeUseCase {

    private final ExtraPaymentRegimeRepository extraPaymentRegimeRepository;
    private final EmployeeExtraPaymentRegimeLookupPort employeeExtraPaymentRegimeLookupPort;
    private final ExtraPaymentRegimeAgreementContextLookupPort employeeAgreementContextLookupPort;
    private final AgreementExtraPaymentProrationLookupPort agreementExtraPaymentProrationLookupPort;
    private final ExtraPaymentRegimeTimelineService extraPaymentRegimeTimelineService;

    public CreateExtraPaymentRegimeService(
            ExtraPaymentRegimeRepository extraPaymentRegimeRepository,
            EmployeeExtraPaymentRegimeLookupPort employeeExtraPaymentRegimeLookupPort,
            ExtraPaymentRegimeAgreementContextLookupPort employeeAgreementContextLookupPort,
            AgreementExtraPaymentProrationLookupPort agreementExtraPaymentProrationLookupPort,
            ExtraPaymentRegimeTimelineService extraPaymentRegimeTimelineService
    ) {
        this.extraPaymentRegimeRepository = extraPaymentRegimeRepository;
        this.employeeExtraPaymentRegimeLookupPort = employeeExtraPaymentRegimeLookupPort;
        this.employeeAgreementContextLookupPort = employeeAgreementContextLookupPort;
        this.agreementExtraPaymentProrationLookupPort = agreementExtraPaymentProrationLookupPort;
        this.extraPaymentRegimeTimelineService = extraPaymentRegimeTimelineService;
    }

    @Override
    @Transactional
    public ExtraPaymentRegime create(CreateExtraPaymentRegimeCommand command) {
        String normalizedRuleSystemCode = normalizeRuleSystemCode(command.ruleSystemCode());
        String normalizedEmployeeTypeCode = normalizeEmployeeTypeCode(command.employeeTypeCode());
        String normalizedEmployeeNumber = normalizeEmployeeNumber(command.employeeNumber());

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

        int nextExtraPaymentRegimeNumber = extraPaymentRegimeRepository
                .findMaxExtraPaymentRegimeNumberByEmployeeId(employee.employeeId())
                .map(value -> value + 1)
                .orElse(1);

        boolean prorated = command.prorated() != null
                ? command.prorated()
                : proratedByTheAgreement(employee, command.startDate());

        ExtraPaymentRegime newRegime = ExtraPaymentRegime.create(
                employee.employeeId(),
                nextExtraPaymentRegimeNumber,
                command.startDate(),
                command.endDate(),
                prorated
        );

        // Anadir una ocurrencia se planifica contra los invariantes de la serie (ADR-057): dentro
        // de la presencia, sin solape y sin hueco. La unica consecuencia automatica es cerrar la
        // ocurrencia en vigor el dia anterior a la nueva.
        ExtraPaymentRegimePlan plan = extraPaymentRegimeTimelineService.planAdd(
                employee.employeeId(),
                new DateRange(newRegime.getStartDate(), newRegime.getEndDate())
        );
        extraPaymentRegimeTimelineService.requireAccepted(
                plan,
                normalizedRuleSystemCode,
                normalizedEmployeeTypeCode,
                normalizedEmployeeNumber
        );

        if (plan.adjustsAnOccurrence()) {
            ExtraPaymentRegime covering = extraPaymentRegimeRepository
                    .findByEmployeeIdAndExtraPaymentRegimeNumber(
                            employee.employeeId(),
                            plan.adjustedOccurrence().extraPaymentRegimeNumber()
                    )
                    .orElseThrow(() -> new IllegalStateException(
                            "Planned occurrence vanished: extraPaymentRegimeNumber="
                                    + plan.adjustedOccurrence().extraPaymentRegimeNumber()
                    ));
            extraPaymentRegimeRepository.save(
                    covering.adjustEndDate(plan.adjustedOccurrence().after().endDate()));
        }

        try {
            return extraPaymentRegimeRepository.save(newRegime);
        } catch (DataIntegrityViolationException ex) {
            throw new ExtraPaymentRegimeNumberConflictException(
                    normalizedRuleSystemCode,
                    normalizedEmployeeTypeCode,
                    normalizedEmployeeNumber,
                    nextExtraPaymentRegimeNumber,
                    ex
            );
        }
    }

    /** El testigo del convenio que le aplica al empleado en esa fecha. Se copia, no se enlaza. */
    private boolean proratedByTheAgreement(EmployeeExtraPaymentRegimeContext employee, java.time.LocalDate startDate) {
        ExtraPaymentRegimeAgreementContext agreement = employeeAgreementContextLookupPort
                .resolveContext(employee.employeeId(), startDate);

        return agreementExtraPaymentProrationLookupPort.resolveProratedByDefault(
                agreement.ruleSystemCode(),
                agreement.agreementCode()
        );
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
