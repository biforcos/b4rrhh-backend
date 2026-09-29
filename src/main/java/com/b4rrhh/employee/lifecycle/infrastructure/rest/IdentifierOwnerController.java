package com.b4rrhh.employee.lifecycle.infrastructure.rest;

import com.b4rrhh.employee.lifecycle.application.usecase.FindIdentifierOwnerUseCase;
import com.b4rrhh.employee.lifecycle.infrastructure.rest.dto.IdentifierOwnerResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * «¿De quién es ya este documento?», antes de picar el alta entera (b4rrhh/backend#149). Con
 * dueño, el mismo que nombraría el 409 del alta; libre, 204 sin cuerpo.
 */
@RestController
@RequestMapping("/employees")
public class IdentifierOwnerController {

    private final FindIdentifierOwnerUseCase findIdentifierOwnerUseCase;

    public IdentifierOwnerController(FindIdentifierOwnerUseCase findIdentifierOwnerUseCase) {
        this.findIdentifierOwnerUseCase = findIdentifierOwnerUseCase;
    }

    @GetMapping("/identifier-owner")
    public ResponseEntity<IdentifierOwnerResponse> findOwner(
            @RequestParam String ruleSystemCode,
            @RequestParam String identifierTypeCode,
            @RequestParam String identifierValue
    ) {
        return findIdentifierOwnerUseCase.findOwner(ruleSystemCode, identifierTypeCode, identifierValue)
                .map(owner -> ResponseEntity.ok(new IdentifierOwnerResponse(
                        owner.employeeTypeCode(),
                        owner.employeeNumber(),
                        owner.active(),
                        owner.ceasedOn(),
                        owner.alreadyIs(identifierTypeCode, identifierValue.trim().toUpperCase())
                )))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
