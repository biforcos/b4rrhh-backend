package com.b4rrhh.employee.absence.application.usecase;

import com.b4rrhh.employee.absence.domain.exception.AbsenceEmployeeNotFoundException;
import com.b4rrhh.employee.absence.domain.model.Absence;
import com.b4rrhh.employee.absence.domain.port.AbsenceRepository;
import com.b4rrhh.employee.employee.application.usecase.GetEmployeeByBusinessKeyUseCase;
import com.b4rrhh.employee.employee.domain.model.Employee;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListEmployeeAbsencesServiceTest {

    @Mock private GetEmployeeByBusinessKeyUseCase getEmployee;
    @Mock private AbsenceRepository absenceRepository;
    @InjectMocks private ListEmployeeAbsencesService service;

    @Test
    void returnsEmptyListWhenNoAbsences() {
        when(getEmployee.getByBusinessKey(anyString(), anyString(), anyString()))
            .thenReturn(Optional.of(new Employee(1L, "ESP", "INTERNAL", "EMP001",
                "Ana", "Lopez", null, null, "ACTIVE", LocalDateTime.now(), LocalDateTime.now(), null)));
        when(absenceRepository.findByEmployeeIdOrderByStartDateDescStartTimeDesc(1L))
            .thenReturn(List.of());

        List<Absence> result = service.listByEmployeeBusinessKey(
            new ListEmployeeAbsencesCommand("ESP", "INTERNAL", "EMP001"));

        assertTrue(result.isEmpty());
    }

    @Test
    void returnsAbsencesWhenEmployeeFound() {
        when(getEmployee.getByBusinessKey(anyString(), anyString(), anyString()))
            .thenReturn(Optional.of(new Employee(1L, "ESP", "INTERNAL", "EMP001",
                "Ana", "Lopez", null, null, "ACTIVE", LocalDateTime.now(), LocalDateTime.now(), null)));
        Absence a = Absence.rehydrate(1L, 1L, "VACATION", LocalDate.of(2026, 5, 14), 0,
            null, null, LocalDateTime.now(), LocalDateTime.now());
        when(absenceRepository.findByEmployeeIdOrderByStartDateDescStartTimeDesc(1L))
            .thenReturn(List.of(a));

        List<Absence> result = service.listByEmployeeBusinessKey(
            new ListEmployeeAbsencesCommand("ESP", "INTERNAL", "EMP001"));

        assertEquals(1, result.size());
    }

    @Test
    void throwsWhenEmployeeNotFound() {
        when(getEmployee.getByBusinessKey(anyString(), anyString(), anyString()))
            .thenReturn(Optional.empty());

        assertThrows(AbsenceEmployeeNotFoundException.class, () -> service.listByEmployeeBusinessKey(
            new ListEmployeeAbsencesCommand("ESP", "INTERNAL", "UNKNOWN")));
    }
}
