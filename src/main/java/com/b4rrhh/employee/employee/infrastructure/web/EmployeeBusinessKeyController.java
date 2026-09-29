package com.b4rrhh.employee.employee.infrastructure.web;

import com.b4rrhh.employee.employee.application.usecase.GetEmployeeByBusinessKeyUseCase;
import com.b4rrhh.employee.employee.application.usecase.DeleteEmployeeByBusinessKeyCommand;
import com.b4rrhh.employee.employee.application.usecase.DeleteEmployeeByBusinessKeyUseCase;
import com.b4rrhh.employee.employee.application.usecase.UpdateEmployeeCommand;
import com.b4rrhh.employee.employee.application.usecase.UpdateEmployeeUseCase;
import com.b4rrhh.employee.employee.domain.model.Employee;
import com.b4rrhh.employee.employee.infrastructure.web.dto.EmployeeResponse;
import com.b4rrhh.employee.employee.infrastructure.web.dto.UpdateEmployeeRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
public class EmployeeBusinessKeyController {

    private final GetEmployeeByBusinessKeyUseCase getEmployeeByBusinessKeyUseCase;
    private final DeleteEmployeeByBusinessKeyUseCase deleteEmployeeByBusinessKeyUseCase;
    private final UpdateEmployeeUseCase updateEmployeeUseCase;
    private final EmployeeResponseAssembler employeeResponseAssembler;

    public EmployeeBusinessKeyController(
            GetEmployeeByBusinessKeyUseCase getEmployeeByBusinessKeyUseCase,
            DeleteEmployeeByBusinessKeyUseCase deleteEmployeeByBusinessKeyUseCase,
            UpdateEmployeeUseCase updateEmployeeUseCase,
            EmployeeResponseAssembler employeeResponseAssembler
    ) {
        this.getEmployeeByBusinessKeyUseCase = getEmployeeByBusinessKeyUseCase;
        this.deleteEmployeeByBusinessKeyUseCase = deleteEmployeeByBusinessKeyUseCase;
        this.updateEmployeeUseCase = updateEmployeeUseCase;
        this.employeeResponseAssembler = employeeResponseAssembler;
    }

    @GetMapping("/employees/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}")
    public ResponseEntity<EmployeeResponse> getByBusinessKey(
            @PathVariable String ruleSystemCode,
            @PathVariable String employeeTypeCode,
            @PathVariable String employeeNumber,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate referenceDate
    ) {
        return getEmployeeByBusinessKeyUseCase.getByBusinessKey(ruleSystemCode, employeeTypeCode, employeeNumber)
                .map(employee -> ResponseEntity.ok(employeeResponseAssembler.toResponse(employee, referenceDate)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/employees/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}")
    public ResponseEntity<EmployeeResponse> updateByBusinessKey(
            @PathVariable String ruleSystemCode,
            @PathVariable String employeeTypeCode,
            @PathVariable String employeeNumber,
            @RequestBody UpdateEmployeeRequest request
    ) {
        Employee updated = updateEmployeeUseCase.update(
                new UpdateEmployeeCommand(
                        ruleSystemCode,
                        employeeTypeCode,
                        employeeNumber,
                        request.firstName(),
                        request.lastName1(),
                        request.lastName2(),
                        request.preferredName()
                )
        );

        return ResponseEntity.ok(employeeResponseAssembler.toResponse(updated));
    }

    @DeleteMapping("/employees/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}")
    public ResponseEntity<Void> deleteByBusinessKey(
            @PathVariable String ruleSystemCode,
            @PathVariable String employeeTypeCode,
            @PathVariable String employeeNumber
    ) {
        deleteEmployeeByBusinessKeyUseCase.delete(new DeleteEmployeeByBusinessKeyCommand(
                ruleSystemCode,
                employeeTypeCode,
                employeeNumber
        ));

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

}
