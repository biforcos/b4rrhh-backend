package com.b4rrhh.employee.absence.application.usecase;

import com.b4rrhh.employee.absence.domain.exception.AbsenceEmployeeNotFoundException;
import com.b4rrhh.employee.absence.domain.model.Absence;
import com.b4rrhh.employee.absence.domain.port.AbsenceRepository;
import com.b4rrhh.employee.employee.application.usecase.GetEmployeeByBusinessKeyUseCase;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListEmployeeAbsencesService implements ListEmployeeAbsencesUseCase {

    private final GetEmployeeByBusinessKeyUseCase getEmployee;
    private final AbsenceRepository absenceRepository;

    public ListEmployeeAbsencesService(GetEmployeeByBusinessKeyUseCase getEmployee,
                                        AbsenceRepository absenceRepository) {
        this.getEmployee = getEmployee;
        this.absenceRepository = absenceRepository;
    }

    @Override
    public List<Absence> listByEmployeeBusinessKey(ListEmployeeAbsencesCommand command) {
        Long employeeId = getEmployee.getByBusinessKey(
                command.ruleSystemCode(), command.employeeTypeCode(), command.employeeNumber())
            .map(e -> e.getId())
            .orElseThrow(() -> new AbsenceEmployeeNotFoundException(
                "Employee not found: " + command.ruleSystemCode() + "/" +
                command.employeeTypeCode() + "/" + command.employeeNumber()));
        return absenceRepository.findByEmployeeIdOrderByStartDateDescStartTimeDesc(employeeId);
    }
}
