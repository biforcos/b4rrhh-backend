package com.b4rrhh.employee.lifecycle.domain.exception;

/**
 * La readmisión trae un documento que no es el del empleado que se readmite (b4rrhh/backend#141):
 * o es de otro empleado, o el readmitido ya tiene otro de ese tipo.
 */
public class RehireEmployeeIdentifierOfAnotherEmployeeException extends RuntimeException {

    public RehireEmployeeIdentifierOfAnotherEmployeeException(String message) {
        super(message);
    }
}
