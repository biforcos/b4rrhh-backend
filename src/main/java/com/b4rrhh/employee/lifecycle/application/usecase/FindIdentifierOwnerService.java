package com.b4rrhh.employee.lifecycle.application.usecase;

import com.b4rrhh.employee.lifecycle.application.model.IdentifierOwner;
import com.b4rrhh.employee.lifecycle.application.port.IdentifierOwnerLookupPort;
import org.springframework.stereotype.Service;

import java.util.Optional;

// El mismo puerto que la guarda del alta, no una segunda busqueda: lo que la pantalla avisa es
// exactamente lo que el alta negaria.
@Service
public class FindIdentifierOwnerService implements FindIdentifierOwnerUseCase {

    private final IdentifierOwnerLookupPort identifierOwnerLookupPort;

    public FindIdentifierOwnerService(IdentifierOwnerLookupPort identifierOwnerLookupPort) {
        this.identifierOwnerLookupPort = identifierOwnerLookupPort;
    }

    @Override
    public Optional<IdentifierOwner> findOwner(String ruleSystemCode, String identifierTypeCode, String identifierValue) {
        return identifierOwnerLookupPort.findOwner(ruleSystemCode, identifierTypeCode, identifierValue);
    }
}
