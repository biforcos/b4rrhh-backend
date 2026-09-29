package com.b4rrhh.employee.payroll_input.infrastructure.web;

import com.b4rrhh.employee.payroll_input.domain.exception.EmployeePayrollInputAlreadyExistsException;
import com.b4rrhh.employee.payroll_input.domain.exception.EmployeePayrollInputNotFoundException;
import com.b4rrhh.employee.payroll_input.domain.exception.EmployeePayrollInputConceptInvalidException;
import com.b4rrhh.employee.payroll_input.domain.exception.EmployeePayrollInputEmployeeNotFoundException;
import com.b4rrhh.employee.payroll_input.domain.exception.EmployeePayrollInputOutsidePresenceException;
import com.b4rrhh.employee.payroll_input.infrastructure.web.dto.EmployeePayrollInputErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = EmployeePayrollInputController.class)
public class EmployeePayrollInputExceptionHandler {

    @ExceptionHandler(EmployeePayrollInputNotFoundException.class)
    public ResponseEntity<EmployeePayrollInputErrorResponse> handleNotFound(
            EmployeePayrollInputNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new EmployeePayrollInputErrorResponse(
                        "PAYROLL_INPUT_NOT_FOUND", ex.getMessage(), null));
    }

    @ExceptionHandler(EmployeePayrollInputAlreadyExistsException.class)
    public ResponseEntity<EmployeePayrollInputErrorResponse> handleConflict(
            EmployeePayrollInputAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new EmployeePayrollInputErrorResponse(
                        "PAYROLL_INPUT_ALREADY_EXISTS", ex.getMessage(), null));
    }

    @ExceptionHandler(EmployeePayrollInputEmployeeNotFoundException.class)
    public ResponseEntity<EmployeePayrollInputErrorResponse> handleEmployeeNotFound(
            EmployeePayrollInputEmployeeNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new EmployeePayrollInputErrorResponse(
                        "PAYROLL_INPUT_EMPLOYEE_NOT_FOUND", ex.getMessage(), null));
    }

    @ExceptionHandler(EmployeePayrollInputConceptInvalidException.class)
    public ResponseEntity<EmployeePayrollInputErrorResponse> handleConceptInvalid(
            EmployeePayrollInputConceptInvalidException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new EmployeePayrollInputErrorResponse(
                        "PAYROLL_INPUT_CONCEPT_INVALID", ex.getMessage(), null));
    }

    @ExceptionHandler(EmployeePayrollInputOutsidePresenceException.class)
    public ResponseEntity<EmployeePayrollInputErrorResponse> handleOutsidePresence(
            EmployeePayrollInputOutsidePresenceException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new EmployeePayrollInputErrorResponse(
                        "PAYROLL_INPUT_OUTSIDE_PRESENCE", ex.getMessage(), null));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<EmployeePayrollInputErrorResponse> handleBadRequest(
            IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new EmployeePayrollInputErrorResponse(
                        "PAYROLL_INPUT_BAD_REQUEST", ex.getMessage(), null));
    }
}
