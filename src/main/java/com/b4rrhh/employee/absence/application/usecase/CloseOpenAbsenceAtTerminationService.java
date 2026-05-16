package com.b4rrhh.employee.absence.application.usecase;

import com.b4rrhh.employee.absence.domain.model.Absence;
import com.b4rrhh.employee.absence.domain.port.AbsenceRepository;
import com.b4rrhh.employee.employee.application.usecase.GetEmployeeByBusinessKeyUseCase;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class CloseOpenAbsenceAtTerminationService implements CloseOpenAbsenceAtTerminationUseCase {

    private final GetEmployeeByBusinessKeyUseCase getEmployee;
    private final AbsenceRepository absenceRepository;

    public CloseOpenAbsenceAtTerminationService(GetEmployeeByBusinessKeyUseCase getEmployee,
                                                 AbsenceRepository absenceRepository) {
        this.getEmployee = getEmployee;
        this.absenceRepository = absenceRepository;
    }

    @Override
    public void closeIfOpen(String ruleSystemCode, String employeeTypeCode,
                             String employeeNumber, LocalDate terminationDate) {
        getEmployee.getByBusinessKey(ruleSystemCode, employeeTypeCode, employeeNumber)
            .ifPresent(employee -> absenceRepository
                .findByEmployeeIdOrderByStartDateDescStartTimeDesc(employee.getId())
                .stream()
                .filter(Absence::isOpen)
                .findFirst()
                .ifPresent(absence -> {
                    Absence closed = absence.closeAt(terminationDate);
                    absenceRepository.save(closed);
                }));
    }
}
