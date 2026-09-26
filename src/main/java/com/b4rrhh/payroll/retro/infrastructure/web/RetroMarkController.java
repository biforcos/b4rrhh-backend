package com.b4rrhh.payroll.retro.infrastructure.web;

import com.b4rrhh.payroll.retro.application.usecase.DiscardRetroMarkCommand;
import com.b4rrhh.payroll.retro.application.usecase.DiscardRetroMarkUseCase;
import com.b4rrhh.payroll.retro.infrastructure.web.dto.DiscardRetroMarkRequest;
import com.b4rrhh.payroll.retro.infrastructure.web.dto.RetroMarkResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * El unico verbo que hay sobre una marca: descartarla, con motivo ({@code backend#130}).
 *
 * <p>No hay {@code PUT} ni {@code DELETE} a proposito, y la ruta dice el verbo en vez de fingir una
 * actualizacion: una marca no se edita en sitio ni se borra, y una API que ofreciera esos dos metodos
 * estaria invitando a lo contrario de lo que el modelo decide.
 *
 * <p>Quien descarta sale del token y no del cuerpo de la peticion. Un cliente que pudiera decir quien
 * descarto convertiria el registro en algo que no prueba nada.
 */
@RestController
@RequestMapping("/payroll/retro-marks")
public class RetroMarkController {

    private final DiscardRetroMarkUseCase discard;
    private final RetroMarkWebMapper mapper;

    public RetroMarkController(DiscardRetroMarkUseCase discard, RetroMarkWebMapper mapper) {
        this.discard = discard;
        this.mapper = mapper;
    }

    @PostMapping("/{id}/discard")
    public RetroMarkResponse discard(
            @PathVariable Long id,
            @RequestBody DiscardRetroMarkRequest request,
            Authentication authentication
    ) {
        String quien = authentication == null ? "system" : authentication.getName();
        return mapper.toResponse(discard.discard(
                new DiscardRetroMarkCommand(id, quien, request == null ? null : request.discardReason())));
    }
}
