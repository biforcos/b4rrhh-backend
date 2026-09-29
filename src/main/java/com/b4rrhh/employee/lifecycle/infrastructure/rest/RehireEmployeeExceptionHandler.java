package com.b4rrhh.employee.lifecycle.infrastructure.rest;

import com.b4rrhh.employee.contract.domain.exception.ContractEmployeeNotFoundException;
import com.b4rrhh.employee.employee.domain.exception.EmployeeRuleSystemNotFoundException;
import com.b4rrhh.employee.labor_classification.domain.exception.LaborClassificationEmployeeNotFoundException;
import com.b4rrhh.employee.lifecycle.domain.exception.RehireEmployeeCatalogValueInvalidException;
import com.b4rrhh.employee.lifecycle.domain.exception.RehireEmployeeBusinessValidationException;
import com.b4rrhh.employee.lifecycle.domain.exception.RehireEmployeeConflictException;
import com.b4rrhh.employee.lifecycle.domain.exception.RehireEmployeeDependentRelationInvalidException;
import com.b4rrhh.employee.lifecycle.domain.exception.RehireEmployeeDistributionInvalidException;
import com.b4rrhh.employee.lifecycle.domain.exception.RehireEmployeeEmployeeNotFoundException;
import com.b4rrhh.employee.lifecycle.domain.exception.RehireEmployeeIdentifierOfAnotherEmployeeException;
import com.b4rrhh.employee.lifecycle.domain.exception.RehireEmployeeRequestInvalidException;
import com.b4rrhh.employee.lifecycle.infrastructure.rest.dto.RehireEmployeeErrorResponse;
import com.b4rrhh.employee.presence.domain.exception.PresenceEmployeeNotFoundException;
import com.b4rrhh.employee.presence.domain.exception.PresenceRuleSystemNotFoundException;
import com.b4rrhh.employee.workcenter.domain.exception.WorkCenterCompanyMismatchException;
import com.b4rrhh.employee.workcenter.domain.exception.WorkCenterCatalogValueInvalidException;
import com.b4rrhh.employee.workcenter.domain.exception.WorkCenterEmployeeNotFoundException;
import com.b4rrhh.employee.workcenter.domain.exception.WorkCenterRuleSystemNotFoundException;
import com.b4rrhh.rulesystem.domain.exception.RuleSystemNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = RehireEmployeeController.class)
public class RehireEmployeeExceptionHandler {

    @ExceptionHandler(RehireEmployeeRequestInvalidException.class)
    public ResponseEntity<RehireEmployeeErrorResponse> handleInvalidRequest(RehireEmployeeRequestInvalidException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new RehireEmployeeErrorResponse("REHIRE_REQUEST_INVALID", ex.getMessage(), null));
    }

    @ExceptionHandler(RehireEmployeeCatalogValueInvalidException.class)
    public ResponseEntity<RehireEmployeeErrorResponse> handleCatalogInvalid(RehireEmployeeCatalogValueInvalidException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new RehireEmployeeErrorResponse("INVALID_CATALOG_VALUE", ex.getMessage(), null));
    }

    @ExceptionHandler(WorkCenterCatalogValueInvalidException.class)
    public ResponseEntity<RehireEmployeeErrorResponse> handleWorkCenterCatalogInvalid(WorkCenterCatalogValueInvalidException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new RehireEmployeeErrorResponse("INVALID_CATALOG_VALUE", ex.getMessage(), null));
    }

    @ExceptionHandler(RehireEmployeeDependentRelationInvalidException.class)
    public ResponseEntity<RehireEmployeeErrorResponse> handleDependentInvalid(RehireEmployeeDependentRelationInvalidException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new RehireEmployeeErrorResponse("INVALID_DEPENDENT_RELATION", ex.getMessage(), null));
    }

    @ExceptionHandler(RehireEmployeeDistributionInvalidException.class)
    public ResponseEntity<RehireEmployeeErrorResponse> handleDistributionInvalid(RehireEmployeeDistributionInvalidException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new RehireEmployeeErrorResponse("INVALID_DISTRIBUTION", ex.getMessage(), null));
    }

    @ExceptionHandler(RehireEmployeeBusinessValidationException.class)
    public ResponseEntity<RehireEmployeeErrorResponse> handleBusinessValidation(RehireEmployeeBusinessValidationException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new RehireEmployeeErrorResponse("REHIRE_BUSINESS_VALIDATION", ex.getMessage(), null));
    }

    @ExceptionHandler(RehireEmployeeIdentifierOfAnotherEmployeeException.class)
    public ResponseEntity<RehireEmployeeErrorResponse> handleIdentifierOfAnotherEmployee(RehireEmployeeIdentifierOfAnotherEmployeeException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new RehireEmployeeErrorResponse("REHIRE_IDENTIFIER_OF_ANOTHER_EMPLOYEE", ex.getMessage(), null));
    }

    @ExceptionHandler(RehireEmployeeConflictException.class)
    public ResponseEntity<RehireEmployeeErrorResponse> handleConflict(RehireEmployeeConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new RehireEmployeeErrorResponse("REHIRE_CONFLICT", ex.getMessage(), null));
    }

    @ExceptionHandler(WorkCenterCompanyMismatchException.class)
    public ResponseEntity<RehireEmployeeErrorResponse> handleWorkCenterCompanyMismatch(WorkCenterCompanyMismatchException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new RehireEmployeeErrorResponse("WORK_CENTER_COMPANY_MISMATCH", ex.getMessage(), null));
    }

    @ExceptionHandler({
            RehireEmployeeEmployeeNotFoundException.class,
            RuleSystemNotFoundException.class,
            EmployeeRuleSystemNotFoundException.class,
            PresenceRuleSystemNotFoundException.class,
            PresenceEmployeeNotFoundException.class,
            LaborClassificationEmployeeNotFoundException.class,
            ContractEmployeeNotFoundException.class,
            WorkCenterRuleSystemNotFoundException.class,
            WorkCenterEmployeeNotFoundException.class
    })
    public ResponseEntity<RehireEmployeeErrorResponse> handleNotFound(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new RehireEmployeeErrorResponse("REHIRE_DEPENDENCY_NOT_FOUND", ex.getMessage(), null));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<RehireEmployeeErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new RehireEmployeeErrorResponse("REHIRE_REQUEST_INVALID", ex.getMessage(), null));
    }
}
