package com.b4rrhh.employee.absence.application.usecase;

import com.b4rrhh.employee.absence.domain.exception.AbsenceNotFoundException;
import com.b4rrhh.employee.absence.domain.port.AbsenceRepository;
import com.b4rrhh.employee.employee.application.usecase.GetEmployeeByBusinessKeyUseCase;
import org.springframework.stereotype.Service;

@Service
public class DeleteAbsenceService implements DeleteAbsenceUseCase {

    private final GetEmployeeByBusinessKeyUseCase getEmployee;
    private final AbsenceRepository absenceRepository;

    public DeleteAbsenceService(GetEmployeeByBusinessKeyUseCase getEmployee,
                                 AbsenceRepository absenceRepository) {
        this.getEmployee = getEmployee;
        this.absenceRepository = absenceRepository;
    }

    @Override
    public void delete(DeleteAbsenceCommand command) {
        Long employeeId = getEmployee.getByBusinessKey(
                command.ruleSystemCode(), command.employeeTypeCode(), command.employeeNumber())
            .map(e -> e.getId())
            .orElseThrow(() -> new AbsenceNotFoundException("Employee not found: " + command.employeeNumber()));

        absenceRepository.findByKey(employeeId, command.absenceTypeCode(), command.startDate(), command.startTime())
            .orElseThrow(() -> new AbsenceNotFoundException(
                "Absence not found: " + command.absenceTypeCode() + "/" + command.startDate()));

        absenceRepository.deleteByKey(employeeId, command.absenceTypeCode(), command.startDate(), command.startTime());
    }
}
