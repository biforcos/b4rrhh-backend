package com.b4rrhh.employee.photo.infrastructure.web;

import com.b4rrhh.employee.employee.application.DisplayNameComputationService;
import com.b4rrhh.employee.employee.application.usecase.GetEmployeeByBusinessKeyUseCase;
import com.b4rrhh.employee.employee.domain.model.Employee;
import com.b4rrhh.employee.employee.infrastructure.web.dto.EmployeeResponse;
import com.b4rrhh.employee.photo.application.usecase.ConfirmEmployeePhotoCommand;
import com.b4rrhh.employee.photo.application.usecase.ConfirmEmployeePhotoUseCase;
import com.b4rrhh.employee.photo.application.usecase.DeleteEmployeePhotoCommand;
import com.b4rrhh.employee.photo.application.usecase.DeleteEmployeePhotoUseCase;
import com.b4rrhh.employee.photo.application.usecase.GeneratePhotoUploadUrlCommand;
import com.b4rrhh.employee.photo.application.usecase.GeneratePhotoUploadUrlResult;
import com.b4rrhh.employee.photo.application.usecase.GeneratePhotoUploadUrlUseCase;
import com.b4rrhh.employee.photo.infrastructure.web.dto.ConfirmEmployeePhotoRequest;
import com.b4rrhh.employee.photo.infrastructure.web.dto.GeneratePhotoUploadUrlResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/employees/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}/photo")
public class EmployeePhotoController {

    private final GeneratePhotoUploadUrlUseCase generatePhotoUploadUrlUseCase;
    private final ConfirmEmployeePhotoUseCase confirmEmployeePhotoUseCase;
    private final DeleteEmployeePhotoUseCase deleteEmployeePhotoUseCase;
    private final GetEmployeeByBusinessKeyUseCase getEmployeeByBusinessKeyUseCase;
    private final DisplayNameComputationService displayNameComputationService;

    public EmployeePhotoController(
            GeneratePhotoUploadUrlUseCase generatePhotoUploadUrlUseCase,
            ConfirmEmployeePhotoUseCase confirmEmployeePhotoUseCase,
            DeleteEmployeePhotoUseCase deleteEmployeePhotoUseCase,
            GetEmployeeByBusinessKeyUseCase getEmployeeByBusinessKeyUseCase,
            DisplayNameComputationService displayNameComputationService) {
        this.generatePhotoUploadUrlUseCase = generatePhotoUploadUrlUseCase;
        this.confirmEmployeePhotoUseCase = confirmEmployeePhotoUseCase;
        this.deleteEmployeePhotoUseCase = deleteEmployeePhotoUseCase;
        this.getEmployeeByBusinessKeyUseCase = getEmployeeByBusinessKeyUseCase;
        this.displayNameComputationService = displayNameComputationService;
    }

    @PostMapping("/upload-url")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GeneratePhotoUploadUrlResponse> generateUploadUrl(
            @PathVariable String ruleSystemCode,
            @PathVariable String employeeTypeCode,
            @PathVariable String employeeNumber
    ) {
        GeneratePhotoUploadUrlResult result = generatePhotoUploadUrlUseCase.generate(
                new GeneratePhotoUploadUrlCommand(ruleSystemCode, employeeTypeCode, employeeNumber));

        return ResponseEntity.ok(
                new GeneratePhotoUploadUrlResponse(result.uploadUrl(), result.objectKey()));
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EmployeeResponse> confirmPhoto(
            @PathVariable String ruleSystemCode,
            @PathVariable String employeeTypeCode,
            @PathVariable String employeeNumber,
            @RequestBody ConfirmEmployeePhotoRequest request
    ) {
        confirmEmployeePhotoUseCase.confirm(
                new ConfirmEmployeePhotoCommand(
                        ruleSystemCode, employeeTypeCode, employeeNumber, request.objectKey()));

        Employee employee = getEmployeeByBusinessKeyUseCase
                .getByBusinessKey(ruleSystemCode, employeeTypeCode, employeeNumber)
                .orElseThrow();

        return ResponseEntity.ok(toResponse(employee));
    }

    @DeleteMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletePhoto(
            @PathVariable String ruleSystemCode,
            @PathVariable String employeeTypeCode,
            @PathVariable String employeeNumber
    ) {
        deleteEmployeePhotoUseCase.delete(
                new DeleteEmployeePhotoCommand(ruleSystemCode, employeeTypeCode, employeeNumber));

        return ResponseEntity.noContent().build();
    }

    private EmployeeResponse toResponse(Employee employee) {
        String displayName = displayNameComputationService.compute(
                employee.getRuleSystemCode(),
                employee.getFirstName(),
                employee.getLastName1(),
                employee.getLastName2(),
                employee.getPreferredName()
        );
        return new EmployeeResponse(
                employee.getRuleSystemCode(),
                employee.getEmployeeTypeCode(),
                employee.getEmployeeNumber(),
                employee.getFirstName(),
                employee.getLastName1(),
                employee.getLastName2(),
                employee.getPreferredName(),
                displayName,
                employee.getStatus(),
                employee.getPhotoUrl()
        );
    }
}
