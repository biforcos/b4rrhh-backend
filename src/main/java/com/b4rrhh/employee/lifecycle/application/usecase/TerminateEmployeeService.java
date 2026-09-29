package com.b4rrhh.employee.lifecycle.application.usecase;

import com.b4rrhh.employee.presence.application.usecase.GetEmployeeStandingUseCase;
import com.b4rrhh.employee.lifecycle.application.command.TerminateEmployeeCommand;
import com.b4rrhh.employee.lifecycle.application.model.TerminateEmployeeResult;
import com.b4rrhh.employee.lifecycle.application.model.TerminationContext;
import com.b4rrhh.employee.lifecycle.application.port.TerminationParticipant;
import com.b4rrhh.employee.lifecycle.application.service.TerminationPreConditionValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class TerminateEmployeeService implements TerminateEmployeeUseCase {

    private final TerminationPreConditionValidator validator;
    private final List<TerminationParticipant> participants;
    private final GetEmployeeStandingUseCase getEmployeeStandingUseCase;

    public TerminateEmployeeService(
            TerminationPreConditionValidator validator,
            List<TerminationParticipant> participants,
            GetEmployeeStandingUseCase getEmployeeStandingUseCase) {
        this.validator = validator;
        this.participants = participants.stream()
                .sorted(Comparator.comparingInt(TerminationParticipant::order))
                .toList();
        this.getEmployeeStandingUseCase = getEmployeeStandingUseCase;
    }

    @Override
    @Transactional
    public TerminateEmployeeResult terminate(TerminateEmployeeCommand command) {
        TerminationContext ctx = validator.validateAndLookup(command);
        if (ctx.isAlreadyTerminated()) return ctx.reconstructIdempotentResult();
        participants.forEach(p -> p.participate(ctx));
        ctx.assertNoActivePresence();
        // El estado de hoy, leído de las presencias: un cese a futuro deja al empleado de alta hasta
        // su fecha (b4rrhh/backend#148).
        return ctx.toResult(getEmployeeStandingUseCase.standingOf(ctx.employee().getId(), null).status().name());
    }
}
