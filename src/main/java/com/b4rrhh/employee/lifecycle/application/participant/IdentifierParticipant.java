package com.b4rrhh.employee.lifecycle.application.participant;

import com.b4rrhh.employee.identifier.application.usecase.CreateIdentifierCommand;
import com.b4rrhh.employee.identifier.application.usecase.CreateIdentifierUseCase;
import com.b4rrhh.employee.identifier.domain.exception.IdentifierCatalogValueInvalidException;
import com.b4rrhh.employee.identifier.domain.exception.IdentifierValueInvalidException;
import com.b4rrhh.employee.lifecycle.application.model.HireContext;
import com.b4rrhh.employee.lifecycle.application.port.HireParticipant;
import com.b4rrhh.employee.lifecycle.domain.exception.HireEmployeeCatalogValueInvalidException;
import org.springframework.stereotype.Component;

/**
 * El documento que identifica a la persona, guardado como su identificador principal
 * (b4rrhh/backend#141). Va justo detrás del empleado, que es de quien cuelga, y no depende de la
 * presencia.
 */
@Component
public class IdentifierParticipant implements HireParticipant {

    private final CreateIdentifierUseCase createIdentifierUseCase;

    public IdentifierParticipant(CreateIdentifierUseCase createIdentifierUseCase) {
        this.createIdentifierUseCase = createIdentifierUseCase;
    }

    @Override
    public int order() {
        return 15;
    }

    @Override
    public void participate(HireContext ctx) {
        try {
            createIdentifierUseCase.create(new CreateIdentifierCommand(
                    ctx.ruleSystemCode(),
                    ctx.employeeTypeCode(),
                    ctx.employeeNumber(),
                    ctx.identifier().identifierTypeCode(),
                    ctx.identifier().identifierValue(),
                    ctx.identifier().issuingCountryCode(),
                    ctx.identifier().expirationDate(),
                    true
            ));
        } catch (IdentifierCatalogValueInvalidException | IdentifierValueInvalidException ex) {
            throw new HireEmployeeCatalogValueInvalidException(ex.getMessage(), ex);
        }
    }
}
