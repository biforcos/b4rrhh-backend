package com.b4rrhh.employee.lifecycle.application.participant;

import com.b4rrhh.employee.absence.application.usecase.CloseOpenAbsenceAtTerminationUseCase;
import com.b4rrhh.employee.lifecycle.application.model.TerminationContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AbsenceTerminationParticipantTest {

    @Mock private CloseOpenAbsenceAtTerminationUseCase closeIfOpen;
    @InjectMocks private AbsenceTerminationParticipant participant;

    @Test
    void orderIs25() {
        assertEquals(25, participant.order());
    }

    @Test
    void delegatesToCloseIfOpen() {
        TerminationContext ctx = mock(TerminationContext.class);
        when(ctx.ruleSystemCode()).thenReturn("ESP");
        when(ctx.employeeTypeCode()).thenReturn("INTERNAL");
        when(ctx.employeeNumber()).thenReturn("EMP001");
        when(ctx.terminationDate()).thenReturn(LocalDate.of(2026, 5, 31));

        participant.participate(ctx);

        verify(closeIfOpen).closeIfOpen("ESP", "INTERNAL", "EMP001", LocalDate.of(2026, 5, 31));
    }
}
