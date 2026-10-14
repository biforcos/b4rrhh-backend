package com.b4rrhh.employee.lifecycle.application.usecase;

import com.b4rrhh.employee.identifier.application.service.SpanishNationalIdValidator;
import com.b4rrhh.employee.lifecycle.application.model.IdentifierCheck;
import com.b4rrhh.employee.lifecycle.application.port.IdentifierOwnerLookupPort;
import org.springframework.stereotype.Service;

import java.util.Optional;

// El mismo validador y el mismo puerto que el alta, no una segunda versión: lo que la pantalla
// avisa es exactamente lo que el alta negaría (backend#149, backend#171).
@Service
public class FindIdentifierOwnerService implements FindIdentifierOwnerUseCase {

    private final IdentifierOwnerLookupPort identifierOwnerLookupPort;
    private final SpanishNationalIdValidator spanishNationalIdValidator;

    public FindIdentifierOwnerService(
            IdentifierOwnerLookupPort identifierOwnerLookupPort,
            SpanishNationalIdValidator spanishNationalIdValidator
    ) {
        this.identifierOwnerLookupPort = identifierOwnerLookupPort;
        this.spanishNationalIdValidator = spanishNationalIdValidator;
    }

    @Override
    public IdentifierCheck check(
            String ruleSystemCode,
            String identifierTypeCode,
            String identifierValue,
            String issuingCountryCode
    ) {
        Optional<String> problem = spanishNationalIdValidator.problemWithIfApplicable(
                identifierTypeCode, issuingCountryCode, identifierValue);
        if (problem.isPresent()) {
            return IdentifierCheck.invalid(problem.get());
        }
        return IdentifierCheck.valid(identifierOwnerLookupPort
                .findOwner(ruleSystemCode, identifierTypeCode, identifierValue)
                .orElse(null));
    }
}
