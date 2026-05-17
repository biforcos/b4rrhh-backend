package com.b4rrhh.employee.absence.infrastructure.web;

import com.b4rrhh.employee.absence.domain.exception.AbsenceCatalogValueInvalidException;
import com.b4rrhh.employee.absence.domain.exception.AbsenceEmployeeNotFoundException;
import com.b4rrhh.employee.absence.domain.exception.AbsenceNotFoundException;
import com.b4rrhh.employee.absence.domain.exception.AbsenceOutsidePresencePeriodException;
import com.b4rrhh.employee.absence.domain.exception.AbsenceOverlapException;
import com.b4rrhh.employee.absence.domain.exception.InvalidAbsenceDateRangeException;
import com.b4rrhh.employee.absence.infrastructure.web.dto.AbsenceErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = AbsenceBusinessKeyController.class)
public class AbsenceExceptionHandler {

    @ExceptionHandler(AbsenceNotFoundException.class)
    public ResponseEntity<AbsenceErrorResponse> handleNotFound(AbsenceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new AbsenceErrorResponse("ABSENCE_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<AbsenceErrorResponse> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new AbsenceErrorResponse("INVALID_REQUEST", ex.getMessage()));
    }

    @ExceptionHandler({
            AbsenceCatalogValueInvalidException.class,
            AbsenceEmployeeNotFoundException.class,
            AbsenceOutsidePresencePeriodException.class,
            InvalidAbsenceDateRangeException.class
    })
    public ResponseEntity<AbsenceErrorResponse> handleUnprocessable(RuntimeException ex) {
        String code = errorCode(ex);
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new AbsenceErrorResponse(code, ex.getMessage()));
    }

    @ExceptionHandler(AbsenceOverlapException.class)
    public ResponseEntity<AbsenceErrorResponse> handleOverlap(AbsenceOverlapException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new AbsenceErrorResponse("ABSENCE_OVERLAP", ex.getMessage()));
    }

    private String errorCode(RuntimeException ex) {
        if (ex instanceof AbsenceCatalogValueInvalidException) return "ABSENCE_CATALOG_VALUE_INVALID";
        if (ex instanceof AbsenceEmployeeNotFoundException)    return "ABSENCE_EMPLOYEE_NOT_FOUND";
        if (ex instanceof AbsenceOutsidePresencePeriodException) return "ABSENCE_OUTSIDE_PRESENCE_PERIOD";
        if (ex instanceof InvalidAbsenceDateRangeException)   return "INVALID_ABSENCE_DATE_RANGE";
        return "UNPROCESSABLE";
    }
}
