package com.b4rrhh.employee.presence.infrastructure.web;

import com.b4rrhh.employee.presence.application.usecase.RectifyPresenceDateCommand;
import com.b4rrhh.employee.presence.application.usecase.RectifyPresenceDateResult;
import com.b4rrhh.employee.presence.application.usecase.RectifyPresenceDateUseCase;
import com.b4rrhh.employee.presence.infrastructure.web.dto.RectifyPresenceDateRequest;
import com.b4rrhh.employee.presence.infrastructure.web.dto.RectifyPresenceDateResponse;
import com.b4rrhh.employee.shared.application.port.PresenceRectification;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Rectificar la fecha de alta o de cese de una presencia (backend#172). Un verbo propio, uno por fecha,
 * y no una edición de campo: es el alta (o el cese) que se tecleó mal, con todo lo que empezó con él.
 */
@RestController
@RequestMapping("/employees/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}/presences/{presenceNumber}")
public class PresenceRectificationController {

    private final RectifyPresenceDateUseCase rectifyPresenceDateUseCase;

    public PresenceRectificationController(RectifyPresenceDateUseCase rectifyPresenceDateUseCase) {
        this.rectifyPresenceDateUseCase = rectifyPresenceDateUseCase;
    }

    @PostMapping("/rectify-start-date")
    public RectifyPresenceDateResponse rectifyStartDate(
            @PathVariable String ruleSystemCode,
            @PathVariable String employeeTypeCode,
            @PathVariable String employeeNumber,
            @PathVariable Integer presenceNumber,
            @RequestBody RectifyPresenceDateRequest request,
            Authentication authentication
    ) {
        return rectify(ruleSystemCode, employeeTypeCode, employeeNumber, presenceNumber,
                PresenceRectification.Field.START_DATE, request, authentication);
    }

    @PostMapping("/rectify-end-date")
    public RectifyPresenceDateResponse rectifyEndDate(
            @PathVariable String ruleSystemCode,
            @PathVariable String employeeTypeCode,
            @PathVariable String employeeNumber,
            @PathVariable Integer presenceNumber,
            @RequestBody RectifyPresenceDateRequest request,
            Authentication authentication
    ) {
        return rectify(ruleSystemCode, employeeTypeCode, employeeNumber, presenceNumber,
                PresenceRectification.Field.END_DATE, request, authentication);
    }

    private RectifyPresenceDateResponse rectify(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber,
            Integer presenceNumber,
            PresenceRectification.Field field,
            RectifyPresenceDateRequest request,
            Authentication authentication
    ) {
        RectifyPresenceDateResult result = rectifyPresenceDateUseCase.rectify(new RectifyPresenceDateCommand(
                ruleSystemCode, employeeTypeCode, employeeNumber, presenceNumber, field, request.newDate(),
                Boolean.TRUE.equals(request.dryRun()),
                authentication == null ? "desconocido" : authentication.getName()));
        return new RectifyPresenceDateResponse(
                result.presenceNumber(),
                result.field().name(),
                result.oldDate(),
                result.newDate(),
                result.dryRun(),
                result.moves().stream()
                        .map(move -> new RectifyPresenceDateResponse.Move(move.vertical(), move.label(),
                                move.startDate(), move.endDate(), move.newStartDate(), move.newEndDate()))
                        .toList());
    }
}
