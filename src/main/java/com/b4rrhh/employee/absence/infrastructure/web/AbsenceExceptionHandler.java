package com.b4rrhh.employee.absence.infrastructure.web;

import com.b4rrhh.employee.absence.domain.exception.AbsenceCatalogValueInvalidException;
import com.b4rrhh.employee.absence.domain.exception.AbsenceEmployeeNotFoundException;
import com.b4rrhh.employee.absence.domain.exception.AbsenceNotFoundException;
import com.b4rrhh.employee.absence.domain.exception.AbsenceOutsidePresencePeriodException;
import com.b4rrhh.employee.absence.domain.exception.AbsenceOverlapException;
import com.b4rrhh.employee.absence.domain.exception.InvalidAbsenceDateRangeException;
import com.b4rrhh.employee.absence.infrastructure.web.dto.AbsenceErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = AbsenceBusinessKeyController.class)
public class AbsenceExceptionHandler {

    @ExceptionHandler(AbsenceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public AbsenceErrorResponse handleNotFound(AbsenceNotFoundException ex) {
        return new AbsenceErrorResponse("ABSENCE_NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(AbsenceCatalogValueInvalidException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public AbsenceErrorResponse handleCatalogInvalid(AbsenceCatalogValueInvalidException ex) {
        return new AbsenceErrorResponse("ABSENCE_CATALOG_VALUE_INVALID", ex.getMessage());
    }

    @ExceptionHandler(AbsenceEmployeeNotFoundException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public AbsenceErrorResponse handleEmployeeNotFound(AbsenceEmployeeNotFoundException ex) {
        return new AbsenceErrorResponse("ABSENCE_EMPLOYEE_NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(AbsenceOutsidePresencePeriodException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public AbsenceErrorResponse handleOutsidePresence(AbsenceOutsidePresencePeriodException ex) {
        return new AbsenceErrorResponse("ABSENCE_OUTSIDE_PRESENCE_PERIOD", ex.getMessage());
    }

    @ExceptionHandler(InvalidAbsenceDateRangeException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public AbsenceErrorResponse handleInvalidDateRange(InvalidAbsenceDateRangeException ex) {
        return new AbsenceErrorResponse("INVALID_ABSENCE_DATE_RANGE", ex.getMessage());
    }

    @ExceptionHandler(AbsenceOverlapException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public AbsenceErrorResponse handleOverlap(AbsenceOverlapException ex) {
        return new AbsenceErrorResponse("ABSENCE_OVERLAP", ex.getMessage());
    }
}
