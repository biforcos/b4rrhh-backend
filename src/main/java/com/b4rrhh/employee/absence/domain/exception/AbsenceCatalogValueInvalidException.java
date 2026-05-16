package com.b4rrhh.employee.absence.domain.exception;

public class AbsenceCatalogValueInvalidException extends RuntimeException {
    public AbsenceCatalogValueInvalidException(String field, String value) {
        super("Invalid absence catalog value for field '" + field + "': " + value);
    }
}
