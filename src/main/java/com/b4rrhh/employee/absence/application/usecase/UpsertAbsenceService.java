package com.b4rrhh.employee.absence.application.usecase;

import com.b4rrhh.employee.absence.domain.exception.AbsenceCatalogValueInvalidException;
import com.b4rrhh.employee.absence.domain.exception.AbsenceEmployeeNotFoundException;
import com.b4rrhh.employee.absence.domain.exception.AbsenceOutsidePresencePeriodException;
import com.b4rrhh.employee.absence.domain.exception.AbsenceOverlapException;
import com.b4rrhh.employee.absence.domain.exception.InvalidAbsenceDateRangeException;
import com.b4rrhh.employee.absence.domain.model.Absence;
import com.b4rrhh.employee.absence.domain.port.AbsenceRepository;
import com.b4rrhh.employee.employee.application.usecase.GetEmployeeByBusinessKeyUseCase;
import com.b4rrhh.employee.employee.domain.model.Employee;
import com.b4rrhh.employee.presence.application.usecase.ListEmployeePresencesUseCase;
import com.b4rrhh.employee.presence.domain.model.Presence;
import com.b4rrhh.rulesystem.domain.port.RuleEntityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class UpsertAbsenceService implements UpsertAbsenceUseCase {

    private final RuleEntityRepository ruleEntityRepository;
    private final GetEmployeeByBusinessKeyUseCase getEmployee;
    private final ListEmployeePresencesUseCase listPresences;
    private final AbsenceRepository absenceRepository;

    public UpsertAbsenceService(
            RuleEntityRepository ruleEntityRepository,
            GetEmployeeByBusinessKeyUseCase getEmployee,
            ListEmployeePresencesUseCase listPresences,
            AbsenceRepository absenceRepository
    ) {
        this.ruleEntityRepository = ruleEntityRepository;
        this.getEmployee = getEmployee;
        this.listPresences = listPresences;
        this.absenceRepository = absenceRepository;
    }

    @Override
    @Transactional
    public Absence upsert(UpsertAbsenceCommand command) {
        String ruleSystemCode   = command.ruleSystemCode();
        String employeeTypeCode = command.employeeTypeCode();
        String employeeNumber   = command.employeeNumber();
        String absenceTypeCode  = command.absenceTypeCode();
        LocalDate startDate     = command.startDate();
        int startTime           = command.startTime();
        LocalDate endDate       = command.endDate();
        Integer endTime         = command.endTime();

        // 1. Validate absence type catalog
        ruleEntityRepository.findByBusinessKey(ruleSystemCode, AbsenceRuleEntityTypeCodes.EMPLOYEE_ABSENCE_TYPE, absenceTypeCode)
                .orElseThrow(() -> new AbsenceCatalogValueInvalidException("absenceTypeCode", absenceTypeCode));

        // 2. Validate employee exists and is ACTIVE
        Employee employee = getEmployee.getByBusinessKey(ruleSystemCode, employeeTypeCode, employeeNumber)
                .filter(Employee::isActive)
                .orElseThrow(() -> new AbsenceEmployeeNotFoundException(
                        "Employee not found or not active: " + ruleSystemCode + "/" + employeeTypeCode + "/" + employeeNumber));

        Long employeeId = employee.getId();

        // 3. Validate startDate (and endDate if set) are covered by a presence period
        List<Presence> presences = listPresences.listByEmployeeBusinessKey(ruleSystemCode, employeeTypeCode, employeeNumber);

        if (!isCoveredByAnyPresence(presences, startDate)) {
            throw new AbsenceOutsidePresencePeriodException(
                    "startDate " + startDate + " is not covered by any presence period");
        }
        if (endDate != null && !isCoveredByAnyPresence(presences, endDate)) {
            throw new AbsenceOutsidePresencePeriodException(
                    "endDate " + endDate + " is not covered by any presence period");
        }

        // 4. Validate date range
        if (endDate != null) {
            if (endDate.isBefore(startDate)) {
                throw new InvalidAbsenceDateRangeException(
                        "endDate must not be before startDate");
            }
            if (endDate.isEqual(startDate) && endTime != null && endTime <= startTime) {
                throw new InvalidAbsenceDateRangeException(
                        "endTime must be after startTime when absence starts and ends on the same day");
            }
        }

        // 5. Look up existing absence by business key
        Optional<Absence> existing = absenceRepository.findByKey(employeeId, absenceTypeCode, startDate, startTime);

        if (existing.isPresent()) {
            Absence current = existing.get();
            if (absenceRepository.existsOverlappingAbsenceExcluding(employeeId, startDate, endDate, current.getId())) {
                throw new AbsenceOverlapException(
                        "Absence overlaps with an existing absence for employee " + employeeNumber);
            }
            Absence updated = current.update(endDate, endTime);
            return absenceRepository.save(updated);
        } else {
            if (absenceRepository.existsOverlappingAbsence(employeeId, startDate, endDate)) {
                throw new AbsenceOverlapException(
                        "Absence overlaps with an existing absence for employee " + employeeNumber);
            }
            Absence newAbsence = Absence.create(employeeId, absenceTypeCode, startDate, startTime, endDate, endTime);
            return absenceRepository.save(newAbsence);
        }
    }

    private boolean isCoveredByAnyPresence(List<Presence> presences, LocalDate date) {
        for (Presence p : presences) {
            if (!p.getStartDate().isAfter(date) &&
                (p.getEndDate() == null || !p.getEndDate().isBefore(date))) {
                return true;
            }
        }
        return false;
    }
}
