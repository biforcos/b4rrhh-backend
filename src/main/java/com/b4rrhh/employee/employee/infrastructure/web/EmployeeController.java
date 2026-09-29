package com.b4rrhh.employee.employee.infrastructure.web;

import com.b4rrhh.employee.employee.application.usecase.CreateEmployeeCommand;
import com.b4rrhh.employee.employee.application.usecase.CreateEmployeeUseCase;
import com.b4rrhh.employee.employee.application.usecase.ListEmployeesQuery;
import com.b4rrhh.employee.employee.application.usecase.ListEmployeesUseCase;
import com.b4rrhh.employee.employee.domain.model.EmployeeDirectoryItem;
import com.b4rrhh.employee.employee.domain.model.EmployeeDirectoryPage;
import com.b4rrhh.employee.employee.domain.model.Employee;
import com.b4rrhh.employee.employee.infrastructure.web.dto.CreateEmployeeRequest;
import com.b4rrhh.employee.employee.infrastructure.web.dto.EmployeeDirectoryItemResponse;
import com.b4rrhh.employee.employee.infrastructure.web.dto.EmployeeDirectoryPageResponse;
import com.b4rrhh.employee.employee.infrastructure.web.dto.EmployeeResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/employees")
public class EmployeeController {

    private final CreateEmployeeUseCase createEmployeeUseCase;
    private final ListEmployeesUseCase listEmployeesUseCase;
    private final EmployeeResponseAssembler employeeResponseAssembler;

    public EmployeeController(
            CreateEmployeeUseCase createEmployeeUseCase,
            ListEmployeesUseCase listEmployeesUseCase,
            EmployeeResponseAssembler employeeResponseAssembler
    ) {
        this.createEmployeeUseCase = createEmployeeUseCase;
        this.listEmployeesUseCase = listEmployeesUseCase;
        this.employeeResponseAssembler = employeeResponseAssembler;
    }

    @GetMapping
    public ResponseEntity<EmployeeDirectoryPageResponse> listEmployees(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String ruleSystemCode,
            @RequestParam(required = false) String employeeTypeCode,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) Integer page
    ) {
        EmployeeDirectoryPage found = listEmployeesUseCase
                .list(new ListEmployeesQuery(q, ruleSystemCode, employeeTypeCode, status, page, size));

        List<EmployeeDirectoryItemResponse> items = found.items().stream()
                .map(this::toDirectoryResponse)
                .toList();

        return ResponseEntity.ok(
                new EmployeeDirectoryPageResponse(items, found.page(), found.size(), found.total())
        );
    }

    @PostMapping
    public ResponseEntity<EmployeeResponse> createEmployee(@RequestBody CreateEmployeeRequest request) {
        Employee createdEmployee = createEmployeeUseCase.create(
                new CreateEmployeeCommand(
                        request.ruleSystemCode(),
                        request.employeeTypeCode(),
                        request.employeeNumber(),
                        request.firstName(),
                        request.lastName1(),
                        request.lastName2(),
                        request.preferredName()
                )
        );

        return ResponseEntity.status(201).body(employeeResponseAssembler.toResponse(createdEmployee));
    }

    private EmployeeDirectoryItemResponse toDirectoryResponse(EmployeeDirectoryItem employee) {
        return new EmployeeDirectoryItemResponse(
                employee.getRuleSystemCode(),
                employee.getEmployeeTypeCode(),
                employee.getEmployeeNumber(),
                employee.getDisplayName(),
                employee.getStatus(),
                employee.getWorkCenterCode()
        );
    }
}