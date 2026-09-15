package com.b4rrhh.payroll_engine.object.infrastructure.web;

import com.b4rrhh.payroll_engine.object.application.usecase.CreateBindingRoleCommand;
import com.b4rrhh.payroll_engine.object.application.usecase.CreateBindingRoleUseCase;
import com.b4rrhh.payroll_engine.object.domain.model.PayrollObject;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Alta de ranuras de tabla, que son roles de vinculacion y no tablas (backend#98, ADR-063).
 *
 * <p><b>Por que ya no cuelga de /tables.</b> Vivia bajo esa ruta junto al GET que lista tablas, y
 * parecian el par obvio —listo lo que creo, creo lo que listo— sin serlo: lo que este POST crea no
 * puede salir en aquel GET hasta que alguien, en otro sitio, le ate una tabla con filas. Compartir
 * ruta era lo que fabricaba esa ilusion, y el contrato lo leen dos clientes que no lo leen dos
 * veces.
 *
 * <p><b>Por que esta aqui y no alli.</b> Lo que crea es un {@code payroll_object} de tipo TABLE, o
 * sea una fila del catalogo de objetos: su pariente es {@link PayrollObjectQueryController}, que es
 * quien los lista. El vertical de tablas habla de tablas y de sus filas, y una ranura no es
 * ninguna de las dos cosas.
 *
 * <p>La pantalla llego antes que el contrato: el designer#10 ya lo llamaba «Nueva ranura» y ya
 * explicaba en el propio modal que no es una tabla de importes.
 */
@RestController
@RequestMapping("/payroll-engine/{ruleSystemCode}/binding-roles")
public class PayrollBindingRoleController {

    private final CreateBindingRoleUseCase createBindingRoleUseCase;

    public PayrollBindingRoleController(CreateBindingRoleUseCase createBindingRoleUseCase) {
        this.createBindingRoleUseCase = createBindingRoleUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BindingRoleResponse create(
            @PathVariable String ruleSystemCode,
            @Valid @RequestBody CreateBindingRoleRequest request
    ) {
        PayrollObject saved = createBindingRoleUseCase.create(
                new CreateBindingRoleCommand(ruleSystemCode, request.bindingRoleCode())
        );
        return new BindingRoleResponse(saved.getRuleSystemCode(), saved.getObjectCode());
    }
}
