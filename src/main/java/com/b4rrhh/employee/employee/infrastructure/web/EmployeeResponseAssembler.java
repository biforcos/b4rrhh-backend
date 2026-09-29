package com.b4rrhh.employee.employee.infrastructure.web;

import com.b4rrhh.employee.employee.application.DisplayNameComputationService;
import com.b4rrhh.employee.presence.application.usecase.GetEmployeeStandingUseCase;
import com.b4rrhh.employee.employee.domain.model.Employee;
import com.b4rrhh.employee.presence.domain.model.EmployeeStanding;
import com.b4rrhh.employee.employee.infrastructure.web.dto.EmployeeResponse;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * La ficha del empleado tal como la dice la API. El estado no viene del empleado: se lee de sus
 * presencias en la fecha pedida, hoy si no se pide ninguna (b4rrhh/backend#148).
 */
@Component
public class EmployeeResponseAssembler {

    private final DisplayNameComputationService displayNameComputationService;
    private final GetEmployeeStandingUseCase getEmployeeStandingUseCase;

    public EmployeeResponseAssembler(
            DisplayNameComputationService displayNameComputationService,
            GetEmployeeStandingUseCase getEmployeeStandingUseCase
    ) {
        this.displayNameComputationService = displayNameComputationService;
        this.getEmployeeStandingUseCase = getEmployeeStandingUseCase;
    }

    public EmployeeResponse toResponse(Employee employee) {
        return toResponse(employee, null);
    }

    public EmployeeResponse toResponse(Employee employee, LocalDate referenceDate) {
        String displayName = displayNameComputationService.compute(
                employee.getRuleSystemCode(),
                employee.getFirstName(),
                employee.getLastName1(),
                employee.getLastName2(),
                employee.getPreferredName()
        );
        EmployeeStanding standing = getEmployeeStandingUseCase.standingOf(employee.getId(), referenceDate);
        return new EmployeeResponse(
                employee.getRuleSystemCode(),
                employee.getEmployeeTypeCode(),
                employee.getEmployeeNumber(),
                employee.getFirstName(),
                employee.getLastName1(),
                employee.getLastName2(),
                employee.getPreferredName(),
                displayName,
                standing.status().name(),
                standing.statusDate(),
                standing.statusSince(),
                standing.plannedTerminationDate(),
                standing.plannedHireDate(),
                employee.getPhotoUrl()
        );
    }
}
