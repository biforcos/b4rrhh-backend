package com.b4rrhh.employee.absence.application.usecase;

import com.b4rrhh.employee.absence.domain.model.Absence;
import com.b4rrhh.employee.absence.domain.port.AbsenceRepository;
import com.b4rrhh.employee.employee.application.usecase.GetEmployeeByBusinessKeyUseCase;
import com.b4rrhh.employee.shared.application.port.DatedWrite;
import com.b4rrhh.employee.shared.application.port.DatedWriteNoticePort;
import com.b4rrhh.employee.shared.application.port.DatedWriteSources;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class CloseOpenAbsenceAtTerminationService implements CloseOpenAbsenceAtTerminationUseCase {

    private static final String TABLA = "employee.employee_absence";

    private final GetEmployeeByBusinessKeyUseCase getEmployee;
    private final AbsenceRepository absenceRepository;
    private final DatedWriteNoticePort datedWrites;

    public CloseOpenAbsenceAtTerminationService(GetEmployeeByBusinessKeyUseCase getEmployee,
                                                 AbsenceRepository absenceRepository,
                                                 DatedWriteNoticePort datedWrites) {
        this.getEmployee = getEmployee;
        this.absenceRepository = absenceRepository;
        this.datedWrites = datedWrites;
    }

    @Override
    public void closeIfOpen(String ruleSystemCode, String employeeTypeCode,
                             String employeeNumber, LocalDate terminationDate) {
        getEmployee.getByBusinessKey(ruleSystemCode, employeeTypeCode, employeeNumber)
            .ifPresent(employee -> absenceRepository
                .findByEmployeeIdOrderByStartDateDescStartTimeDesc(employee.getId())
                .stream()
                // overlap constraint guarantees at most one open absence per employee
                .filter(Absence::isOpen)
                .findFirst()
                .ifPresent(absence -> {
                    Absence closed = absence.closeAt(terminationDate);
                    absenceRepository.save(closed);

                    // El cese tambien es una escritura con fecha, y por eso este participante avisa:
                    // cesar a alguien con efecto en un mes cerrado le cambia el recibo de aquel mes.
                    // El dia SIGUIENTE al cierre, que es cuando la baja deja de estar (backend#130).
                    //
                    // El id se toma de la fila que ya existia y no de lo que devuelve el repositorio:
                    // en un cierre la fila esta, asi que su id se conoce antes de guardar.
                    datedWrites.notice(DatedWrite.on(terminationDate.plusDays(1),
                            ruleSystemCode, employeeTypeCode, employeeNumber,
                            DatedWriteSources.ABSENCE, TABLA, closed.getId()));
                }));
    }
}
