package com.b4rrhh.employee.absence.application.usecase;

import com.b4rrhh.employee.absence.domain.exception.AbsenceEmployeeNotFoundException;
import com.b4rrhh.employee.absence.domain.exception.AbsenceNotFoundException;
import com.b4rrhh.employee.absence.domain.model.Absence;
import com.b4rrhh.employee.absence.domain.port.AbsenceRepository;
import com.b4rrhh.employee.employee.application.usecase.GetEmployeeByBusinessKeyUseCase;
import com.b4rrhh.employee.shared.application.port.DatedWrite;
import com.b4rrhh.employee.shared.application.port.DatedWriteNoticePort;
import com.b4rrhh.employee.shared.application.port.DatedWriteSources;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeleteAbsenceService implements DeleteAbsenceUseCase {

    private static final String TABLA = "employee.employee_absence";

    private final GetEmployeeByBusinessKeyUseCase getEmployee;
    private final AbsenceRepository absenceRepository;
    private final DatedWriteNoticePort datedWrites;

    public DeleteAbsenceService(GetEmployeeByBusinessKeyUseCase getEmployee,
                                 AbsenceRepository absenceRepository,
                                 DatedWriteNoticePort datedWrites) {
        this.getEmployee = getEmployee;
        this.absenceRepository = absenceRepository;
        this.datedWrites = datedWrites;
    }

    @Override
    @Transactional
    public void delete(DeleteAbsenceCommand command) {
        Long employeeId = getEmployee.getByBusinessKey(
                command.ruleSystemCode(), command.employeeTypeCode(), command.employeeNumber())
            .map(e -> e.getId())
            .orElseThrow(() -> new AbsenceEmployeeNotFoundException(
                "No existe el empleado " + command.ruleSystemCode() + "/" +
                command.employeeTypeCode() + "/" + command.employeeNumber() + "."));

        Absence existente = absenceRepository
            .findByKey(employeeId, command.absenceTypeCode(), command.startDate(), command.startTime())
            .orElseThrow(() -> new AbsenceNotFoundException(
                "Absence not found: " + command.absenceTypeCode() + "/" + command.startDate()));

        absenceRepository.deleteByKey(employeeId, command.absenceTypeCode(), command.startDate(), command.startTime());

        // Borrar una baja de un mes cerrado le devuelve los dias que le habia quitado (ADR-073): el
        // atraso sale positivo. La fila ya no esta y la marca es lo unico que queda de ella
        // (backend#130).
        datedWrites.notice(DatedWrite.on(existente.getStartDate(),
                command.ruleSystemCode(), command.employeeTypeCode(), command.employeeNumber(),
                DatedWriteSources.ABSENCE, TABLA, existente.getId()));
    }
}
