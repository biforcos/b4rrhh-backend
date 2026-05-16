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
import com.b4rrhh.rulesystem.domain.model.RuleEntity;
import com.b4rrhh.rulesystem.domain.port.RuleEntityRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpsertAbsenceServiceTest {

    @Mock private RuleEntityRepository ruleEntityRepository;
    @Mock private GetEmployeeByBusinessKeyUseCase getEmployee;
    @Mock private ListEmployeePresencesUseCase listPresences;
    @Mock private AbsenceRepository absenceRepository;

    @InjectMocks private UpsertAbsenceService service;

    private static final String RULE_SYSTEM = "ESP";
    private static final String EMP_TYPE = "INTERNAL";
    private static final String EMP_NUMBER = "001";
    private static final String ABSENCE_TYPE = "SICK";

    private static final LocalDate MAY_1 = LocalDate.of(2026, 5, 1);
    private static final LocalDate MAY_14 = LocalDate.of(2026, 5, 14);
    private static final LocalDate MAY_18 = LocalDate.of(2026, 5, 18);
    private static final LocalDate MAY_31 = LocalDate.of(2026, 5, 31);
    private static final LocalDateTime NOW = LocalDateTime.now();

    // ---- Helpers ----

    private RuleEntity activeRuleEntity() {
        return new RuleEntity(1L, RULE_SYSTEM, "EMPLOYEE_ABSENCE_TYPE", ABSENCE_TYPE,
                "Sick", null, true, LocalDate.of(2000, 1, 1), null, NOW, NOW);
    }

    private Employee activeEmployee() {
        // Employee(id, ruleSystemCode, employeeTypeCode, employeeNumber,
        //          firstName, lastName1, lastName2, preferredName,
        //          status, createdAt, updatedAt, photoUrl)
        return new Employee(42L, RULE_SYSTEM, EMP_TYPE, EMP_NUMBER,
                "John", "Doe", null, null,
                "ACTIVE", NOW, NOW, null);
    }

    private Employee inactiveEmployee() {
        return new Employee(42L, RULE_SYSTEM, EMP_TYPE, EMP_NUMBER,
                "John", "Doe", null, null,
                "TERMINATED", NOW, NOW, null);
    }

    /** A presence that covers all of May 2026 (open-ended). */
    private Presence mayPresence() {
        return new Presence(1L, 42L, 1, "B4", "HIRE", null,
                MAY_1, null, NOW, NOW);
    }

    private UpsertAbsenceCommand commandFor(LocalDate start, LocalDate end) {
        return new UpsertAbsenceCommand(RULE_SYSTEM, EMP_TYPE, EMP_NUMBER, ABSENCE_TYPE,
                start, 480, end, end != null ? 960 : null);
    }

    private UpsertAbsenceCommand commandSameDay(int startTime, int endTime) {
        return new UpsertAbsenceCommand(RULE_SYSTEM, EMP_TYPE, EMP_NUMBER, ABSENCE_TYPE,
                MAY_14, startTime, MAY_14, endTime);
    }

    // ---- Tests ----

    @Test
    void throwsWhenAbsenceTypeCodeInvalid() {
        when(ruleEntityRepository.findByBusinessKey(RULE_SYSTEM, "EMPLOYEE_ABSENCE_TYPE", ABSENCE_TYPE))
                .thenReturn(Optional.empty());

        UpsertAbsenceCommand cmd = commandFor(MAY_14, MAY_18);

        assertThrows(AbsenceCatalogValueInvalidException.class, () -> service.upsert(cmd));
    }

    @Test
    void throwsWhenEmployeeNotFound() {
        when(ruleEntityRepository.findByBusinessKey(RULE_SYSTEM, "EMPLOYEE_ABSENCE_TYPE", ABSENCE_TYPE))
                .thenReturn(Optional.of(activeRuleEntity()));
        when(getEmployee.getByBusinessKey(RULE_SYSTEM, EMP_TYPE, EMP_NUMBER))
                .thenReturn(Optional.empty());

        UpsertAbsenceCommand cmd = commandFor(MAY_14, MAY_18);

        assertThrows(AbsenceEmployeeNotFoundException.class, () -> service.upsert(cmd));
    }

    @Test
    void throwsWhenEmployeeNotActive() {
        when(ruleEntityRepository.findByBusinessKey(RULE_SYSTEM, "EMPLOYEE_ABSENCE_TYPE", ABSENCE_TYPE))
                .thenReturn(Optional.of(activeRuleEntity()));
        when(getEmployee.getByBusinessKey(RULE_SYSTEM, EMP_TYPE, EMP_NUMBER))
                .thenReturn(Optional.of(inactiveEmployee()));

        UpsertAbsenceCommand cmd = commandFor(MAY_14, MAY_18);

        assertThrows(AbsenceEmployeeNotFoundException.class, () -> service.upsert(cmd));
    }

    @Test
    void throwsWhenStartDateNotCoveredByPresence() {
        when(ruleEntityRepository.findByBusinessKey(RULE_SYSTEM, "EMPLOYEE_ABSENCE_TYPE", ABSENCE_TYPE))
                .thenReturn(Optional.of(activeRuleEntity()));
        when(getEmployee.getByBusinessKey(RULE_SYSTEM, EMP_TYPE, EMP_NUMBER))
                .thenReturn(Optional.of(activeEmployee()));
        // Presence covers only May 1–13, not May 14
        Presence oldPresence = new Presence(1L, 42L, 1, "B4", "HIRE", "RESIGNATION",
                MAY_1, LocalDate.of(2026, 5, 13), NOW, NOW);
        when(listPresences.listByEmployeeBusinessKey(RULE_SYSTEM, EMP_TYPE, EMP_NUMBER))
                .thenReturn(List.of(oldPresence));

        UpsertAbsenceCommand cmd = commandFor(MAY_14, MAY_18);

        assertThrows(AbsenceOutsidePresencePeriodException.class, () -> service.upsert(cmd));
    }

    @Test
    void throwsWhenEndDateNotCoveredByPresence() {
        when(ruleEntityRepository.findByBusinessKey(RULE_SYSTEM, "EMPLOYEE_ABSENCE_TYPE", ABSENCE_TYPE))
                .thenReturn(Optional.of(activeRuleEntity()));
        when(getEmployee.getByBusinessKey(RULE_SYSTEM, EMP_TYPE, EMP_NUMBER))
                .thenReturn(Optional.of(activeEmployee()));
        // Presence covers May 1–15; endDate May 18 is outside
        Presence shortPresence = new Presence(1L, 42L, 1, "B4", "HIRE", null,
                MAY_1, LocalDate.of(2026, 5, 15), NOW, NOW);
        when(listPresences.listByEmployeeBusinessKey(RULE_SYSTEM, EMP_TYPE, EMP_NUMBER))
                .thenReturn(List.of(shortPresence));

        UpsertAbsenceCommand cmd = commandFor(MAY_14, MAY_18);

        assertThrows(AbsenceOutsidePresencePeriodException.class, () -> service.upsert(cmd));
    }

    @Test
    void throwsWhenEndDateBeforeStartDate() {
        when(ruleEntityRepository.findByBusinessKey(RULE_SYSTEM, "EMPLOYEE_ABSENCE_TYPE", ABSENCE_TYPE))
                .thenReturn(Optional.of(activeRuleEntity()));
        when(getEmployee.getByBusinessKey(RULE_SYSTEM, EMP_TYPE, EMP_NUMBER))
                .thenReturn(Optional.of(activeEmployee()));
        when(listPresences.listByEmployeeBusinessKey(RULE_SYSTEM, EMP_TYPE, EMP_NUMBER))
                .thenReturn(List.of(mayPresence()));

        // endDate before startDate
        UpsertAbsenceCommand cmd = new UpsertAbsenceCommand(RULE_SYSTEM, EMP_TYPE, EMP_NUMBER, ABSENCE_TYPE,
                MAY_18, 480, MAY_14, 960);

        assertThrows(InvalidAbsenceDateRangeException.class, () -> service.upsert(cmd));
    }

    @Test
    void throwsWhenSameDayHourModeEndTimeNotAfterStartTime() {
        when(ruleEntityRepository.findByBusinessKey(RULE_SYSTEM, "EMPLOYEE_ABSENCE_TYPE", ABSENCE_TYPE))
                .thenReturn(Optional.of(activeRuleEntity()));
        when(getEmployee.getByBusinessKey(RULE_SYSTEM, EMP_TYPE, EMP_NUMBER))
                .thenReturn(Optional.of(activeEmployee()));
        when(listPresences.listByEmployeeBusinessKey(RULE_SYSTEM, EMP_TYPE, EMP_NUMBER))
                .thenReturn(List.of(mayPresence()));

        // same day, endTime <= startTime
        UpsertAbsenceCommand cmd = commandSameDay(960, 480);

        assertThrows(InvalidAbsenceDateRangeException.class, () -> service.upsert(cmd));
    }

    @Test
    void throwsWhenOverlapExists() {
        when(ruleEntityRepository.findByBusinessKey(RULE_SYSTEM, "EMPLOYEE_ABSENCE_TYPE", ABSENCE_TYPE))
                .thenReturn(Optional.of(activeRuleEntity()));
        when(getEmployee.getByBusinessKey(RULE_SYSTEM, EMP_TYPE, EMP_NUMBER))
                .thenReturn(Optional.of(activeEmployee()));
        when(listPresences.listByEmployeeBusinessKey(RULE_SYSTEM, EMP_TYPE, EMP_NUMBER))
                .thenReturn(List.of(mayPresence()));
        // No existing key match → new absence path
        when(absenceRepository.findByKey(42L, ABSENCE_TYPE, MAY_14, 480))
                .thenReturn(Optional.empty());
        when(absenceRepository.existsOverlappingAbsence(eq(42L), eq(MAY_14), any(LocalDate.class)))
                .thenReturn(true);

        UpsertAbsenceCommand cmd = commandFor(MAY_14, MAY_18);

        assertThrows(AbsenceOverlapException.class, () -> service.upsert(cmd));
    }

    @Test
    void createsAbsenceWhenKeyNotFound() {
        when(ruleEntityRepository.findByBusinessKey(RULE_SYSTEM, "EMPLOYEE_ABSENCE_TYPE", ABSENCE_TYPE))
                .thenReturn(Optional.of(activeRuleEntity()));
        when(getEmployee.getByBusinessKey(RULE_SYSTEM, EMP_TYPE, EMP_NUMBER))
                .thenReturn(Optional.of(activeEmployee()));
        when(listPresences.listByEmployeeBusinessKey(RULE_SYSTEM, EMP_TYPE, EMP_NUMBER))
                .thenReturn(List.of(mayPresence()));
        when(absenceRepository.findByKey(42L, ABSENCE_TYPE, MAY_14, 480))
                .thenReturn(Optional.empty());
        when(absenceRepository.existsOverlappingAbsence(eq(42L), eq(MAY_14), any(LocalDate.class)))
                .thenReturn(false);

        Absence saved = Absence.rehydrate(99L, 42L, ABSENCE_TYPE, MAY_14, 480, MAY_18, 960, NOW, NOW);
        when(absenceRepository.save(any(Absence.class))).thenReturn(saved);

        UpsertAbsenceCommand cmd = commandFor(MAY_14, MAY_18);
        Absence result = service.upsert(cmd);

        assertNotNull(result);
        assertEquals(99L, result.getId());
        verify(absenceRepository).save(any(Absence.class));
    }

    @Test
    void updatesExistingAbsenceWhenKeyFound() {
        when(ruleEntityRepository.findByBusinessKey(RULE_SYSTEM, "EMPLOYEE_ABSENCE_TYPE", ABSENCE_TYPE))
                .thenReturn(Optional.of(activeRuleEntity()));
        when(getEmployee.getByBusinessKey(RULE_SYSTEM, EMP_TYPE, EMP_NUMBER))
                .thenReturn(Optional.of(activeEmployee()));
        when(listPresences.listByEmployeeBusinessKey(RULE_SYSTEM, EMP_TYPE, EMP_NUMBER))
                .thenReturn(List.of(mayPresence()));

        Absence existing = Absence.rehydrate(77L, 42L, ABSENCE_TYPE, MAY_14, 480, null, null, NOW, NOW);
        when(absenceRepository.findByKey(42L, ABSENCE_TYPE, MAY_14, 480))
                .thenReturn(Optional.of(existing));
        when(absenceRepository.existsOverlappingAbsenceExcluding(eq(42L), eq(MAY_14), any(LocalDate.class), eq(77L)))
                .thenReturn(false);

        Absence updated = existing.update(MAY_18, 960);
        when(absenceRepository.save(any(Absence.class))).thenReturn(updated);

        UpsertAbsenceCommand cmd = commandFor(MAY_14, MAY_18);
        Absence result = service.upsert(cmd);

        assertNotNull(result);
        assertEquals(MAY_18, result.getEndDate());
        verify(absenceRepository).save(any(Absence.class));
    }
}
