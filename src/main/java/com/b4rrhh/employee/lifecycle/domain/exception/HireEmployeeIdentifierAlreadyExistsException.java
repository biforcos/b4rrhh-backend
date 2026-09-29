package com.b4rrhh.employee.lifecycle.domain.exception;

import com.b4rrhh.employee.lifecycle.application.model.IdentifierOwner;

/**
 * El documento del alta ya es de otro empleado (b4rrhh/backend#141). Lleva al dueño para que la
 * respuesta pueda enlazarlo y ofrecer, si está cesado, la readmisión.
 */
public class HireEmployeeIdentifierAlreadyExistsException extends RuntimeException {

    private final IdentifierOwner owner;

    public HireEmployeeIdentifierAlreadyExistsException(IdentifierOwner owner, String identifierTypeCode, String identifierValue) {
        super(owner.alreadyIs(identifierTypeCode, identifierValue) + (owner.active()
                ? ": no se puede contratar dos veces a la misma persona."
                : ": si vuelve, es una readmisión, desde su ficha."));
        this.owner = owner;
    }

    public IdentifierOwner owner() {
        return owner;
    }
}
