package com.b4rrhh.employee.lifecycle.infrastructure.rest;

import com.b4rrhh.employee.lifecycle.application.model.IdentifierCheck;
import com.b4rrhh.employee.lifecycle.application.model.IdentifierOwner;
import com.b4rrhh.employee.lifecycle.application.usecase.FindIdentifierOwnerUseCase;
import com.b4rrhh.employee.lifecycle.infrastructure.rest.dto.IdentifierOwnerResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * «¿Está bien escrito este documento, y de quién es ya?», antes de picar el alta entera
 * (b4rrhh/backend#149, backend#171). Siempre contesta: mal escrito, con el motivo; con dueño, el
 * mismo que nombraría el 409 del alta; libre, válido y sin dueño.
 */
@RestController
@RequestMapping("/employees")
public class IdentifierOwnerController {

    private final FindIdentifierOwnerUseCase findIdentifierOwnerUseCase;

    public IdentifierOwnerController(FindIdentifierOwnerUseCase findIdentifierOwnerUseCase) {
        this.findIdentifierOwnerUseCase = findIdentifierOwnerUseCase;
    }

    @GetMapping("/identifier-owner")
    public IdentifierOwnerResponse findOwner(
            @RequestParam String ruleSystemCode,
            @RequestParam String identifierTypeCode,
            @RequestParam String identifierValue,
            @RequestParam(required = false) String issuingCountryCode
    ) {
        IdentifierCheck check = findIdentifierOwnerUseCase.check(
                ruleSystemCode, identifierTypeCode, identifierValue, issuingCountryCode);
        IdentifierOwner owner = check.owner();
        if (owner == null) {
            return new IdentifierOwnerResponse(check.valid(), check.reason(), null, null, null, null, null);
        }
        return new IdentifierOwnerResponse(
                true,
                null,
                owner.employeeTypeCode(),
                owner.employeeNumber(),
                owner.active(),
                owner.ceasedOn(),
                owner.alreadyIs(identifierTypeCode, identifierValue.trim().toUpperCase()));
    }
}
