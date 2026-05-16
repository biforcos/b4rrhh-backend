package com.b4rrhh.employee.absence.application.usecase;

import com.b4rrhh.employee.absence.domain.model.Absence;
import com.b4rrhh.employee.absence.domain.port.AbsenceRepository;
import com.b4rrhh.employee.employee.application.usecase.GetEmployeeByBusinessKeyUseCase;
import com.b4rrhh.employee.employee.domain.model.Employee;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
class CloseOpenAbsenceAtTerminationServiceTest {

    @Mock private GetEmployeeByBusinessKeyUseCase getEmployee;
    @Mock private AbsenceRepository absenceRepository;
    @InjectMocks private CloseOpenAbsenceAtTerminationService service;

    private static final LocalDate MAY_1 = LocalDate.of(2026, 5, 1);
    private static final LocalDate MAY_31 = LocalDate.of(2026, 5, 31);

    private Employee employee() {
        return new Employee(1L, "ESP", "INTERNAL", "EMP001",
            "Ana", "Lopez", null, null, "ACTIVE",
            LocalDateTime.now(), LocalDateTime.now(), null);
    }

    @Test
    void closesOpenAbsenceAtTerminationDate() {
        when(getEmployee.getByBusinessKey("ESP", "INTERNAL", "EMP001"))
            .thenReturn(Optional.of(employee()));
        Absence openAbsence = Absence.rehydrate(1L, 1L, "VACATION", MAY_1, 0,
            null, null, LocalDateTime.now(), LocalDateTime.now());
        when(absenceRepository.findByEmployeeIdOrderByStartDateDescStartTimeDesc(1L))
            .thenReturn(List.of(openAbsence));

        service.closeIfOpen("ESP", "INTERNAL", "EMP001", MAY_31);

        ArgumentCaptor<Absence> captor = ArgumentCaptor.forClass(Absence.class);
        verify(absenceRepository).save(captor.capture());
        assertEquals(MAY_31, captor.getValue().getEndDate());
        assertNull(captor.getValue().getEndTime());
    }

    @Test
    void doesNothingWhenNoOpenAbsence() {
        when(getEmployee.getByBusinessKey("ESP", "INTERNAL", "EMP001"))
            .thenReturn(Optional.of(employee()));
        Absence closed = Absence.rehydrate(1L, 1L, "VACATION", MAY_1, 0,
            LocalDate.of(2026, 5, 15), null, LocalDateTime.now(), LocalDateTime.now());
        when(absenceRepository.findByEmployeeIdOrderByStartDateDescStartTimeDesc(1L))
            .thenReturn(List.of(closed));

        service.closeIfOpen("ESP", "INTERNAL", "EMP001", MAY_31);

        verify(absenceRepository, never()).save(any());
    }

    @Test
    void doesNothingWhenEmployeeNotFound() {
        when(getEmployee.getByBusinessKey(anyString(), anyString(), anyString()))
            .thenReturn(Optional.empty());

        service.closeIfOpen("ESP", "INTERNAL", "UNKNOWN", MAY_31);

        verify(absenceRepository, never()).save(any());
    }
}
