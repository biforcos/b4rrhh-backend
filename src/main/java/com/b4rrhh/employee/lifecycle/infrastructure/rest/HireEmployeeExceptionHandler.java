package com.b4rrhh.employee.lifecycle.infrastructure.rest;

import com.b4rrhh.employee.lifecycle.domain.exception.HireEmployeeAlreadyExistsException;
import com.b4rrhh.employee.lifecycle.domain.exception.HireEmployeeBusinessValidationException;
import com.b4rrhh.employee.lifecycle.domain.exception.HireEmployeeCatalogValueInvalidException;
import com.b4rrhh.employee.lifecycle.domain.exception.HireEmployeeConflictException;
import com.b4rrhh.employee.lifecycle.domain.exception.HireEmployeeDependentRelationInvalidException;
import com.b4rrhh.employee.lifecycle.domain.exception.HireEmployeeEntryReasonNotHiringException;
import com.b4rrhh.employee.lifecycle.domain.exception.HireEmployeeRequestInvalidException;
import com.b4rrhh.employee.lifecycle.infrastructure.rest.dto.HireEmployeeErrorResponse;
import com.b4rrhh.employee.employee.domain.exception.EmployeeRuleSystemNotFoundException;
import com.b4rrhh.employee.labor_classification.domain.exception.LaborClassificationEmployeeNotFoundException;
import com.b4rrhh.employee.presence.domain.exception.PresenceEmployeeNotFoundException;
import com.b4rrhh.employee.presence.domain.exception.PresenceRuleSystemNotFoundException;
import com.b4rrhh.employee.contract.domain.exception.ContractEmployeeNotFoundException;
import com.b4rrhh.employee.working_time.domain.exception.WorkingTimeEmployeeNotFoundException;
import com.b4rrhh.employee.workcenter.domain.exception.WorkCenterCatalogValueInvalidException;
import com.b4rrhh.employee.workcenter.domain.exception.WorkCenterCompanyMismatchException;
import com.b4rrhh.employee.workcenter.domain.exception.WorkCenterEmployeeNotFoundException;
import com.b4rrhh.employee.workcenter.domain.exception.WorkCenterRuleSystemNotFoundException;
import com.b4rrhh.rulesystem.domain.exception.RuleSystemNotFoundException;
import com.b4rrhh.rulesystem.employeenumbering.domain.exception.EmployeeNumberingConfigNotFoundException;
import com.b4rrhh.rulesystem.employeenumbering.domain.exception.EmployeeNumberingExhaustedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = HireEmployeeController.class)
public class HireEmployeeExceptionHandler {

    @ExceptionHandler(HireEmployeeAlreadyExistsException.class)
    public ResponseEntity<HireEmployeeErrorResponse> handleAlreadyExists(HireEmployeeAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new HireEmployeeErrorResponse("EMPLOYEE_ALREADY_EXISTS", ex.getMessage(), null));
    }

    @ExceptionHandler(HireEmployeeConflictException.class)
    public ResponseEntity<HireEmployeeErrorResponse> handleConflict(HireEmployeeConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new HireEmployeeErrorResponse("HIRE_CONFLICT", ex.getMessage(), null));
    }

    @ExceptionHandler(WorkCenterCompanyMismatchException.class)
    public ResponseEntity<HireEmployeeErrorResponse> handleWorkCenterCompanyMismatch(WorkCenterCompanyMismatchException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new HireEmployeeErrorResponse("WORK_CENTER_COMPANY_MISMATCH", ex.getMessage(), null));
    }

    @ExceptionHandler(HireEmployeeCatalogValueInvalidException.class)
    public ResponseEntity<HireEmployeeErrorResponse> handleInvalidCatalog(HireEmployeeCatalogValueInvalidException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new HireEmployeeErrorResponse("INVALID_CATALOG_VALUE", ex.getMessage(), null));
    }

    @ExceptionHandler(WorkCenterCatalogValueInvalidException.class)
    public ResponseEntity<HireEmployeeErrorResponse> handleWorkCenterCatalogInvalid(WorkCenterCatalogValueInvalidException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new HireEmployeeErrorResponse("INVALID_CATALOG_VALUE", ex.getMessage(), null));
    }

    @ExceptionHandler(HireEmployeeDependentRelationInvalidException.class)
    public ResponseEntity<HireEmployeeErrorResponse> handleInvalidDependentRelation(HireEmployeeDependentRelationInvalidException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new HireEmployeeErrorResponse("INVALID_DEPENDENT_RELATION", ex.getMessage(), null));
    }

    @ExceptionHandler(HireEmployeeEntryReasonNotHiringException.class)
    public ResponseEntity<HireEmployeeErrorResponse> handleEntryReasonNotHiring(HireEmployeeEntryReasonNotHiringException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new HireEmployeeErrorResponse("HIRE_ENTRY_REASON_NOT_HIRING", ex.getMessage(), null));
    }

    @ExceptionHandler(HireEmployeeBusinessValidationException.class)
    public ResponseEntity<HireEmployeeErrorResponse> handleBusinessValidation(HireEmployeeBusinessValidationException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new HireEmployeeErrorResponse("HIRE_BUSINESS_VALIDATION_FAILED", ex.getMessage(), null));
    }

    @ExceptionHandler(HireEmployeeRequestInvalidException.class)
    public ResponseEntity<HireEmployeeErrorResponse> handleRequestInvalid(HireEmployeeRequestInvalidException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new HireEmployeeErrorResponse("HIRE_REQUEST_INVALID", ex.getMessage(), null));
    }

    @ExceptionHandler({
            RuleSystemNotFoundException.class,
            EmployeeRuleSystemNotFoundException.class,
            PresenceRuleSystemNotFoundException.class,
            PresenceEmployeeNotFoundException.class,
            LaborClassificationEmployeeNotFoundException.class,
            ContractEmployeeNotFoundException.class,
                WorkingTimeEmployeeNotFoundException.class,
            WorkCenterRuleSystemNotFoundException.class,
            WorkCenterEmployeeNotFoundException.class
    })
    public ResponseEntity<HireEmployeeErrorResponse> handleNotFound(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new HireEmployeeErrorResponse("HIRE_DEPENDENCY_NOT_FOUND", ex.getMessage(), null));
    }

    @ExceptionHandler(EmployeeNumberingConfigNotFoundException.class)
    public ResponseEntity<HireEmployeeErrorResponse> handleNumberingConfigNotFound(EmployeeNumberingConfigNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new HireEmployeeErrorResponse("NUMBERING_CONFIG_NOT_FOUND", ex.getMessage(), null));
    }

    @ExceptionHandler(EmployeeNumberingExhaustedException.class)
    public ResponseEntity<HireEmployeeErrorResponse> handleNumberingExhausted(EmployeeNumberingExhaustedException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new HireEmployeeErrorResponse("EMPLOYEE_NUMBERING_EXHAUSTED", ex.getMessage(), null));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<HireEmployeeErrorResponse> handleInvalidRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new HireEmployeeErrorResponse("HIRE_REQUEST_INVALID", ex.getMessage(), null));
    }
}
