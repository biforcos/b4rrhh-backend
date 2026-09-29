package com.b4rrhh.employee.lifecycle.application.service;

import com.b4rrhh.employee.identifier.application.usecase.CreateIdentifierCommand;
import com.b4rrhh.employee.identifier.application.usecase.CreateIdentifierUseCase;
import com.b4rrhh.employee.identifier.application.usecase.ListEmployeeIdentifiersUseCase;
import com.b4rrhh.employee.identifier.domain.model.Identifier;
import com.b4rrhh.employee.lifecycle.application.command.RehireEmployeeCommand;
import com.b4rrhh.employee.lifecycle.application.model.IdentifierOwner;
import com.b4rrhh.employee.lifecycle.application.port.IdentifierOwnerLookupPort;
import com.b4rrhh.employee.lifecycle.domain.exception.RehireEmployeeIdentifierOfAnotherEmployeeException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * El documento que llega en una readmisión es el del empleado que se readmite
 * (b4rrhh/backend#141): la comprobación del alta, al revés.
 *
 * <p>En el alta el documento no puede ser de nadie; aquí tiene que ser del readmitido. Si es de
 * otro, se rechaza nombrándolo. Si el readmitido ya tiene uno de ese tipo con otro valor, también:
 * no se sabe cuál de los dos es el bueno y no se decide aquí. Si no tiene ninguno de ese tipo, se
 * le guarda, principal si no tenía principal. El documento es opcional en la readmisión: el
 * empleado ya existe y ya es quien es.
 */
@Component
public class RehireIdentifierGuard {

    private final IdentifierOwnerLookupPort identifierOwnerLookupPort;
    private final ListEmployeeIdentifiersUseCase listEmployeeIdentifiersUseCase;
    private final CreateIdentifierUseCase createIdentifierUseCase;

    public RehireIdentifierGuard(
            IdentifierOwnerLookupPort identifierOwnerLookupPort,
            ListEmployeeIdentifiersUseCase listEmployeeIdentifiersUseCase,
            CreateIdentifierUseCase createIdentifierUseCase
    ) {
        this.identifierOwnerLookupPort = identifierOwnerLookupPort;
        this.listEmployeeIdentifiersUseCase = listEmployeeIdentifiersUseCase;
        this.createIdentifierUseCase = createIdentifierUseCase;
    }

    public void checkAndKeep(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber,
            RehireEmployeeCommand.RehireEmployeeIdentifierCommand identifier
    ) {
        if (identifier == null || isBlank(identifier.identifierTypeCode()) || isBlank(identifier.identifierValue())) {
            return;
        }
        String typeCode = identifier.identifierTypeCode().trim().toUpperCase();
        String value = identifier.identifierValue().trim().toUpperCase();

        Optional<IdentifierOwner> owner = identifierOwnerLookupPort.findOwner(ruleSystemCode, typeCode, value);
        if (owner.isPresent()) {
            IdentifierOwner found = owner.get();
            if (found.employeeTypeCode().equals(employeeTypeCode) && found.employeeNumber().equals(employeeNumber)) {
                return;
            }
            throw new RehireEmployeeIdentifierOfAnotherEmployeeException(
                    found.alreadyIs(typeCode, value) + ", no " + employeeNumber
                            + ": no se puede readmitir a una persona con el documento de otra.");
        }

        List<Identifier> own = listEmployeeIdentifiersUseCase
                .listByEmployeeBusinessKey(ruleSystemCode, employeeTypeCode, employeeNumber);
        Optional<Identifier> sameType = own.stream()
                .filter(existing -> typeCode.equals(existing.getIdentifierTypeCode()))
                .findFirst();
        if (sameType.isPresent()) {
            throw new RehireEmployeeIdentifierOfAnotherEmployeeException(
                    employeeNumber + " ya tiene " + sameType.get().getIdentifierValue()
                            + " y llega " + value + ": el documento de la readmisión no es el suyo.");
        }

        createIdentifierUseCase.create(new CreateIdentifierCommand(
                ruleSystemCode,
                employeeTypeCode,
                employeeNumber,
                typeCode,
                value,
                isBlank(identifier.issuingCountryCode()) ? null : identifier.issuingCountryCode().trim().toUpperCase(),
                null,
                own.stream().noneMatch(Identifier::isPrimary)
        ));
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
