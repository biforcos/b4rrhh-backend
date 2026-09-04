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
import com.b4rrhh.employee.working_time.domain.exception.WorkingTimeNumberConflictException;
import com.b4rrhh.employee.working_time.domain.model.WorkingTimeDerivedHours;
import com.b4rrhh.employee.working_time.domain.model.WorkingTime;
import com.b4rrhh.employee.working_time.domain.port.WorkingTimeRepository;
import com.b4rrhh.employee.working_time.domain.service.WorkingTimeDerivationPolicy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class CreateWorkingTimeService implements CreateWorkingTimeUseCase {

    private final WorkingTimeRepository workingTimeRepository;
    private final EmployeeWorkingTimeLookupPort employeeWorkingTimeLookupPort;
    private final EmployeeAgreementContextLookupPort employeeAgreementContextLookupPort;
    private final AgreementAnnualHoursLookupPort agreementAnnualHoursLookupPort;
    private final WorkingTimeTimelineService workingTimeTimelineService;
    private final WorkingTimeDerivationPolicy workingTimeDerivationPolicy;

    public CreateWorkingTimeService(
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
    public WorkingTime create(CreateWorkingTimeCommand command) {
        String normalizedRuleSystemCode = normalizeRuleSystemCode(command.ruleSystemCode());
        String normalizedEmployeeTypeCode = normalizeEmployeeTypeCode(command.employeeTypeCode());
        String normalizedEmployeeNumber = normalizeEmployeeNumber(command.employeeNumber());

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

        int nextWorkingTimeNumber = workingTimeRepository.findMaxWorkingTimeNumberByEmployeeId(employee.employeeId())
                .map(value -> value + 1)
                .orElse(1);

        // Step 1: Resolve which agreement applies to this employee at the working time start date
        EmployeeAgreementContext agreementContext = employeeAgreementContextLookupPort
                .resolveContext(employee.employeeId(), command.startDate());

        // Step 2: Resolve annual hours from agreement_profile using the agreement business key
        BigDecimal annualHours = agreementAnnualHoursLookupPort.resolveAnnualHours(
                agreementContext.ruleSystemCode(),
                agreementContext.agreementCode()
        );

        WorkingTimeDerivedHours derivedHours = workingTimeDerivationPolicy.derive(
                command.workingTimePercentage(),
                annualHours
        );

        WorkingTime newWorkingTime = WorkingTime.create(
                employee.employeeId(),
                nextWorkingTimeNumber,
                command.startDate(),
                command.endDate(),
                command.workingTimePercentage(),
                derivedHours,
                annualHours,
                workingTimeDerivationPolicy
        );

        // Step 3: Adding an occurrence is planned against the invariants of the series (ADR-057):
        // inside the presence, no overlap, no gap. The one automatic consequence is closing the
        // occurrence in force on the new start date the day before it.
        WorkingTimePlan plan = workingTimeTimelineService.planAdd(
                employee.employeeId(),
                new DateRange(newWorkingTime.getStartDate(), newWorkingTime.getEndDate())
        );
        workingTimeTimelineService.requireAccepted(
                plan,
                normalizedRuleSystemCode,
                normalizedEmployeeTypeCode,
                normalizedEmployeeNumber
        );

        if (plan.adjustsAnOccurrence()) {
            WorkingTime covering = workingTimeRepository
                    .findByEmployeeIdAndWorkingTimeNumber(
                            employee.employeeId(),
                            plan.adjustedOccurrence().workingTimeNumber()
                    )
                    .orElseThrow(() -> new IllegalStateException(
                            "Planned occurrence vanished: workingTimeNumber="
                                    + plan.adjustedOccurrence().workingTimeNumber()
                    ));
            workingTimeRepository.save(covering.adjustEndDate(plan.adjustedOccurrence().after().endDate()));
        }

        try {
            return workingTimeRepository.save(newWorkingTime);
        } catch (DataIntegrityViolationException ex) {
            throw new WorkingTimeNumberConflictException(
                    normalizedRuleSystemCode,
                    normalizedEmployeeTypeCode,
                    normalizedEmployeeNumber,
                    nextWorkingTimeNumber,
                    ex
            );
        }
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