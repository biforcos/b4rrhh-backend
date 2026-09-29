package com.b4rrhh.payroll.year.infrastructure.web;

import com.b4rrhh.payroll.year.domain.exception.EmployeeYearEmployeeNotFoundException;
import com.b4rrhh.payroll.year.infrastructure.web.dto.EmployeeYearErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = EmployeeYearController.class)
public class EmployeeYearExceptionHandler {

    @ExceptionHandler(EmployeeYearEmployeeNotFoundException.class)
    public ResponseEntity<EmployeeYearErrorResponse> handleEmployeeNotFound(EmployeeYearEmployeeNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new EmployeeYearErrorResponse("EMPLOYEE_YEAR_EMPLOYEE_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<EmployeeYearErrorResponse> handleInvalid(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new EmployeeYearErrorResponse("EMPLOYEE_YEAR_REQUEST_INVALID", ex.getMessage()));
    }
}
