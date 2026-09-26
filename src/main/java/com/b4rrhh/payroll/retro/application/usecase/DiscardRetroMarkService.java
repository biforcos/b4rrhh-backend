package com.b4rrhh.payroll.retro.application.usecase;

import com.b4rrhh.payroll.retro.domain.exception.RetroMarkNotActiveException;
import com.b4rrhh.payroll.retro.domain.exception.RetroMarkNotFoundException;
import com.b4rrhh.payroll.retro.domain.model.RetroMark;
import com.b4rrhh.payroll.retro.domain.port.RetroMarkRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Descartar una marca: decidir que esa correccion <b>no se paga</b> ({@code backend#130}).
 *
 * <p>Es el unico verbo que hay sobre una marca, y no es un borrado. La fila se queda, en
 * {@code DISCARDED}, con quien y con por que, porque <b>el recibo tiene que poder contar que habia una
 * correccion conocida que alguien decidio no pagar</b>. Un borrado dejaria el recibo sin nada que
 * contar y al empleado sin nadie a quien preguntar.
 *
 * <p>Y no se acorta en sitio: si lo que hace falta es recalcular desde otro mes, eso es una marca
 * nueva, no esta cambiada. Editar en sitio es lo que convierte un registro en un indicador.
 */
@Service
public class DiscardRetroMarkService implements DiscardRetroMarkUseCase {

    private final RetroMarkRepository marks;

    public DiscardRetroMarkService(RetroMarkRepository marks) {
        this.marks = marks;
    }

    @Override
    @Transactional
    public RetroMark discard(DiscardRetroMarkCommand command) {
        RetroMark mark = marks.findById(command.id())
                .orElseThrow(() -> new RetroMarkNotFoundException(command.id()));

        if (!mark.getStatus().isActive()) {
            throw new RetroMarkNotActiveException(command.id(), mark.getStatus());
        }

        return marks.save(mark.discard(
                command.discardedBy(), command.discardReason(), Instant.now()));
    }
}
