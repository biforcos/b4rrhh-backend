package com.b4rrhh.employee.lifecycle.application.participant;

import com.b4rrhh.employee.absence.application.usecase.CloseOpenAbsenceAtTerminationUseCase;
import com.b4rrhh.employee.lifecycle.application.model.TerminationContext;
import com.b4rrhh.employee.lifecycle.application.port.TerminationParticipant;
import org.springframework.stereotype.Component;

@Component
public class AbsenceTerminationParticipant implements TerminationParticipant {

    private final CloseOpenAbsenceAtTerminationUseCase closeIfOpen;

    public AbsenceTerminationParticipant(CloseOpenAbsenceAtTerminationUseCase closeIfOpen) {
        this.closeIfOpen = closeIfOpen;
    }

    @Override
    public int order() {
        return 25;
    }

    @Override
    public void participate(TerminationContext ctx) {
        closeIfOpen.closeIfOpen(ctx.ruleSystemCode(), ctx.employeeTypeCode(),
            ctx.employeeNumber(), ctx.terminationDate());
    }
}
