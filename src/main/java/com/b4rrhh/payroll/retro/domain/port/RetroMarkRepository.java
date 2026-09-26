package com.b4rrhh.payroll.retro.domain.port;

import com.b4rrhh.payroll.retro.domain.model.RetroMark;

import java.util.List;
import java.util.Optional;

/** Las marcas de retroactividad, como las ve el dominio ({@code backend#130}). */
public interface RetroMarkRepository {

    RetroMark save(RetroMark mark);

    Optional<RetroMark> findById(Long id);

    /** Todas las de un empleado, en cualquier estado y de la mas reciente a la mas antigua. */
    List<RetroMark> findByEmployee(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber
    );

    /**
     * Las activas de un empleado.
     *
     * <p>Es la pregunta del lanzamiento, y se hace una vez por empleado del run: con suelo para todos
     * son ochocientas sesenta veces por corrida.
     */
    List<RetroMark> findActiveByEmployee(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber
    );
}
