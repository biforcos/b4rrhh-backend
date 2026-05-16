package com.b4rrhh.employee.absence.application.usecase;

import com.b4rrhh.employee.absence.domain.exception.AbsenceEmployeeNotFoundException;
import com.b4rrhh.employee.absence.domain.exception.AbsenceNotFoundException;
import com.b4rrhh.employee.absence.domain.model.Absence;
import com.b4rrhh.employee.absence.domain.port.AbsenceRepository;
import com.b4rrhh.employee.employee.application.usecase.GetEmployeeByBusinessKeyUseCase;
import org.springframework.stereotype.Service;

@Service
public class GetAbsenceByBusinessKeyService implements GetAbsenceByBusinessKeyUseCase {

    private final GetEmployeeByBusinessKeyUseCase getEmployee;
    private final AbsenceRepository absenceRepository;

    public GetAbsenceByBusinessKeyService(GetEmployeeByBusinessKeyUseCase getEmployee,
                                           AbsenceRepository absenceRepository) {
        this.getEmployee = getEmployee;
        this.absenceRepository = absenceRepository;
    }

    @Override
    public Absence getByBusinessKey(GetAbsenceByBusinessKeyCommand command) {
        Long employeeId = getEmployee.getByBusinessKey(
                command.ruleSystemCode(), command.employeeTypeCode(), command.employeeNumber())
            .map(e -> e.getId())
            .orElseThrow(() -> new AbsenceEmployeeNotFoundException(
                "Employee not found: " + command.ruleSystemCode() + "/" +
                command.employeeTypeCode() + "/" + command.employeeNumber()));

        return absenceRepository.findByKey(
                employeeId, command.absenceTypeCode(), command.startDate(), command.startTime())
            .orElseThrow(() -> new AbsenceNotFoundException(
                "Absence not found: " + command.absenceTypeCode() + "/" + command.startDate()));
    }
}
