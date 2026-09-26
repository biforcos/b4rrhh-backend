package com.b4rrhh.employee.working_time.application.usecase;

import com.b4rrhh.employee.shared.application.port.DatedWrite;
import com.b4rrhh.employee.shared.application.port.DatedWriteNoticePort;
import com.b4rrhh.employee.shared.application.port.DatedWriteSources;
import com.b4rrhh.employee.working_time.application.port.EmployeeWorkingTimeContext;
import com.b4rrhh.employee.working_time.application.port.EmployeeWorkingTimeLookupPort;
import com.b4rrhh.employee.working_time.application.service.WorkingTimePresenceConsistencyValidator;
import com.b4rrhh.employee.working_time.domain.exception.WorkingTimeEmployeeNotFoundException;
import com.b4rrhh.employee.working_time.domain.exception.WorkingTimeNotFoundException;
import com.b4rrhh.employee.working_time.domain.model.WorkingTime;
import com.b4rrhh.employee.working_time.domain.port.WorkingTimeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CloseWorkingTimeService implements CloseWorkingTimeUseCase {

    private static final String TABLA = "employee.working_time";

    private final WorkingTimeRepository workingTimeRepository;
    private final EmployeeWorkingTimeLookupPort employeeWorkingTimeLookupPort;
    private final WorkingTimePresenceConsistencyValidator workingTimePresenceConsistencyValidator;
    private final DatedWriteNoticePort datedWrites;

    public CloseWorkingTimeService(
            WorkingTimeRepository workingTimeRepository,
            EmployeeWorkingTimeLookupPort employeeWorkingTimeLookupPort,
            WorkingTimePresenceConsistencyValidator workingTimePresenceConsistencyValidator,
            DatedWriteNoticePort datedWrites
    ) {
        this.workingTimeRepository = workingTimeRepository;
        this.employeeWorkingTimeLookupPort = employeeWorkingTimeLookupPort;
        this.workingTimePresenceConsistencyValidator = workingTimePresenceConsistencyValidator;
        this.datedWrites = datedWrites;
    }

    @Override
    @Transactional
    public WorkingTime close(CloseWorkingTimeCommand command) {
        String normalizedRuleSystemCode = normalizeRuleSystemCode(command.ruleSystemCode());
        String normalizedEmployeeTypeCode = normalizeEmployeeTypeCode(command.employeeTypeCode());
        String normalizedEmployeeNumber = normalizeEmployeeNumber(command.employeeNumber());
        Integer normalizedWorkingTimeNumber = normalizeWorkingTimeNumber(command.workingTimeNumber());

        EmployeeWorkingTimeContext employee = employeeWorkingTimeLookupPort
                .findByBusinessKeyForUpdate(
                        normalizedRuleSystemCode,
                        normalizedEmployeeTypeCode,
                        normalizedEmployeeNumber
                )
                .orElseThrow(() -> new WorkingTimeEmployeeNotFoundException(
                        normalizedRuleSystemCode,
                        normalizedEmployeeTypeCode,
                        normalizedEmployeeNumber
                ));

        WorkingTime existing = workingTimeRepository.findByEmployeeIdAndWorkingTimeNumber(
                        employee.employeeId(),
                        normalizedWorkingTimeNumber
                )
                .orElseThrow(() -> new WorkingTimeNotFoundException(
                        normalizedRuleSystemCode,
                        normalizedEmployeeTypeCode,
                        normalizedEmployeeNumber,
                        normalizedWorkingTimeNumber
                ));

        WorkingTime closed = existing.close(command.endDate());

        workingTimePresenceConsistencyValidator.validatePeriodWithinPresence(
                employee.employeeId(),
                closed.getStartDate(),
                closed.getEndDate(),
                normalizedRuleSystemCode,
                normalizedEmployeeTypeCode,
                normalizedEmployeeNumber
        );

        WorkingTime guardada = workingTimeRepository.save(closed);

        // El dia SIGUIENTE al cierre: cerrar el 20 de agosto no cambia nada hasta el 21, que es
        // cuando la cobertura desaparece. Se leen del objeto que ya existia y no de lo que devuelve el
        // repositorio: en un cierre la fila esta, asi que su id y su fecha se conocen antes de guardar
        // (backend#130).
        datedWrites.notice(DatedWrite.on(closed.getEndDate().plusDays(1),
                normalizedRuleSystemCode, normalizedEmployeeTypeCode, normalizedEmployeeNumber,
                DatedWriteSources.WORKING_TIME, TABLA, closed.getId()));

        return guardada;
    }

    private String normalizeRuleSystemCode(String ruleSystemCode) {
        if (ruleSystemCode == null || ruleSystemCode.trim().isEmpty()) {
            throw new IllegalArgumentException("ruleSystemCode is required");
        }

        return ruleSystemCode.trim().toUpperCase();
    }

    private String normalizeEmployeeTypeCode(String employeeTypeCode) {
        if (employeeTypeCode == null || employeeTypeCode.trim().isEmpty()) {
            throw new IllegalArgumentException("employeeTypeCode is required");
        }

        return employeeTypeCode.trim().toUpperCase();
    }

    private String normalizeEmployeeNumber(String employeeNumber) {
        if (employeeNumber == null || employeeNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("employeeNumber is required");
        }

        return employeeNumber.trim();
    }

    private Integer normalizeWorkingTimeNumber(Integer workingTimeNumber) {
        if (workingTimeNumber == null || workingTimeNumber <= 0) {
            throw new IllegalArgumentException("workingTimeNumber must be a positive integer");
        }

        return workingTimeNumber;
    }
}