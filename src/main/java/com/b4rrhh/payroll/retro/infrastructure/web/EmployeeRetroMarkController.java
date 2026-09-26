package com.b4rrhh.payroll.retro.infrastructure.web;

import com.b4rrhh.payroll.retro.application.usecase.ListEmployeeRetroMarksCommand;
import com.b4rrhh.payroll.retro.application.usecase.ListEmployeeRetroMarksUseCase;
import com.b4rrhh.payroll.retro.infrastructure.web.dto.RetroMarkResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Las marcas de retroactividad de un empleado ({@code backend#130}).
 *
 * <p>Solo lectura. El unico verbo que hay sobre una marca —descartar— esta en
 * {@link RetroMarkController} y no aqui: no es una operacion sobre el empleado, es sobre la marca, y
 * la ruta lo dice.
 */
@RestController
@RequestMapping("/employees/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}/retro-marks")
public class EmployeeRetroMarkController {

    private final ListEmployeeRetroMarksUseCase list;
    private final RetroMarkWebMapper mapper;

    public EmployeeRetroMarkController(ListEmployeeRetroMarksUseCase list, RetroMarkWebMapper mapper) {
        this.list = list;
        this.mapper = mapper;
    }

    @GetMapping
    public List<RetroMarkResponse> list(
            @PathVariable String ruleSystemCode,
            @PathVariable String employeeTypeCode,
            @PathVariable String employeeNumber
    ) {
        return list.list(new ListEmployeeRetroMarksCommand(
                        ruleSystemCode, employeeTypeCode, employeeNumber))
                .stream().map(mapper::toResponse).toList();
    }
}
