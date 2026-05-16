package com.b4rrhh.employee.absence.domain.exception;

public class AbsenceOutsidePresencePeriodException extends RuntimeException {
    public AbsenceOutsidePresencePeriodException(String message) { super(message); }
}
