package com.b4rrhh.employee.absence.application.usecase;

import com.b4rrhh.employee.absence.domain.exception.AbsenceNotFoundException;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetAbsenceByBusinessKeyServiceTest {

    @Mock private GetEmployeeByBusinessKeyUseCase getEmployee;
    @Mock private AbsenceRepository absenceRepository;
    @InjectMocks private GetAbsenceByBusinessKeyService service;

    private static final LocalDate MAY_14 = LocalDate.of(2026, 5, 14);

    private Employee employee() {
        return new Employee(1L, "ESP", "INTERNAL", "EMP001",
            "Ana", "Lopez", null, null, "ACTIVE",
            LocalDateTime.now(), LocalDateTime.now(), null);
    }

    @Test
    void returnsAbsenceWhenFound() {
        when(getEmployee.getByBusinessKey("ESP", "INTERNAL", "EMP001"))
            .thenReturn(Optional.of(employee()));
        Absence absence = Absence.rehydrate(1L, 1L, "VACATION", MAY_14, 0, null, null,
            LocalDateTime.now(), LocalDateTime.now());
        when(absenceRepository.findByKey(1L, "VACATION", MAY_14, 0))
            .thenReturn(Optional.of(absence));

        Absence result = service.getByBusinessKey(
            new GetAbsenceByBusinessKeyCommand("ESP", "INTERNAL", "EMP001", "VACATION", MAY_14, 0));

        assertNotNull(result);
        assertEquals("VACATION", result.getAbsenceTypeCode());
    }

    @Test
    void throwsAbsenceNotFoundWhenEmployeeNotFound() {
        when(getEmployee.getByBusinessKey(anyString(), anyString(), anyString()))
            .thenReturn(Optional.empty());

        assertThrows(AbsenceNotFoundException.class, () -> service.getByBusinessKey(
            new GetAbsenceByBusinessKeyCommand("ESP", "INTERNAL", "EMP001", "VACATION", MAY_14, 0)));
    }

    @Test
    void throwsAbsenceNotFoundWhenAbsenceMissing() {
        when(getEmployee.getByBusinessKey(anyString(), anyString(), anyString()))
            .thenReturn(Optional.of(employee()));
        when(absenceRepository.findByKey(anyLong(), anyString(), any(), anyInt()))
            .thenReturn(Optional.empty());

        assertThrows(AbsenceNotFoundException.class, () -> service.getByBusinessKey(
            new GetAbsenceByBusinessKeyCommand("ESP", "INTERNAL", "EMP001", "VACATION", MAY_14, 0)));
    }
}
