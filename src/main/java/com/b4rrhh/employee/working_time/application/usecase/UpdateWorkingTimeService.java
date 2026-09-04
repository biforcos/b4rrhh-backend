package com.b4rrhh.employee.working_time.application.usecase;

import com.b4rrhh.employee.working_time.application.port.AgreementAnnualHoursLookupPort;
import com.b4rrhh.employee.working_time.application.port.EmployeeAgreementContext;
import com.b4rrhh.employee.working_time.application.port.EmployeeAgreementContextLookupPort;
import com.b4rrhh.employee.working_time.application.port.EmployeeWorkingTimeContext;
import com.b4rrhh.employee.working_time.application.port.EmployeeWorkingTimeLookupPort;
import com.b4rrhh.employee.temporal.support.DateRange;
import com.b4rrhh.employee.working_time.application.model.WorkingTimePlan;
import com.b4rrhh.employee.working_time.application.service.WorkingTimeTimelineService;
import com.b4rrhh.employee.working_time.domain.exception.WorkingTimeEmployeeNotFoundException;
import com.b4rrhh.employee.working_time.domain.exception.WorkingTimeNotFoundException;
import com.b4rrhh.employee.working_time.domain.model.WorkingTime;
import com.b4rrhh.employee.working_time.domain.model.WorkingTimeDerivedHours;
import com.b4rrhh.employee.working_time.domain.port.WorkingTimeRepository;
import com.b4rrhh.employee.working_time.domain.service.WorkingTimeDerivationPolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Corrects an occurrence: its dates, its percentage, or both. Nothing else
 * moves (ADR-057, decision 3): if the corrected dates leave a gap or an
 * overlap, the plan rejects them and names what the user would have to
 * stretch instead.
 */
@Service
public class UpdateWorkingTimeService implements UpdateWorkingTimeUseCase {

    private final WorkingTimeRepository workingTimeRepository;
    private final EmployeeWorkingTimeLookupPort employeeWorkingTimeLookupPort;
    private final EmployeeAgreementContextLookupPort employeeAgreementContextLookupPort;
    private final AgreementAnnualHoursLookupPort agreementAnnualHoursLookupPort;
    private final WorkingTimeTimelineService workingTimeTimelineService;
    private final WorkingTimeDerivationPolicy workingTimeDerivationPolicy;

    public UpdateWorkingTimeService(
            WorkingTimeRepository workingTimeRepository,
            EmployeeWorkingTimeLookupPort employeeWorkingTimeLookupPort,
            EmployeeAgreementContextLookupPort employeeAgreementContextLookupPort,
            AgreementAnnualHoursLookupPort agreementAnnualHoursLookupPort,
            WorkingTimeTimelineService workingTimeTimelineService,
            WorkingTimeDerivationPolicy workingTimeDerivationPolicy
    ) {
        this.workingTimeRepository = workingTimeRepository;
        this.employeeWorkingTimeLookupPort = employeeWorkingTimeLookupPort;
        this.employeeAgreementContextLookupPort = employeeAgreementContextLookupPort;
        this.agreementAnnualHoursLookupPort = agreementAnnualHoursLookupPort;
        this.workingTimeTimelineService = workingTimeTimelineService;
        this.workingTimeDerivationPolicy = workingTimeDerivationPolicy;
    }

    @Override
    @Transactional
    public WorkingTime update(UpdateWorkingTimeCommand command) {
        String normalizedRuleSystemCode = normalizeRuleSystemCode(command.ruleSystemCode());
        String normalizedEmployeeTypeCode = normalizeEmployeeTypeCode(command.employeeTypeCode());
        String normalizedEmployeeNumber = normalizeEmployeeNumber(command.employeeNumber());
        Integer normalizedWorkingTimeNumber = normalizeWorkingTimeNumber(command.workingTimeNumber());
        LocalDate normalizedStartDate = normalizeStartDate(command.startDate());
        BigDecimal normalizedPercentage = normalizeWorkingTimePercentage(command.workingTimePercentage());

        EmployeeWorkingTimeContext employee = employeeWorkingTimeLookupPort
                .findByBusinessKeyForUpdate(
                        normalizedRuleSystemCode,
                        normalizedEmployeeTypeCode,
                        normalizedEmployeeNumber
                )
                .orElseThrow(() -> new WorkingTimeEmployeeNotFoundException(
                        normalizedRuleSystemCode,
                        normalizedEmployeeTypeCode,
                        normalizedEmployeeNumber
                ));

        WorkingTime existing = workingTimeRepository
                .findByEmployeeIdAndWorkingTimeNumber(employee.employeeId(), normalizedWorkingTimeNumber)
                .orElseThrow(() -> new WorkingTimeNotFoundException(
                        normalizedRuleSystemCode,
                        normalizedEmployeeTypeCode,
                        normalizedEmployeeNumber,
                        normalizedWorkingTimeNumber
                ));

        // Resolve agreement context at the new startDate to compute derived hours
        EmployeeAgreementContext agreementContext = employeeAgreementContextLookupPort
                .resolveContext(employee.employeeId(), normalizedStartDate);

        BigDecimal annualHours = agreementAnnualHoursLookupPort.resolveAnnualHours(
                agreementContext.ruleSystemCode(),
                agreementContext.agreementCode()
        );

        WorkingTimeDerivedHours derivedHours = workingTimeDerivationPolicy.derive(normalizedPercentage, annualHours);

        WorkingTime updated = WorkingTime.rehydrate(
                existing.getId(),
                existing.getEmployeeId(),
                existing.getWorkingTimeNumber(),
                normalizedStartDate,
                command.endDate(),
                normalizedPercentage,
                derivedHours,
                existing.getCreatedAt(),
                null
        );

        WorkingTimePlan plan = workingTimeTimelineService.planCorrect(
                employee.employeeId(),
                existing,
                new DateRange(updated.getStartDate(), updated.getEndDate())
        );
        workingTimeTimelineService.requireAccepted(
                plan,
                normalizedRuleSystemCode,
                normalizedEmployeeTypeCode,
                normalizedEmployeeNumber
        );

        return workingTimeRepository.save(updated);
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

    private Integer normalizeWorkingTimeNumber(Integer workingTimeNumber) {
        if (workingTimeNumber == null || workingTimeNumber <= 0) {
            throw new IllegalArgumentException("workingTimeNumber must be a positive integer");
        }
        return workingTimeNumber;
    }

    private LocalDate normalizeStartDate(LocalDate startDate) {
        if (startDate == null) {
            throw new IllegalArgumentException("startDate is required");
        }
        return startDate;
    }

    private BigDecimal normalizeWorkingTimePercentage(BigDecimal percentage) {
        if (percentage == null) {
            throw new IllegalArgumentException("workingTimePercentage is required");
        }
        return percentage;
    }
}
