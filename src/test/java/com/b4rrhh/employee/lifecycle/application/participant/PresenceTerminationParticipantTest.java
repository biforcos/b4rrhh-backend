package com.b4rrhh.employee.lifecycle.application.participant;

import com.b4rrhh.employee.lifecycle.application.model.TerminationContext;
import com.b4rrhh.employee.lifecycle.domain.exception.TerminateEmployeeCatalogValueInvalidException;
import com.b4rrhh.employee.lifecycle.domain.exception.TerminateEmployeeConflictException;
import com.b4rrhh.employee.presence.application.usecase.ClosePresenceCommand;
import com.b4rrhh.employee.presence.application.usecase.ClosePresenceUseCase;
import com.b4rrhh.employee.presence.application.usecase.ListEmployeePresencesUseCase;
import com.b4rrhh.employee.presence.domain.exception.PresenceAlreadyClosedException;
import com.b4rrhh.employee.presence.domain.exception.PresenceCatalogValueInvalidException;
import com.b4rrhh.employee.presence.domain.model.Presence;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PresenceTerminationParticipantTest {

    @Mock private ListEmployeePresencesUseCase listPresences;
    @Mock private ClosePresenceUseCase closePresence;

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 1, 1, 0, 0);
    private static final LocalDate TERMINATION_DATE = LocalDate.of(2026, 3, 31);
    private static final LocalDate START_DATE = LocalDate.of(2026, 1, 1);

    @Test
    void orderIs60() {
        // La presencia es la raiz del cese y se cierra la primera: los demas
        // verticales validan sus periodos contra ella.
        assertEquals(5, participant().order());
    }

    @Test
    void closesActivePresenceAndStoresInContext() {
        Presence active = presence(1, START_DATE, null, null);
        Presence closed = presence(1, START_DATE, TERMINATION_DATE, "EXIT");

        when(listPresences.listByEmployeeBusinessKey("ESP", "INTERNAL", "EMP001"))
                .thenReturn(List.of(active));
        when(closePresence.close(any())).thenReturn(closed);

        TerminationContext ctx = context();

        participant().participate(ctx);

        ArgumentCaptor<ClosePresenceCommand> captor =
                ArgumentCaptor.forClass(ClosePresenceCommand.class);
        verify(closePresence).close(captor.capture());
        ClosePresenceCommand cmd = captor.getValue();
        assertEquals("ESP", cmd.ruleSystemCode());
        assertEquals("INTERNAL", cmd.employeeTypeCode());
        assertEquals("EMP001", cmd.employeeNumber());
        assertEquals(1, cmd.presenceNumber());
        assertEquals(TERMINATION_DATE, cmd.endDate());
        assertEquals("RESIGN", cmd.exitReasonCode());

        verify(ctx).setClosedPresence(closed);
    }

    @Test
    void deduplicatesByPresenceNumberPreferringLatestEndDateWhenBothClosed() {
        // Two records with same presenceNumber=1, both closed — the later endDate wins
        Presence olderClosed = presence(1, START_DATE, LocalDate.of(2026, 2, 28), "FIN");
        Presence newerClosed = presence(1, START_DATE, LocalDate.of(2026, 3, 15), "FIN");
        // presenceNumber=2 is the active one
        Presence active2 = presence(2, LocalDate.of(2026, 1, 1), null, null);
        Presence closed2 = presence(2, LocalDate.of(2026, 1, 1), TERMINATION_DATE, "VOL");

        when(listPresences.listByEmployeeBusinessKey(any(), any(), any()))
                .thenReturn(List.of(olderClosed, newerClosed, active2));
        when(closePresence.close(any())).thenReturn(closed2);

        participant().participate(context());

        // Only presenceNumber=2 should be closed (presenceNumber=1 deduplicated to newerClosed which is not active)
        ArgumentCaptor<ClosePresenceCommand> captor =
                ArgumentCaptor.forClass(ClosePresenceCommand.class);
        verify(closePresence).close(captor.capture());
        assertEquals(2, captor.getValue().presenceNumber());
    }

    @Test
    void throwsWhenNoActivePresenceFound() {
        Presence closedOne = presence(1, START_DATE, LocalDate.of(2026, 2, 28), "EXIT");

        when(listPresences.listByEmployeeBusinessKey("ESP", "INTERNAL", "EMP001"))
                .thenReturn(List.of(closedOne));

        TerminationContext ctx = context();

        assertThrows(TerminateEmployeeConflictException.class,
                () -> participant().participate(ctx));

        verify(closePresence, never()).close(any());
    }

    @Test
    void deduplicatesByPresenceNumberPreferringClosedOverActive() {
        // presenceNumber=1: one active, one closed => deduplicated to closed => not active
        Presence p1Active = presence(1, START_DATE, null, null);
        Presence p1Closed = presence(1, START_DATE, LocalDate.of(2026, 2, 28), "TRANSFER");
        // presenceNumber=2: one active => this is the active one to close
        Presence p2Active = presence(2, LocalDate.of(2026, 3, 1), null, null);

        Presence p2Closed = presence(2, LocalDate.of(2026, 3, 1), TERMINATION_DATE, "EXIT");

        when(listPresences.listByEmployeeBusinessKey("ESP", "INTERNAL", "EMP001"))
                .thenReturn(List.of(p1Active, p1Closed, p2Active));
        when(closePresence.close(any())).thenReturn(p2Closed);

        TerminationContext ctx = context();
        participant().participate(ctx);

        ArgumentCaptor<ClosePresenceCommand> captor =
                ArgumentCaptor.forClass(ClosePresenceCommand.class);
        verify(closePresence).close(captor.capture());
        assertEquals(2, captor.getValue().presenceNumber());

        verify(ctx).setClosedPresence(p2Closed);
    }

    @Test
    void translatesCatalogExceptionToCatalogInvalidException() {
        Presence active = presence(1, START_DATE, null, null);
        when(listPresences.listByEmployeeBusinessKey("ESP", "INTERNAL", "EMP001"))
                .thenReturn(List.of(active));
        PresenceCatalogValueInvalidException cause =
                new PresenceCatalogValueInvalidException("exitReasonCode", "BAD");
        when(closePresence.close(any())).thenThrow(cause);

        TerminateEmployeeCatalogValueInvalidException ex = assertThrows(
                TerminateEmployeeCatalogValueInvalidException.class,
                () -> participant().participate(context()));
        assertSame(cause, ex.getCause());
    }

    @Test
    void translatesDomainExceptionToConflictException() {
        Presence active = presence(1, START_DATE, null, null);
        when(listPresences.listByEmployeeBusinessKey("ESP", "INTERNAL", "EMP001"))
                .thenReturn(List.of(active));
        PresenceAlreadyClosedException cause =
                new PresenceAlreadyClosedException(1);
        when(closePresence.close(any())).thenThrow(cause);

        TerminateEmployeeConflictException ex = assertThrows(
                TerminateEmployeeConflictException.class,
                () -> participant().participate(context()));
        assertSame(cause, ex.getCause());
    }

    // --- helpers ---

    private PresenceTerminationParticipant participant() {
        return new PresenceTerminationParticipant(listPresences, closePresence);
    }

    private TerminationContext context() {
        TerminationContext ctx = mock(TerminationContext.class);
        when(ctx.ruleSystemCode()).thenReturn("ESP");
        when(ctx.employeeTypeCode()).thenReturn("INTERNAL");
        when(ctx.employeeNumber()).thenReturn("EMP001");
        when(ctx.terminationDate()).thenReturn(TERMINATION_DATE);
        when(ctx.exitReasonCode()).thenReturn("RESIGN");
        return ctx;
    }

    private Presence presence(int number, LocalDate startDate, LocalDate endDate, String exitReason) {
        return new Presence(10L, 100L, number, "COMP", "HIRE", exitReason, startDate, endDate, NOW, NOW);
    }
}
