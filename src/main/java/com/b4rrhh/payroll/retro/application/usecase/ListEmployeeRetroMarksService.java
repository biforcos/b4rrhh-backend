package com.b4rrhh.payroll.retro.application.usecase;

import com.b4rrhh.payroll.retro.domain.model.RetroMark;
import com.b4rrhh.payroll.retro.domain.port.RetroMarkRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Las marcas de un empleado, <b>todas y en su estado</b> ({@code backend#130}).
 *
 * <p>Todas y no solo las activas: la ficha tiene que ensenar las descartadas con su motivo —es la
 * razon de que descartar no borre— y las consumidas con el recibo que las pago.
 */
@Service
public class ListEmployeeRetroMarksService implements ListEmployeeRetroMarksUseCase {

    private final RetroMarkRepository marks;

    public ListEmployeeRetroMarksService(RetroMarkRepository marks) {
        this.marks = marks;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RetroMark> list(ListEmployeeRetroMarksCommand command) {
        return marks.findByEmployee(
                command.ruleSystemCode().trim().toUpperCase(),
                command.employeeTypeCode().trim().toUpperCase(),
                command.employeeNumber().trim());
    }
}
